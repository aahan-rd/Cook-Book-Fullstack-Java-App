package app.cookyourbooks.services.ocr;

/**
 * Exception thrown when recipe OCR extraction fails.
 *
 * <p>The message is intended to be user-facing and should describe what went wrong in plain
 * language (e.g., "API key error", "Network timeout", "Could not extract a recipe from the image").
 */
public class OcrException extends Exception {

  /**
   * Constructs an OcrException with a user-facing message.
   *
   * @param message description of what went wrong
   */
  public OcrException(String message) {
    super(message);
  }

  /**
   * Constructs an OcrException with a user-facing message and underlying cause.
   *
   * @param message description of what went wrong
   * @param cause the underlying exception
   */
  public OcrException(String message, Throwable cause) {
    super(message, cause);
  }
}
