package app.cookyourbooks.adapters.gemini;

/** Thrown when the API key is invalid or missing (HTTP 401/403). */
public final class GeminiAuthException extends GeminiException {

  public GeminiAuthException() {
    super("Invalid or missing API key");
  }
}
