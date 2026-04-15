package app.cookyourbooks.gui.viewmodel;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

/** ViewModel interface for the Library View feature. */
public interface LibraryViewModel {

  /** Synthetic collection ID used by the always-present "All recipes" card in the Library UI. */
  String ALL_RECIPES_ID = "__all_recipes__";

  /** Filtered list of collection cards shown in the left panel. */
  ObservableList<?> collectionsProperty();

  /** Query used to filter collection cards by title immediately (in-memory, no debounce). */
  StringProperty queryProperty();

  /** Debounced query used to filter recipes by title. */
  StringProperty recipeQueryProperty();

  /** Legacy alias retained for compatibility with existing tests/calls. */
  default StringProperty filterTextProperty() {
    return queryProperty();
  }

  /** Recipes in the currently selected collection. */
  ObservableList<?> recipesProperty();

  /** Whether collections are currently loading from the service layer. */
  BooleanProperty loadingProperty();

  /** Whether undo is available after deletion. */
  BooleanProperty undoAvailableProperty();

  /** Human-readable undo message (for example, "Deleted: Breakfast"). */
  StringProperty undoMessageProperty();

  /** Loads/reloads collections from the service layer. */
  void refresh();

  /** Selects a collection by ID. */
  void selectCollection(String collectionId);

  /**
   * Sets the collection query text.
   *
   * <p>The implementation applies this filter to collection cards immediately.
   */
  void setQuery(String query);

  /** Sets the debounced recipes query text. */
  void setRecipeQuery(String query);

  /** Creates a new collection with the given title. */
  void createCollection(String title);

  /** Deletes a collection by ID with undo window semantics. */
  void deleteCollection(String collectionId);

  /** Deletes a recipe from the selected scope with undo window semantics. */
  void deleteRecipe(String recipeId);

  /** Restores the most recently deleted collection if undo is still available. */
  void undoDelete();

  /** Selects a recipe from the currently selected collection for navigation. */
  void selectRecipe(String recipeId);

  /** Returns IDs of all current collections (excluding synthetic entries). */
  List<String> getCollectionIds();

  /** Returns selected collection ID, or null when none is selected. */
  @Nullable String getSelectedCollectionId();

  /** Returns the current query text. */
  String getQuery();

  /** Returns the current recipe query text. */
  String getRecipeQuery();

  /** Returns IDs of recipes in the selected collection. */
  List<String> getRecipeIds();

  /** Returns whether collections are currently loading. */
  boolean isLoading();

  /** Returns whether undo is currently available. */
  boolean isUndoAvailable();

  /** Legacy alias retained for compatibility with existing tests/calls. */
  default String getFilterText() {
    return getQuery();
  }
}
