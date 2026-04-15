package app.cookyourbooks.adapters.gemini;

/** Thrown when the API rate limit is exceeded (HTTP 429). */
public final class GeminiRateLimitException extends GeminiException {

  public GeminiRateLimitException() {
    super("Rate limit exceeded");
  }
}
