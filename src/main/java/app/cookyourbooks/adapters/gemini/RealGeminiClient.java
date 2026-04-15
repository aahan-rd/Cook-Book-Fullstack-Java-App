package app.cookyourbooks.adapters.gemini;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Real implementation of GeminiClient that calls the Google Gemini API.
 *
 * <p>Handles HTTP communication, base64 encoding, and the extraction prompt. Students do not see
 * this implementation's internals — they implement error handling in GeminiOcrAdapter.
 */
public final class RealGeminiClient implements GeminiClient {

  private static final Logger LOG = LoggerFactory.getLogger(RealGeminiClient.class);
  private static final String MODEL = "gemini-3-flash-preview";

  /** Prompt asking Gemini to return JSON matching the Recipe schema. */
  private static final String RECIPE_JSON_PROMPT =
      """
    Extract recipe information from this image and return it as valid JSON (no markdown, no code blocks).

    The image may contain one or more recipes. Extract all recipes you can identify.

    IMPORTANT: Look carefully at the ENTIRE image, including:
    - Top and bottom margins/headers (for book title and page numbers)
    - Corners of the page (for page numbers)
    - Text before or after the recipe (for background/description)
    - Yield information (e.g., "serves 4", "makes 12 cookies", "yields 1 loaf")
    - Special equipment mentioned (e.g., "stand mixer", "food processor", "baking sheet")

    Return a JSON object with this structure:
    {
      "recipes": [
        {
          "title": "Recipe Title",
          "pageNumbers": [123],
          "bookTitle": "Cookbook Name",
          "yield": {
            "type": "exact",
            "value": 4.0,
            "unit": "PEOPLE"
          },
          "timeEstimate": "30 minutes",
          "equipment": ["stand mixer", "baking sheet"],
          "description": "Background text or description about the recipe",
          "ingredients": [
            {
              "type": "measured",
              "name": "flour",
              "quantity": {
                "type": "exact",
                "value": 250.0,
                "unit": "GRAM"
              },
              "preparation": null,
              "notes": null
            },
            {
              "type": "measured",
              "name": "sugar",
              "quantity": {
                "type": "fractional",
                "whole": 0,
                "numerator": 1,
                "denominator": 2,
                "unit": "CUP"
              },
              "preparation": null,
              "notes": null
            },
            {
              "type": "measured",
              "name": "milk",
              "quantity": {
                "type": "range",
                "min": 2.0,
                "max": 3.0,
                "unit": "CUP"
              },
              "preparation": null,
              "notes": null
            },
            {
              "type": "vague",
              "name": "salt",
              "description": "to taste",
              "preparation": null,
              "notes": null
            },
            {
              "type": "vague",
              "name": "pepper",
              "description": "to taste",
              "preparation": null,
              "notes": null
            }
          ],
          "instructions": [
            {
              "stepNumber": 1,
              "text": "Mix 2 cups flour and 1 cup sugar together",
              "temperature": null,
              "subInstructions": [],
              "notes": null,
              "consumedIngredients": [
                {
                  "ingredientName": "flour",
                  "quantity": {
                    "type": "exact",
                    "value": 2.0,
                    "unit": "CUP"
                  }
                },
                {
                  "ingredientName": "sugar",
                  "quantity": {
                    "type": "exact",
                    "value": 1.0,
                    "unit": "CUP"
                  }
                }
              ]
            },
            {
              "stepNumber": 2,
              "text": "Add 1 cup milk gradually",
              "temperature": null,
              "subInstructions": [],
              "notes": null,
              "consumedIngredients": [
                {
                  "ingredientName": "milk",
                  "quantity": {
                    "type": "exact",
                    "value": 1.0,
                    "unit": "CUP"
                  }
                }
              ]
            },
            {
              "stepNumber": 3,
              "text": "Season with salt and pepper",
              "temperature": null,
              "subInstructions": [],
              "notes": null,
              "consumedIngredients": [
                {
                  "ingredientName": "salt",
                  "vague": true
                },
                {
                  "ingredientName": "pepper",
                  "vague": true
                }
              ]
            }
          ]
        }
      ],
      "rawText": "The raw text extracted from the image"
    }

    Important rules:
    - INGREDIENT TYPE: The ingredient "type" field MUST be exactly one of: "measured" or "vague" (nothing else)
      * "measured": Use when the ingredient has a specific quantity (even if it's a range like "2-3 cups")
      * "vague": Use when the ingredient has no specific quantity (e.g., "salt to taste", "pepper", "water as needed")
      * NEVER use "range", "exact", or "fractional" as the ingredient type - these are QUANTITY types, not ingredient types
    - For measured ingredients, use "type": "measured" with a "quantity" object (the quantity itself can be exact, fractional, or range)
    - For vague ingredients (like "salt to taste"), use "type": "vague" with a "description" field
    - MEASUREMENT PREFERENCES: When both weight and volume measures are listed, prefer weight over volume. When both metric and imperial are listed, prefer metric over imperial. For example, if you see "2 cups (250g flour)", use GRAM with value 250.0, not CUP with value 2.0.

    QUANTITY TYPES (for measured ingredients and yield):
    There are three types of quantities you can use:

    1. EXACT QUANTITY: Use for precise decimal values (e.g., "2.5 cups", "100 grams", "3 eggs")
       Format: { "type": "exact", "value": 2.5, "unit": "CUP" }
       - value: a positive decimal number (> 0.0)
       - Examples: "2.5 cups" -> { "type": "exact", "value": 2.5, "unit": "CUP" }
                   "100 g" -> { "type": "exact", "value": 100.0, "unit": "GRAM" }
                   "3 eggs" -> { "type": "exact", "value": 3.0, "unit": "WHOLE" }

    2. FRACTIONAL QUANTITY: Use for fractions and mixed numbers (e.g., "1/2 cup", "2 1/3 tablespoons")
       Format: { "type": "fractional", "whole": 0, "numerator": 1, "denominator": 2, "unit": "CUP" }
       - whole: the whole number part (non-negative integer, >= 0)
       - numerator: the numerator of the fraction (non-negative integer, >= 0)
       - denominator: the denominator of the fraction (positive integer, > 0)
       - At least one of whole or numerator must be positive
       - Examples: "1/2 cup" -> { "type": "fractional", "whole": 0, "numerator": 1, "denominator": 2, "unit": "CUP" }
                   "2 1/3 tbsp" -> { "type": "fractional", "whole": 2, "numerator": 1, "denominator": 3, "unit": "TABLESPOON" }
                   "1/4 tsp" -> { "type": "fractional", "whole": 0, "numerator": 1, "denominator": 4, "unit": "TEASPOON" }

    3. RANGE QUANTITY: Use for ranges (e.g., "2-3 cups", "100-150 grams")
       Format: { "type": "range", "min": 2.0, "max": 3.0, "unit": "CUP" }
       - min: the minimum value (positive decimal, > 0.0)
       - max: the maximum value (must be greater than min)
       - Examples: "2-3 cups" -> { "type": "range", "min": 2.0, "max": 3.0, "unit": "CUP" }
                   "100-150 g" -> { "type": "range", "min": 100.0, "max": 150.0, "unit": "GRAM" }

    UNITS:
    Valid units are organized into three categories:

    IMPERIAL UNITS (volume): CUP, TABLESPOON, TEASPOON, FLUID_OUNCE
    IMPERIAL UNITS (weight): OUNCE, POUND
    METRIC UNITS (volume): MILLILITER, LITER, DECILITER
    METRIC UNITS (weight): GRAM, KILOGRAM
    COUNT UNITS: WHOLE (for counting items like eggs, cookies, loaves), PEOPLE (for serving quantities like "serves 4", "serves 4-6" - use only for yield, not ingredients)

    HOUSE UNITS (informal/imprecise measurements):
    These are "house" units used for small, imprecise measurements that don't have exact conversions:
    - PINCH: A very small amount, typically what you can pinch between thumb and forefinger (e.g., "a pinch of salt")
    - DASH: A small amount, slightly more than a pinch (e.g., "a dash of vanilla")
    - HANDFUL: An amount that fits in your hand (e.g., "a handful of nuts")
    - TO_TASTE: Used for vague ingredients where amount is adjusted to personal preference (e.g., "salt to taste")

    - Temperature must be null OR an object like: { "value": 350, "unit": "FAHRENHEIT" }
    - Include the raw text extracted from the image in the "rawText" field
    - If multiple recipes are present, include all in the "recipes" array
    - pageNumbers: array of integers, extract from corners or headers/footers (e.g., [123] or [123, 124] if recipe spans pages)
    - bookTitle: extract from top/bottom of page if visible (null if not found)
    - yield: extract yield information as a Quantity object. For "serves 4", "makes 12 cookies", "yields 1 loaf", etc., extract the numeric value and use PEOPLE unit for serving quantities (e.g., "serves 4", "serves 4-6"). Use "exact" type for single values, "range" type if a range is given (e.g., "serves 4-6"). Format: { "type": "exact", "value": 4.0, "unit": "PEOPLE" } or { "type": "range", "min": 4.0, "max": 6.0, "unit": "PEOPLE" } (null if not found). Examples: "serves 4" -> { "type": "exact", "value": 4.0, "unit": "PEOPLE" }, "serves 4-6" -> { "type": "range", "min": 4.0, "max": 6.0, "unit": "PEOPLE" }, "makes 12 cookies" -> { "type": "exact", "value": 12.0, "unit": "WHOLE" } (use WHOLE for non-serving yields like cookies, loaves, etc.), "yields 1 loaf" -> { "type": "exact", "value": 1.0, "unit": "WHOLE" }
    - timeEstimate: extract time estimate if provided (e.g., "30 minutes", "1 hour", "45 min prep, 1 hour cook", "20 min prep + 30 min cook") (null if not found)
    - equipment: array of strings listing special equipment needed (empty array if none)
    - description: any background text, introduction, or description about the recipe (null if not found)

    INSTRUCTION INGREDIENT REFERENCES:
    For each instruction, identify which ingredients from the recipe's ingredient list are consumed/used in that step, along with the quantity consumed. Extract both the ingredient name and the quantity that is explicitly mentioned or clearly used in the instruction text.
    - consumedIngredients: array of consumed ingredient objects. Empty array if none or unclear.
    - For measured ingredients (have quantity in recipe): { "ingredientName": "flour", "quantity": { "type": "exact", "value": 2.0, "unit": "CUP" } }
    - For vague ingredients (no quantity in recipe), set "vague": true and omit "quantity". For measured ingredients, always include "quantity".
    - Match ingredient names case-insensitively and handle variations (e.g., "flour" matches "Flour", "all-purpose flour", etc.)
    - Extract the quantity mentioned in the instruction text. If no quantity is specified, use the full recipe quantity for that ingredient.
    - Only include ingredients that are explicitly mentioned or clearly used in the step
    - Examples:
      * "Mix 2 cups flour and 1 cup sugar" -> [{ "ingredientName": "flour", "quantity": { "type": "exact", "value": 2.0, "unit": "CUP" } }, { "ingredientName": "sugar", "quantity": { "type": "exact", "value": 1.0, "unit": "CUP" } }]
      * "Add the milk gradually" (if recipe has "1 cup milk") -> [{ "ingredientName": "milk", "quantity": { "type": "exact", "value": 1.0, "unit": "CUP" } }]
      * "Season with salt and pepper" (if salt/pepper are vague) -> [{ "ingredientName": "salt", "vague": true }, { "ingredientName": "cracked black pepper", "vague": true }]
      * "Combine all dry ingredients" -> include all dry ingredients from the recipe with their full quantities
      * "Bake for 30 minutes" -> [] (no ingredients consumed)
      * "Add 3 eggs one at a time" -> [{ "ingredientName": "eggs", "quantity": { "type": "exact", "value": 3.0, "unit": "WHOLE" } }]
      * "Add remaining flour" (if recipe has "2 cups flour" and 1 cup was used earlier) -> [{ "ingredientName": "flour", "quantity": { "type": "exact", "value": 1.0, "unit": "CUP" } }]
    """;

