package app.cookyourbooks.gui.editor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.shared.EditableIngredient;
import app.cookyourbooks.gui.view.RecipeEditorViewController;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModel;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.repository.RecipeRepository;

/**
 * Concrete implementation of {@link RecipeEditorViewModel}.
 *
 * <p><b>Responsibilities:</b>
 *
 * <ul>
 *   <li>Load recipes from {@link RecipeRepository} when navigated to
 *   <li>Manage edit mode, dirty state, and validation
 *   <li>Support add, remove, and reorder of ingredients
 *   <li>Persist edits asynchronously via {@link BackgroundTaskRunner}
 *   <li>Expose observable JavaFX properties for the View to bind to
 * </ul>
 *
 * <p><b>Dirty tracking:</b> A {@code suppressDirty} flag prevents programmatic resets
 * (loadProperties, discardChanges) from falsely triggering dirty = true.
 *
 * <p><b>Ingredient reordering:</b> {@link #moveIngredientUp(int)} and {@link
 * #moveIngredientDown(int)} are not on the interface — they are called directly by the View
 * controller via a cast.
 */
public class RecipeEditorViewModelImpl implements RecipeEditorViewModel {

  // ── Injected dependency ─────────────────────────────────────────────────

  private final RecipeRepository recipeRepository;
  private final Consumer<Recipe> onSaveSuccess;

  // ── Internal state ──────────────────────────────────────────────────────

  /** Last successfully loaded recipe — used for discard and building edits. */
  @Nullable private Recipe originalRecipe;

  /**
   * Step edits staged by the View before calling save(). Null means use
   * originalRecipe.getInstructions() unchanged.
   */
  @Nullable private List<Instruction> pendingInstructions = null;

  /**
   * When true, property changes do not set dirty = true. Used during programmatic resets
   * (loadProperties, discardChanges).
   */
  private boolean suppressDirty = false;

  // ── Observable properties ───────────────────────────────────────────────

  private final StringProperty title = new SimpleStringProperty("");
  private final ObservableList<EditableIngredient> ingredients =
      FXCollections.observableArrayList();
  private final BooleanProperty editing = new SimpleBooleanProperty(false);
  private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
  private final BooleanProperty isValid = new SimpleBooleanProperty(false);
  private final BooleanProperty isSaving = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");

  // ── Constructor ─────────────────────────────────────────────────────────

  /**
   * Creates a new RecipeEditorViewModelImpl.
   *
   * <p>Called once in {@code CookYourBooksApp.main()} and passed to {@code NavigationService} so it
   * can call {@link #loadRecipe(String)} when routing to this feature.
   *
   * @param recipeRepository repository for loading and saving recipes
   */
  public RecipeEditorViewModelImpl(RecipeRepository recipeRepository) {
    this(recipeRepository, savedRecipe -> {});
  }

  public RecipeEditorViewModelImpl(
      RecipeRepository recipeRepository, Consumer<Recipe> onSaveSuccess) {
    this.recipeRepository = recipeRepository;
    this.onSaveSuccess = Objects.requireNonNull(onSaveSuccess, "onSaveSuccess");
    wireListeners();
  }

  // ── Listener wiring ─────────────────────────────────────────────────────

  /** Sets up dirty tracking and validation binding. Called once from constructor. */
  private void wireListeners() {
    // E5: isValid is true when title is non-blank
    isValid.bind(Bindings.createBooleanBinding(() -> !title.get().isBlank(), title));

    // E3: title changes set dirty (unless suppressed)
    title.addListener(
        (obs, oldVal, newVal) -> {
          if (!suppressDirty) {
            isDirty.set(true);
          }
        });

    // E3: ingredient list changes set dirty (unless suppressed)
    ingredients.addListener(
        (ListChangeListener<EditableIngredient>)
            change -> {
              if (!suppressDirty) {
                isDirty.set(true);
              }
            });
  }

  // ── RecipeEditorViewModel — observable properties ───────────────────────
  /** {@inheritDoc} */
  @Override
  public StringProperty titleProperty() {
    return title;
  }

