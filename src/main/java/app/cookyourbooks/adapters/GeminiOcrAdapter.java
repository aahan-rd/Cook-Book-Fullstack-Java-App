package app.cookyourbooks.adapters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.cookyourbooks.adapters.gemini.GeminiAuthException;
import app.cookyourbooks.adapters.gemini.GeminiClient;
import app.cookyourbooks.adapters.gemini.GeminiException;
import app.cookyourbooks.adapters.gemini.GeminiNetworkException;
import app.cookyourbooks.adapters.gemini.GeminiParseException;
import app.cookyourbooks.adapters.gemini.GeminiRateLimitException;
import app.cookyourbooks.adapters.gemini.GeminiServerException;
import app.cookyourbooks.adapters.gemini.GeminiTimeoutException;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.ocr.OcrException;

/**
 * Driven adapter that uses Google's Gemini API to extract recipes from images.
 *
 * <p>This adapter implements the Hexagonal Architecture pattern: it adapts the external Gemini API
 * (via {@link GeminiClient}) to the internal {@link
 * app.cookyourbooks.services.ocr.RecipeOcrService} interface.
 *
 * <h2>Responsibilities</h2>
 *
 * <ul>
 *   <li>Delegates image-to-JSON extraction to the injected {@link GeminiClient}
 *   <li>Translates Gemini-specific exceptions to user-friendly {@link OcrException} messages
 *   <li>Parses Gemini's JSON response format into the application's {@link Recipe} model
 *   <li>Handles schema transformation (e.g., {@code yield} → {@code servings}, {@code
 *       consumedIngredients} → {@code ingredientRefs})
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * GeminiClient client = new GeminiClientImpl(); // or mock for testing
 * RecipeOcrService ocrService = new GeminiOcrAdapter(client);
 * Recipe recipe = ocrService.extractRecipe(Path.of("recipe-photo.jpg"));
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <p>All Gemini-specific exceptions are translated to {@link OcrException} with actionable
 * messages:
 *
 * <ul>
 *   <li>{@link GeminiAuthException} → "API key error. Check that GOOGLE_API_KEY is set correctly."
 *   <li>{@link GeminiRateLimitException} → "API rate limit reached. Please wait and try again."
 *   <li>{@link GeminiTimeoutException} → "Network error: request timed out."
 *   <li>{@link GeminiParseException} → "Could not extract a recipe from the image."
 * </ul>
 *
 * @see GeminiClient
 * @see app.cookyourbooks.services.ocr.RecipeOcrService
 */
public final class GeminiOcrAdapter implements app.cookyourbooks.services.ocr.RecipeOcrService {

  private final GeminiClient client;
  private final ObjectMapper objectMapper;

