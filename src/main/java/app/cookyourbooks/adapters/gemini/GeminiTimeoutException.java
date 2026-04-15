package app.cookyourbooks.adapters.gemini;

/** Thrown when the request to the Gemini API times out. */
public final class GeminiTimeoutException extends GeminiException {

  public GeminiTimeoutException() {
    super("Request timed out");
  }
}
