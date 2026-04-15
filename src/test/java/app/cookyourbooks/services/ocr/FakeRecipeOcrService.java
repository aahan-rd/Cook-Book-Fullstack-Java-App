package app.cookyourbooks.services.ocr;

import java.nio.file.Path;
import java.util.List;

import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.VagueIngredient;

/**
 * A fake {@link RecipeOcrService} for development and testing.
 *
 * <p>Returns a canned recipe after a configurable delay, simulating the latency of a real OCR
 * service. Use this in your ViewModel tests and during local development so you don't need a Gemini
 * API key.
 *
 * <h2>Example usage in tests</h2>
 *
 * <pre>{@code
 * RecipeOcrService ocr = new FakeRecipeOcrService(500); // 500ms simulated delay
 * ImportViewModel vm = new MyImportViewModel(ocr, librarianService);
 * vm.startImport(Path.of("pancakes.jpg"));
 * // ... wait for async completion, then assert REVIEW state
 * }</pre>
 *
 * <h2>Example usage in the app (for local development)</h2>
 *
 * <pre>{@code
 * RecipeOcrService ocr = new FakeRecipeOcrService(1000); // 1s delay feels realistic
 * var importVm = new MyImportViewModel(ocr, librarianService);
 * }</pre>
 */
public class FakeRecipeOcrService implements RecipeOcrService {

  private final long delayMs;

  /**
   * Creates a fake OCR service with the specified simulated delay.
   *
   * @param delayMs how long to sleep before returning the canned recipe (in milliseconds)
   */
  public FakeRecipeOcrService(long delayMs) {
    this.delayMs = delayMs;
  }

  @Override
  public Recipe extractRecipe(Path imagePath) throws OcrException {
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
      throw new OcrException("Import cancelled", e);
    }
    String filename = imagePath.getFileName().toString();
    return new Recipe(
        null,
        "Imported: " + filename,
        null,
        List.of(new VagueIngredient("placeholder ingredient", "edit me", null, null)),
        List.of(new Instruction(1, "Imported from " + filename + " — edit these steps", List.of())),
        List.of());
  }
}