  /**
   * Constructs a new GeminiOcrAdapter.
   *
   * @param client the Gemini client (real or mock for testing)
   */
  public GeminiOcrAdapter(GeminiClient client) {
    this.client = client;
    this.objectMapper = createObjectMapper();
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  @Override
  public @NonNull Recipe extractRecipe(@NonNull Path imagePath) throws OcrException {
    try {
      String json = client.extractRecipeJson(imagePath);
      return parseRecipeFromResponse(json);
    } catch (GeminiAuthException e) {
      throw new OcrException("API key error. Check that GOOGLE_API_KEY is set correctly.", e);
    } catch (GeminiRateLimitException e) {
      throw new OcrException("API rate limit reached. Please wait a moment and try again.", e);
    } catch (GeminiTimeoutException e) {
      throw new OcrException(
          "Network error: request timed out. Check your connection and try again.", e);
    } catch (GeminiServerException e) {
      throw new OcrException("API error: HTTP " + e.getStatusCode(), e);
    } catch (GeminiNetworkException e) {
      throw new OcrException("Network error: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new OcrException("Image file not found: " + imagePath, e);
    } catch (GeminiParseException e) {
      throw new OcrException(
          "Could not extract a recipe from the image. Is the image clear and does it contain a recipe?",
          e);
    } catch (GeminiException e) {
      throw new OcrException("Network error: " + e.getMessage(), e);
    }
  }

  /**
   * Parses JSON in the prompt's format to a Recipe. Package-private for testing.
   *
   * @param jsonResponse raw JSON (prompt format: recipes array, yield, value, consumedIngredients)
   * @return the parsed Recipe
   */
  Recipe parseRecipeFromJson(String jsonResponse) throws OcrException {
    return parseRecipeFromResponse(jsonResponse);
  }

  /**
   * Parses the prompt's JSON structure (recipes array, yield, value, consumedIngredients) and
   * transforms it to the Recipe schema (servings, amount, ingredientRefs).
   */
  private Recipe parseRecipeFromResponse(String jsonResponse) throws OcrException {
    String json = extractJsonFromResponse(jsonResponse);
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode recipesArray = root.path("recipes");
      if (!recipesArray.isArray() || recipesArray.isEmpty()) {
        throw new OcrException(
            "Could not extract a recipe from the image. Is the image clear and does it contain a recipe?");
      }
      // Use first recipe when multiple are present on the page
      JsonNode recipeNode = recipesArray.get(0);
      ObjectNode transformed = transformPromptRecipeToRecipeSchema(recipeNode);
      return objectMapper.treeToValue(transformed, Recipe.class);
    } catch (IOException e) {
      throw new OcrException(
          "Could not parse the extracted recipe. Is the image clear and does it contain a recipe? "
              + Objects.requireNonNullElse(e.getMessage(), ""),
          e);
    }
  }

  private ObjectNode transformPromptRecipeToRecipeSchema(JsonNode recipeNode) {
    ObjectNode out = objectMapper.createObjectNode();
    out.put("title", recipeNode.path("title").asText());

    // yield -> servings
    JsonNode yield = recipeNode.path("yield");
    if (yield != null && !yield.isNull() && !yield.isEmpty()) {
      int amount;
      String description = "servings";
      if (yield.has("type") && "exact".equals(yield.path("type").asText()) && yield.has("value")) {
        amount = (int) yield.path("value").asDouble();
      } else if (yield.has("min")) {
        amount = (int) yield.path("min").asDouble();
      } else {
        amount = 4;
      }
      if (amount > 0) {
        ObjectNode servings = objectMapper.createObjectNode();
        servings.put("amount", amount);
        servings.put("description", description);
        out.set("servings", servings);
      }
    }

    // ingredients: normalize quantity "value" -> "amount" for exact
    ArrayNode ingredients = objectMapper.createArrayNode();
    for (JsonNode ing : recipeNode.path("ingredients")) {
      ingredients.add(normalizeIngredientQuantity(ing));
    }
    out.set("ingredients", ingredients);

    // instructions: consumedIngredients -> ingredientRefs
    ArrayNode instructions = objectMapper.createArrayNode();
    for (JsonNode inst : recipeNode.path("instructions")) {
      instructions.add(transformInstruction(inst));
    }
    out.set("instructions", instructions);

    out.set("conversionRules", objectMapper.createArrayNode());
    return out;
  }

  private JsonNode normalizeIngredientQuantity(JsonNode ing) {
    ObjectNode out = ing.deepCopy();
    if (out.has("quantity")
        && out.get("quantity").has("type")
        && "exact".equals(out.get("quantity").path("type").asText())) {
      ObjectNode qty = (ObjectNode) out.get("quantity");
      if (qty.has("value") && !qty.has("amount")) {
        qty.put("amount", qty.get("value").asDouble());
        qty.remove("value");
      }
    }
    return out;
  }

  private JsonNode transformInstruction(JsonNode inst) {
    ObjectNode out = objectMapper.createObjectNode();
    out.put("stepNumber", inst.path("stepNumber").asInt());
    out.put("text", inst.path("text").asText());

    // consumedIngredients -> ingredientRefs
    ArrayNode refs = objectMapper.createArrayNode();
    for (JsonNode consumed : inst.path("consumedIngredients")) {
      String name = consumed.path("ingredientName").asText();
      JsonNode qty = consumed.path("quantity");
      boolean isVague =
          consumed.path("vague").asBoolean(false)
              || qty == null
              || qty.isNull()
              || qty.isMissingNode();

      ObjectNode ingredient = objectMapper.createObjectNode();
      ObjectNode ref = objectMapper.createObjectNode();

      if (isVague) {
        ingredient.put("type", "vague");
        ingredient.put("name", name);
        ingredient.putNull("description");
        ingredient.putNull("preparation");
        ingredient.putNull("notes");
        ref.set("ingredient", ingredient);
        ref.putNull("quantity");
      } else {
        ObjectNode normalizedQty = normalizeQuantityNode(qty);
        ingredient.put("type", "measured");
        ingredient.put("name", name);
        ingredient.set("quantity", normalizedQty);
        ingredient.putNull("preparation");
        ingredient.putNull("notes");
        ref.set("ingredient", ingredient);
        ref.set("quantity", normalizedQty);
      }
      refs.add(ref);
    }
    out.set("ingredientRefs", refs);
    return out;
  }

  private ObjectNode normalizeQuantityNode(JsonNode qty) {
    ObjectNode out = qty.deepCopy();
    if (out.has("type")
        && "exact".equals(out.path("type").asText())
        && out.has("value")
        && !out.has("amount")) {
      out.put("amount", out.get("value").asDouble());
      out.remove("value");
    }
    return out;
  }

  private static String extractJsonFromResponse(String response) {
    response = response.trim();
    if (response.startsWith("```json")) {
      response = response.substring(7);
    } else if (response.startsWith("```")) {
      response = response.substring(3);
    }
    if (response.endsWith("```")) {
      response = response.substring(0, response.length() - 3);
    }
    return response.trim();
  }
}
