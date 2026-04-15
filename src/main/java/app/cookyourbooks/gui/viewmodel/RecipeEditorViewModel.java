package app.cookyourbooks.gui.viewmodel;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

/**
 * ViewModel interface for the Recipe Editor feature.
 *
 * <p>View and edit recipe content. This ViewModel loads a recipe, exposes mutable edit state,
 * tracks dirty changes, validates, and saves asynchronously.
 *
 * <h2>Design challenge: dirty tracking</h2>
 *
 * <p>The domain {@code Recipe} is immutable — your ViewModel must maintain <b>mutable copies</b> of
 * the title, ingredients, etc. for editing. When any of these change, {@link #isDirtyProperty()}
 * should become {@code true}. The tricky part: loading a recipe or discarding changes also sets
 * properties (back to their original values), which would falsely trigger dirty detection. You need
 * a mechanism to suppress dirty tracking during programmatic resets. Think about this before you
 * start coding.
 *
 * <h2>Observable list types</h2>
 *
 * <p>{@link #ingredientsProperty()} returns {@code ObservableList<?>} — you choose the entry type.
 * Each ingredient entry should make the ingredient's name and description accessible to the View.
 *
 * <h2>Requirement mapping</h2>
 *
 * <ul>
 *   <li><b>E1:</b> {@link #loadRecipe(String)} populates recipe, title, and ingredient list
 *   <li><b>E2:</b> {@link #toggleEditMode()} enables/disables editing
 *   <li><b>E3:</b> Changing title or ingredients in edit mode sets isDirty to true
 *   <li><b>E4:</b> {@link #discardChanges()} reverts to original state and clears dirty
 *   <li><b>E5:</b> {@link #isValidProperty()} is false when title is blank
 *   <li><b>E6:</b> {@link #addIngredient()} and {@link #removeIngredient(int)} modify the list
 *   <li><b>E7:</b> {@link #save()} persists the edited recipe
 *   <li><b>E8:</b> {@link #save()} runs on a background thread; isSaving is true while in progress
 *   <li><b>E9:</b> Save failure preserves dirty state and shows error message
 *   <li><b>E10:</b> {@link #save()} while not dirty or not valid is a no-op
 * </ul>
 */
public interface RecipeEditorViewModel {

  // ──────────────────────────────────────────────────────────────────────────
  // Observable properties (for JavaFX binding in the View)
  // ──────────────────────────────────────────────────────────────────────────

  /** The editable recipe title (bound to a TextField in the View). */
  StringProperty titleProperty();

  /**
   * The editable list of ingredients. You choose the entry type. Each entry should expose the
   * ingredient's name and description.
   */
  ObservableList<?> ingredientsProperty();

  /** Whether the editor is in edit mode (controls are editable). */
  BooleanProperty editingProperty();

  /** Whether there are unsaved changes. */
  BooleanProperty isDirtyProperty();

  /** Whether the current edits are valid (e.g., title is not blank). */
  BooleanProperty isValidProperty();

  /** Whether a save operation is currently in progress on a background thread. */
  BooleanProperty isSavingProperty();

  /** A status or error message (e.g., "Saved successfully" or "Save failed: ..."). */
  StringProperty statusMessageProperty();

  // ──────────────────────────────────────────────────────────────────────────
  // Commands (user actions)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Loads a recipe by ID, populating the title and ingredient list.
   *
   * @param recipeId the ID of the recipe to load
   */
  void loadRecipe(String recipeId);

  /** Toggles between view mode and edit mode. */
  void toggleEditMode();

  /**
   * Saves the edited recipe to the repository.
   *
   * <p>Must run on a background thread via {@code BackgroundTaskRunner}. While saving, {@link
   * #isSavingProperty()} must be {@code true} and edit controls should be disabled. On success:
   * exit edit mode and show a success message. On failure: stay in edit mode, preserve dirty state,
   * and show an error message.
   *
   * <p>This is a no-op if the recipe is not dirty or not valid.
   */
  void save();

  /** Discards all unsaved changes, reverting to the last-saved recipe state. */
  void discardChanges();

  /** Adds a new blank ingredient to the end of the ingredient list. */
  void addIngredient();

  /**
   * Removes the ingredient at the specified index.
   *
   * @param index the zero-based index of the ingredient to remove
   */
  void removeIngredient(int index);

  // ──────────────────────────────────────────────────────────────────────────
  // Non-JavaFX accessors (for grading tests)
  // ──────────────────────────────────────────────────────────────────────────

  /** Returns the ID of the currently loaded recipe, or null if none. */
  @Nullable String getRecipeId();

  /** Returns the current title text. */
  String getTitle();

  /** Returns the number of ingredients currently in the list. */
  int getIngredientCount();

  /** Returns the names of all ingredients in the list. */
  List<String> getIngredientNames();

  /** Returns whether the editor is in edit mode. */
  boolean isEditing();

  /** Returns whether there are unsaved changes. */
  boolean isDirty();

  /** Returns whether the current edits are valid. */
  boolean isValid();

  /** Returns whether a save is in progress. */
  boolean isSaving();

  /** Returns the current status/error message. */
  String getStatusMessage();
}
