package app.cookyourbooks.services.ocr;

import java.nio.file.Path;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Recipe;

/**
 * Service interface for extracting recipes from images using OCR.
 *
 * <p>Implementations may use different backends (e.g., Gemini API, Tesseract) to perform the
 * extraction. All implementations should handle image loading, API communication, and response
 * parsing internally.
 */
public interface RecipeOcrService {

  /**
   * Extracts a recipe from an image file.
   *
   * @param imagePath path to the image file (JPEG, PNG, or WebP)
   * @return the extracted Recipe
   * @throws OcrException if extraction fails (network error, invalid image, no recipe found, etc.)
   */
  @NonNull Recipe extractRecipe(@NonNull Path imagePath) throws OcrException;
}
