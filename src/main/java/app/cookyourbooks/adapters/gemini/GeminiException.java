package app.cookyourbooks.adapters.gemini;

/** Base exception for Gemini API errors. Subtypes represent specific failure scenarios. */
public abstract class GeminiException extends Exception {

  protected GeminiException(String message) {
    super(message);
  }

  protected GeminiException(String message, Throwable cause) {
    super(message, cause);
  }
}
