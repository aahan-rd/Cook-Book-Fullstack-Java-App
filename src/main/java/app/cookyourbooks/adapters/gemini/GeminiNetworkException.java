package app.cookyourbooks.adapters.gemini;

/** Thrown for other network-related errors (connection refused, DNS failure, etc.). */
public final class GeminiNetworkException extends GeminiException {

  public GeminiNetworkException(String message, Throwable cause) {
    super(message, cause);
  }
}
