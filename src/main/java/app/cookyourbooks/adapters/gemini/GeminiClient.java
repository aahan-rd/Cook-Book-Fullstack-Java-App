package app.cookyourbooks.adapters.gemini;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Client for sending images to the Google Gemini API for recipe extraction.
 *
 * <p>Implementations handle HTTP communication, base64 encoding, and the extraction prompt. Callers
 * receive raw JSON and are responsible for parsing and error handling.
 */
public interface GeminiClient {

  /**
   * Sends an image to Gemini for recipe extraction.
   *
   * @param imagePath path to the image file (JPEG, PNG, or WebP)
   * @return raw JSON string containing the extracted recipe data (recipes array format)
   * @throws GeminiAuthException for HTTP 401/403 (invalid or missing API key)
   * @throws GeminiRateLimitException for HTTP 429 (rate limited)
   * @throws GeminiServerException for HTTP 5xx (server error)
   * @throws GeminiTimeoutException if request times out
   * @throws GeminiNetworkException for other network errors
   * @throws GeminiParseException if the API response is not valid JSON
   * @throws IOException if the image file cannot be read
   */
  String extractRecipeJson(Path imagePath)
      throws GeminiException, GeminiParseException, IOException;
}