  private final @Nullable String apiKey;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  /**
   * Constructs a new RealGeminiClient.
   *
   * @param apiKey the Google API key (from GOOGLE_API_KEY env var), may be null for testing
   */
  public RealGeminiClient(@Nullable String apiKey) {
    this.apiKey = apiKey;
    this.objectMapper = createObjectMapper();
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  @Override
  public String extractRecipeJson(Path imagePath)
      throws GeminiException, GeminiParseException, IOException {
    if (apiKey == null || apiKey.isBlank()) {
      throw new GeminiAuthException();
    }

    byte[] imageBytes = Files.readAllBytes(imagePath);
    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
    String mimeType = getMimeType(imagePath);

    return callGeminiApi(base64Image, mimeType);
  }

  private String getMimeType(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".png")) {
      return "image/png";
    }
    if (name.endsWith(".webp")) {
      return "image/webp";
    }
    return "image/jpeg";
  }

  private String callGeminiApi(String base64Image, String mimeType)
      throws GeminiException, GeminiParseException {
    String requestBody =
        """
        {
          "contents": [{
            "parts": [
              {"text": "%s"},
              {
                "inline_data": {
                  "mime_type": "%s",
                  "data": "%s"
                }
              }
            ]
          }],
        }
        """
            .formatted(RECIPE_JSON_PROMPT.replace("\"", "\\\""), mimeType, base64Image);

    LOG.debug("Calling Gemini API for image extraction (model={}, mime={})", MODEL, mimeType);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                        + MODEL
                        + ":generateContent?key="
                        + apiKey))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();

      if (status == 401 || status == 403) {
        throw new GeminiAuthException();
      }
      if (status == 429) {
        throw new GeminiRateLimitException();
      }
      if (status >= 500) {
        throw new GeminiServerException(status);
      }
      if (status != 200) {
        throw new GeminiParseException("API error: HTTP " + status + " " + response.body());
      }

      String responseBody = response.body();
      LOG.debug("Gemini API response: status={}, bodyLength={}", status, responseBody.length());

      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode candidates = root.path("candidates");
      if (candidates.isEmpty()) {
        LOG.warn("Gemini returned no candidates. Full response: {}", responseBody);
        throw new GeminiParseException(
            "Could not extract a recipe from the image. Is the image clear and does it contain a recipe?");
      }
      JsonNode content = candidates.get(0).path("content").path("parts");
      if (content.isEmpty()) {
        LOG.warn("Gemini candidates have no content. Full response: {}", responseBody);
        throw new GeminiParseException(
            "Could not extract a recipe from the image. Is the image clear and does it contain a recipe?");
      }
      String jsonResponse = content.get(0).path("text").asText();
      LOG.debug("Gemini extracted JSON ({} chars): {}", jsonResponse.length(), jsonResponse);
      return jsonResponse;
    } catch (java.net.http.HttpTimeoutException e) {
      throw new GeminiTimeoutException();
    } catch (IOException e) {
      if (e.getMessage() != null && e.getMessage().contains("timed out")) {
        throw new GeminiTimeoutException();
      }
      throw new GeminiNetworkException(
          "Could not extract a recipe from the image. Is the image clear and does it contain a recipe?",
          e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GeminiNetworkException("Request interrupted", e);
    }
  }
}
