package app.cookyourbooks.adapters.gemini;

/** Thrown when the API response cannot be parsed as valid recipe JSON. */
public final class GeminiParseException extends GeminiException {

  public GeminiParseException(String message) {
    super(message);
  }

  public GeminiParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
