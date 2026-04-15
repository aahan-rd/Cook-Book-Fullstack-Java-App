package app.cookyourbooks.adapters.gemini;

/** Thrown when the Gemini API returns a server error (HTTP 5xx). */
public final class GeminiServerException extends GeminiException {

  private final int statusCode;

  public GeminiServerException(int statusCode) {
    super("Server error: HTTP " + statusCode);
    this.statusCode = statusCode;
  }

  /** Returns the HTTP status code (e.g., 500, 503). */
  public int getStatusCode() {
    return statusCode;
  }
}
