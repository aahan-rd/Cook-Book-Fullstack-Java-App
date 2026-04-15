package app.cookyourbooks.gui.viewmodel;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

/**
 * ViewModel interface for the Import Interface feature.
 *
 * <p>Import recipes from images using OCR. This ViewModel manages a state machine for the import
 * workflow and supports pre-save editing of the extracted recipe.
 *
 * <h2>State machine</h2>
 *
 * <p>Your ViewModel must manage these state transitions:
 *
 * <pre>
 *   IDLE ──startImport()──▶ PROCESSING ──(success)──▶ REVIEW ──acceptImport()──▶ IDLE
 *                               │                       │
 *                               │                       └──rejectImport()──▶ IDLE
 *                               │
 *                               └──(failure)──▶ ERROR ──(reset)──▶ IDLE
 *                               │
 *                               └──cancelImport()──▶ IDLE
 * </pre>
 *
 * <p>You define your own state enum or representation. The grading accessor {@link #getState()}
 * returns a string so tests don't depend on your enum type. Return one of: {@code "idle"}, {@code
 * "processing"}, {@code "review"}, or {@code "error"}.
 *
 * <h2>Threading</h2>
 *
 * <p>Use {@code BackgroundTaskRunner} to run the OCR operation on a background thread. Inject
 * {@code FakeRecipeOcrService} (provided in the handout test fixtures) for development and testing.
 *
 * <h2>Requirement mapping</h2>
 *
 * <ul>
 *   <li><b>I1:</b> Initial state is idle; no imported recipe, no error
 *   <li><b>I2:</b> {@link #startImport(Path)} transitions to processing
 *   <li><b>I3:</b> Successful OCR transitions to review; imported recipe is populated
 *   <li><b>I4:</b> OCR failure transitions to error; error message is populated
 *   <li><b>I5:</b> {@link #cancelImport()} during processing transitions back to idle
 *   <li><b>I6:</b> {@link #acceptImport()} saves to selected collection and transitions to idle
 *   <li><b>I7:</b> {@link #rejectImport()} discards and transitions to idle
 *   <li><b>I8:</b> Available collections are loaded from the repository
 *   <li><b>I9:</b> Pre-save editing: title/ingredients can be modified before accept
 *   <li><b>I10:</b> {@link #acceptImport()} with no collection or no recipe is a no-op
 * </ul>
 */
public interface ImportViewModel {

  // ──────────────────────────────────────────────────────────────────────────
  // Observable properties (for JavaFX binding in the View)
  // ──────────────────────────────────────────────────────────────────────────

  /** A status message describing the current step (e.g., "Extracting recipe...", "Ready"). */
  StringProperty statusMessageProperty();

  /** An error message (populated when in error state). */
  StringProperty errorMessageProperty();

  /** Progress for import operation; negative value can indicate indeterminate/idle. */
  DoubleProperty importProgressProperty();

  /**
   * The current import state as an observable lowercase string (idle, processing, review, error).
   */
  StringProperty stateProperty();

  /**
   * The editable title of the imported recipe (available during review state for pre-save editing).
   */
  StringProperty importedTitleProperty();

  /** Editable metadata summary for the imported recipe, e.g. servings/prep/cook text. */
  StringProperty importedDescriptionProperty();

  /**
   * The editable ingredients of the imported recipe (available during review for pre-save editing).
   * You choose the entry type.
   */
  ObservableList<String> importedIngredientsProperty();

  /** The extracted instructions of the imported recipe, exposed as step text for review. */
  ObservableList<String> importedInstructionsProperty();

  /**
   * The list of available collections to import into. You choose the entry type. Each entry should
   * expose the collection's ID and title.
   */
  ObservableList<?> availableCollectionsProperty();

  // ──────────────────────────────────────────────────────────────────────────
  // Commands (user actions)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Starts an OCR import from the given image file.
   *
   * <p>Transitions from IDLE to PROCESSING. The OCR runs on a background thread via {@code
   * BackgroundTaskRunner}. On success, transitions to REVIEW with the extracted recipe. On failure,
   * transitions to ERROR.
   *
   * @param imagePath path to the image file (JPEG, PNG, or WebP)
   */
  void startImport(Path imagePath);

  /**
   * Cancels the in-progress import.
   *
   * <p>Only valid during PROCESSING state. Transitions back to IDLE.
   *
   * <p><b>Implementation note:</b> {@code Task.cancel()} does not trigger the {@code onFailure}
   * callback — neither callback fires on cancellation. Your cancel method must handle the
   * PROCESSING → IDLE state transition directly.
   */
  void cancelImport();

  /**
   * Accepts the imported recipe, saving it to the selected target collection.
   *
   * <p>Only valid during REVIEW state. Transitions to IDLE. This is a no-op if no collection is
   * selected or no recipe is available.
   */
  void acceptImport();

  /**
   * Rejects the imported recipe, discarding it.
   *
   * <p>Only valid during REVIEW state. Transitions to IDLE.
   */
  void rejectImport();

  /**
   * Selects the target collection for the imported recipe.
   *
   * @param collectionId the ID of the collection to import into
   */
  void selectTargetCollection(String collectionId);

  /** Loads the list of available collections from the repository. */
  void loadCollections();

  /**
   * Resets import state to IDLE from any current state.
   *
   * <p>Clears imported title/ingredients/error, sets progress to 0.0, and status to "Ready".
   */
  void resetToIdle();

  // ──────────────────────────────────────────────────────────────────────────
  // Non-JavaFX accessors (for grading tests)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Returns the current state as a lowercase string: {@code "idle"}, {@code "processing"}, {@code
   * "review"}, or {@code "error"}.
   */
  String getState();

  /** Returns the current status message. */
  String getStatusMessage();

  /** Returns the current error message, or null if not in error state. */
  @Nullable String getErrorMessage();

  /** Returns the title of the imported recipe, or null if not in review state. */
  @Nullable String getImportedRecipeTitle();

  /** Returns the ingredient names of the imported recipe. Empty if not in review state. */
  List<String> getImportedIngredientNames();

  /** Returns the IDs of the available collections. */
  List<String> getAvailableCollectionIds();

  /** Returns the ID of the selected target collection, or null if none. */
  @Nullable String getSelectedCollectionId();
}
