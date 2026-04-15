package app.cookyourbooks.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.model.Recipe;

/**
 * Shared navigation state for the CookYourBooks GUI.
 *
 * <p>This service holds the current view and any context needed for cross-feature navigation (e.g.,
 * which recipe to open in the editor). It is shared across all feature ViewModels and the main
 * layout controller.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Navigate to a view
 * navigationService.navigateTo(NavigationService.View.LIBRARY);
 *
 * // Navigate to a specific recipe in the editor
 * navigationService.navigateToRecipe("recipe-id-123");
 *
 * // Listen for navigation changes in MainViewController
 * navigationService.currentViewProperty().addListener((obs, oldView, newView) -> {
 *   // swap the content pane
 * });
 * }</pre>
 */
public class NavigationService {

  /** The available views in the application. */
  public enum View {
    LIBRARY,
    RECIPE_EDITOR,
    IMPORT,
    SEARCH
  }

  private final ObjectProperty<View> currentView = new SimpleObjectProperty<>(View.LIBRARY);
  private final StringProperty selectedRecipeId = new SimpleStringProperty();
  private final ObjectProperty<Recipe> draftRecipe = new SimpleObjectProperty<>(null);
  private final StringProperty pendingTargetCollectionId = new SimpleStringProperty();

  /**
   * Navigates to the specified view.
   *
   * @param view the view to navigate to
   */
  public void navigateTo(View view) {
    currentView.set(view);
  }

  /**
   * Navigates to the Recipe Editor with the specified recipe selected.
   *
   * @param recipeId the ID of the recipe to open
   */
  public void navigateToRecipe(String recipeId) {
    selectedRecipeId.set(recipeId);
    pendingTargetCollectionId.set(null);
    currentView.set(View.RECIPE_EDITOR);
  }

  /** Returns the current view property (for binding/listening). */
  public ObjectProperty<View> currentViewProperty() {
    return currentView;
  }

  /** Returns the current view. */
  public View getCurrentView() {
    return currentView.get();
  }

  /** Returns the selected recipe ID property (set when navigating to the editor). */
  public StringProperty selectedRecipeIdProperty() {
    return selectedRecipeId;
  }

  /** Returns the currently selected recipe ID, or null if none. */
  public @Nullable String getSelectedRecipeId() {
    return selectedRecipeId.get();
  }

  public void navigateToEditorWithDraft(Recipe draftRecipe) {
    this.draftRecipe.set(draftRecipe);
    this.selectedRecipeId.set(null); // clear ID-based load
    currentView.set(View.RECIPE_EDITOR);
  }

  public Recipe getDraftRecipe() {
    return draftRecipe.get();
  }

  public ObjectProperty<Recipe> draftRecipeProperty() {
    return draftRecipe;
  }

  public StringProperty pendingTargetCollectionIdProperty() {
    return pendingTargetCollectionId;
  }

  public @Nullable String getPendingTargetCollectionId() {
    return pendingTargetCollectionId.get();
  }

  public void setPendingTargetCollectionId(@Nullable String collectionId) {
    pendingTargetCollectionId.set(collectionId);
  }
}