  /** {@inheritDoc} */
  @Override
  public ObservableList<EditableIngredient> ingredientsProperty() {
    return ingredients;
  }

  /** {@inheritDoc} */
  @Override
  public BooleanProperty editingProperty() {
    return editing;
  }

  /** {@inheritDoc} */
  @Override
  public BooleanProperty isDirtyProperty() {
    return isDirty;
  }

  /** {@inheritDoc} */
  @Override
  public BooleanProperty isValidProperty() {
    return isValid;
  }

  /** {@inheritDoc} */
  @Override
  public BooleanProperty isSavingProperty() {
    return isSaving;
  }

  /** {@inheritDoc} */
  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  // ── RecipeEditorViewModel — commands ────────────────────────────────────

  /**
   * {@inheritDoc}
   *
   * <p>Called by {@code NavigationService} when routing to this feature. Fetches the recipe from
   * the repository and resets all editable state.
   */
  @Override
  public void loadRecipe(String recipeId) {
    recipeRepository
        .findById(recipeId)
        .ifPresent(
            recipe -> {
              this.originalRecipe = recipe;
              this.pendingInstructions = null;
              loadProperties(recipe);
              editing.set(false);
              statusMessage.set("");
            });
  }

  /**
   * Loads an in-memory draft recipe directly into the editor without repository lookup.
   *
   * <p>Used for flows like Import -> Recipe Editor where a recipe may not have a persisted ID yet.
   *
   * @param draftRecipe recipe draft to load into editor state
   */
  public void loadDraft(Recipe draftRecipe) {
    this.originalRecipe = draftRecipe;
    this.pendingInstructions = null;
    loadProperties(draftRecipe);
    editing.set(false);
    statusMessage.set("");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Entering edit mode preserves current state. Exiting edit mode is equivalent to
   * discardChanges().
   */
  @Override
  public void toggleEditMode() {
    if (editing.get()) {
      discardChanges();
    } else {
      editing.set(true);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Reverts title and ingredients to the last loaded recipe state. Uses suppressDirty so the
   * reset does not itself trigger dirty = true.
   */
  @Override
  public void discardChanges() {
    if (originalRecipe != null) {
      loadProperties(originalRecipe);
    }
    clearPendingInstructions();
    editing.set(false);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Runs on a background thread via {@link BackgroundTaskRunner}. On success: reloads properties
   * from saved recipe, exits edit mode. On failure: clears isSaving only — preserves dirty state
   * and edit mode.
   */
  @Override
  public void save() {
    // E10: no-op guard
    if (!isDirty.get() || !isValid.get() || isSaving.get()) {
      return;
    }

    isSaving.set(true);
    statusMessage.set("");

    Recipe updated = buildRecipeFromEdits();

    BackgroundTaskRunner.run(
        () -> {
          recipeRepository.save(updated);
          return updated;
        },
        saved -> { // FX thread — success
          this.originalRecipe = saved;
          this.pendingInstructions = null;
          loadProperties(saved);
          onSaveSuccess.accept(saved);
          editing.set(false);
          isSaving.set(false);
          statusMessage.set("Saved successfully.");
        },
        error -> { // FX thread — failure (E9)
          isSaving.set(false);
          statusMessage.set("Save failed: " + error.getMessage());
        });
  }

  /** {@inheritDoc} */
  @Override
  public void addIngredient() {
    ingredients.add(new EditableIngredient());
  }

  /** {@inheritDoc} */
  @Override
  public void removeIngredient(int index) {
    if (index >= 0 && index < ingredients.size()) {
      ingredients.remove(index);
    }
  }

  /**
   * Moves the ingredient at the given index one position up in the list. Sets dirty = true. No-op
   * if already at the top.
   *
   * <p>Not on the {@link RecipeEditorViewModel} interface — called directly by {@link
   * RecipeEditorViewController} via instanceof cast.
   *
   * @param index the index of the ingredient to move up
   */
  public void moveIngredientUp(int index) {
    if (index > 0 && index < ingredients.size()) {
      Collections.swap(ingredients, index, index - 1);
      isDirty.set(true);
    }
  }

  /**
   * Moves the ingredient at the given index one position down in the list. Sets dirty = true. No-op
   * if already at the bottom.
   *
   * <p>Not on the {@link RecipeEditorViewModel} interface — called directly by {@link
   * RecipeEditorViewController} via instanceof cast.
   *
   * @param index the index of the ingredient to move down
   */
  public void moveIngredientDown(int index) {
    if (index >= 0 && index < ingredients.size() - 1) {
      Collections.swap(ingredients, index, index + 1);
      isDirty.set(true);
    }
  }

  // ── RecipeEditorViewModel — non-JavaFX accessors ────────────────────────

  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getRecipeId() {
    return originalRecipe == null ? null : originalRecipe.getId();
  }

  /** {@inheritDoc} */
  @Override
  public String getTitle() {
    return title.get();
  }

  /** {@inheritDoc} */
  @Override
  public int getIngredientCount() {
    return ingredients.size();
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getIngredientNames() {
    return ingredients.stream().map(EditableIngredient::getName).collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEditing() {
    return editing.get();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isDirty() {
    return isDirty.get();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValid() {
    return isValid.get();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSaving() {
    return isSaving.get();
  }

  /** {@inheritDoc} */
  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  /**
   * Returns the instructions from the currently loaded recipe for display.
   *
   * <p>Not on the interface — used by the View for step display and editing.
   *
   * @return instructions list, or empty list if no recipe loaded
   */
  public List<Instruction> getInstructions() {
    if (originalRecipe == null) {
      return List.of();
    }
    return originalRecipe.getInstructions();
  }

  /**
   * Overrides the instructions that will be used when {@link #save()} is called.
   *
   * <p>Not on the interface — called by {@link RecipeEditorViewController} just before calling
   * {@code save()} so that step edits made in the View are included in the persisted recipe.
   *
   * <p>Also sets dirty = true since instructions have changed.
   *
   * @param instructions the updated instruction list to persist on next save
   */
  public void setInstructions(List<Instruction> instructions) {
    this.pendingInstructions = instructions;
    isDirty.set(true);
  }

  /**
   * Clears any pending instruction override. Called by discardChanges() so step edits are also
   * reverted.
   */
  public void clearPendingInstructions() {
    this.pendingInstructions = null;
  }

  // ── Private helpers ─────────────────────────────────────────────────────

  /**
   * Resets all observable properties to reflect the given recipe.
   *
   * <p>Wraps all mutations in {@code suppressDirty} so programmatic resets do not trigger dirty =
   * true. After this method returns, isDirty is always false.
   *
   * @param recipe the recipe to load into the editor
   */
  private void loadProperties(Recipe recipe) {
    suppressDirty = true;
    title.set(recipe.getTitle());
    ingredients.setAll(
        recipe.getIngredients().stream().map(EditableIngredient::new).collect(Collectors.toList()));
    isDirty.set(false);
    suppressDirty = false;
  }

  /**
   * Builds an updated immutable {@link Recipe} from the current editable state.
   *
   * <p>Filters out blank ingredient entries. Preserves original recipe ID, servings, instructions,
   * and conversion rules since those are not editable.
   *
   * @return a new Recipe reflecting the current editor state
   * @throws IllegalStateException if called before any recipe is loaded
   */
  private Recipe buildRecipeFromEdits() {
    if (originalRecipe == null) {
      throw new IllegalStateException("Cannot build recipe from edits: no recipe loaded");
    }

    List<Ingredient> builtIngredients =
        ingredients.stream()
            .filter(e -> !e.isBlank())
            .map(EditableIngredient::toIngredient)
            .collect(Collectors.toList());

    List<Instruction> instructions =
        pendingInstructions != null ? pendingInstructions : originalRecipe.getInstructions();

    return new Recipe(
        originalRecipe.getId(),
        title.get().strip(),
        originalRecipe.getServings(),
        builtIngredients,
        instructions,
        originalRecipe.getConversionRules());
  }
}
