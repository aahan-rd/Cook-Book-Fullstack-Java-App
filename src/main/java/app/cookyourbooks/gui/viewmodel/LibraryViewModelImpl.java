package app.cookyourbooks.gui.viewmodel;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.util.Duration;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.services.LibrarianService;

/** Default implementation of {@link LibraryViewModel}. */
public final class LibraryViewModelImpl implements LibraryViewModel {

  private static final Duration RECIPE_SEARCH_DEBOUNCE = Duration.millis(300);
  private static final String ALL_RECIPES_TITLE = "All recipes";

  private final LibrarianService librarianService;
  private final NavigationService navigationService;

  private final ObservableList<CollectionListItem> allCollections;
  private final FilteredList<CollectionListItem> collections;
  private final StringProperty query;
  private final ObservableList<RecipeListItem> allRecipes;
  private final FilteredList<RecipeListItem> recipes;
  private final StringProperty recipeQuery;
  private final BooleanProperty loading;
  private final BooleanProperty undoAvailable;
  private final StringProperty undoMessage;

  @SuppressWarnings("UnusedVariable")
  private final Duration undoTimeout;

  private final PauseTransition undoTimer;
  private final PauseTransition recipeDebounceTimer;

  @SuppressWarnings("unused")
  private @Nullable Task<?> refreshTask;

  private @Nullable PendingDelete pendingDelete;
  private @Nullable PendingRecipeDelete pendingRecipeDelete;
  private @Nullable String selectedCollectionId;

  /**
   * Creates the Library ViewModel and wires collection/recipe query behavior.
   *
   * <p>Collection filtering is applied immediately. Recipe filtering remains debounced to avoid
   * excessive predicate recalculation while typing. The undo timeout is configurable to allow fast
   * testing (e.g., 50ms) while using 5 seconds in production.
   *
   * @param librarianService service used for collection and recipe operations
   * @param navigationService navigation gateway for opening selected recipes
   * @param undoTimeout duration before undo state expires and delete is committed
   */
  public LibraryViewModelImpl(
      LibrarianService librarianService,
      NavigationService navigationService,
      Duration undoTimeout) {
    this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
    this.navigationService = Objects.requireNonNull(navigationService, "navigationService");
    this.undoTimeout = Objects.requireNonNull(undoTimeout, "undoTimeout");

    this.allCollections = FXCollections.observableArrayList();
    this.collections = new FilteredList<>(allCollections, item -> true);
    this.query = new SimpleStringProperty("");
    this.allRecipes = FXCollections.observableArrayList();
    this.recipes = new FilteredList<>(allRecipes, item -> true);
    this.recipeQuery = new SimpleStringProperty("");
    this.loading = new SimpleBooleanProperty(false);
    this.undoAvailable = new SimpleBooleanProperty(false);
    this.undoMessage = new SimpleStringProperty("");

    this.pendingDelete = null;
    this.pendingRecipeDelete = null;
    this.selectedCollectionId = null;

    this.undoTimer = new PauseTransition(undoTimeout);
    this.undoTimer.setOnFinished(event -> commitPendingDelete());

    this.recipeDebounceTimer = new PauseTransition(RECIPE_SEARCH_DEBOUNCE);
    this.recipeDebounceTimer.setOnFinished(event -> applyRecipeQuery());

    this.query.addListener(
        (obs, oldValue, newValue) -> {
          applyCollectionQuery();
        });

    this.recipeQuery.addListener(
        (obs, oldValue, newValue) -> {
          recipeDebounceTimer.stop();
          recipeDebounceTimer.playFromStart();
        });

    applyCollectionQuery();
    applyRecipeQuery();
  }

  @Override
  public ObservableList<?> collectionsProperty() {
    return collections;
  }

  @Override
  public StringProperty queryProperty() {
    return query;
  }

  @Override
  public StringProperty recipeQueryProperty() {
    return recipeQuery;
  }

  @Override
  public ObservableList<?> recipesProperty() {
    return recipes;
  }

  @Override
  public BooleanProperty loadingProperty() {
    return loading;
  }

  @Override
  public BooleanProperty undoAvailableProperty() {
    return undoAvailable;
  }

  @Override
  public StringProperty undoMessageProperty() {
    return undoMessage;
  }

  @Override
  public void refresh() {
    // Loading is toggled before scheduling background work so the UI can react
    // immediately.
    loading.set(true);
    refreshTask =
        BackgroundTaskRunner.run(
            librarianService::listCollections,
            this::onRefreshSucceeded,
            error -> loading.set(false));
  }

  @Override
  public void selectCollection(String collectionId) {
    if (collectionId == null || collectionId.isBlank()) {
      clearSelection();
      return;
    }

    if (ALL_RECIPES_ID.equals(collectionId)) {
      selectedCollectionId = ALL_RECIPES_ID;
      allRecipes.setAll(
          librarianService.listAllRecipes().stream().map(this::toRecipeItem).toList());
      return;
    }

    librarianService
        .findCollectionById(collectionId)
        .ifPresentOrElse(
            collection -> {
              selectedCollectionId = collection.getId();
              allRecipes.setAll(toRecipeItems(collection));
            },
            this::clearSelection);
  }

  @Override
  public void setQuery(String queryText) {
    // Keep canonical query state trimmed so filtering and getter behavior are
    // consistent.
    query.set(queryText == null ? "" : queryText.trim());
  }

  @Override
  public void setRecipeQuery(String queryText) {
    // Keep canonical query state trimmed so filtering and getter behavior are
    // consistent.
    recipeQuery.set(queryText == null ? "" : queryText.trim());
  }

  @Override
  public void createCollection(String title) {
    RecipeCollection created = librarianService.createCollection(title);
    if (created == null) {
      return;
    }
    if (allCollections.isEmpty()) {
      allCollections.add(allRecipesCollectionItem());
    }
    allCollections.add(toCollectionItem(created));
    selectCollection(created.getId());
  }

  @Override
  public void deleteCollection(String collectionId) {
    if (collectionId == null || collectionId.isBlank() || ALL_RECIPES_ID.equals(collectionId)) {
      return;
    }

    int index = findCollectionIndexById(collectionId);
    if (index < 0) {
      return;
    }

    if (pendingDelete != null || pendingRecipeDelete != null) {
      commitPendingDelete();
    }

    CollectionListItem removed = allCollections.remove(index);
    pendingDelete = new PendingDelete(removed, index);
    undoAvailable.set(true);
    undoMessage.set("Deleted: " + removed.title());
    undoTimer.playFromStart();

    if (collectionId.equals(selectedCollectionId)) {
      clearSelection();
    }
  }

  @Override
  public void deleteRecipe(String recipeId) {
    if (recipeId == null || recipeId.isBlank() || selectedCollectionId == null) {
      return;
    }

    Recipe recipe = findRecipeById(recipeId);
    if (recipe == null) {
      return;
    }

    if (pendingDelete != null || pendingRecipeDelete != null) {
      commitPendingDelete();
    }

    if (ALL_RECIPES_ID.equals(selectedCollectionId)) {
      List<String> affectedCollectionIds =
          librarianService.listCollections().stream()
              .filter(collection -> collection.findRecipeById(recipeId).isPresent())
              .map(RecipeCollection::getId)
              .toList();
      librarianService.deleteRecipe(recipeId);
      pendingRecipeDelete =
          new PendingRecipeDelete(recipe, null, affectedCollectionIds, true, recipe.getTitle());
      adjustAllRecipesCount(-1);
      affectedCollectionIds.forEach(id -> adjustCollectionCount(id, -1));
    } else {
      String collectionId = selectedCollectionId;
      librarianService.removeRecipeFromCollection(collectionId, recipeId);
      pendingRecipeDelete =
          new PendingRecipeDelete(
              recipe, collectionId, List.of(collectionId), false, recipe.getTitle());
      adjustCollectionCount(collectionId, -1);
    }

    allRecipes.removeIf(item -> item.id().equals(recipeId));
    undoAvailable.set(true);
    undoMessage.set("Deleted recipe: " + recipe.getTitle());
    undoTimer.playFromStart();
  }

  @Override
  public void undoDelete() {
    if (pendingDelete == null && pendingRecipeDelete == null) {
      return;
    }

    undoTimer.stop();
    if (pendingRecipeDelete != null) {
      restorePendingRecipeDelete();
    } else {
      restorePendingDelete();
    }
    clearUndoState();
  }

  @Override
  public void selectRecipe(String recipeId) {
    if (recipeId == null || recipeId.isBlank()) {
      return;
    }

    boolean existsInSelection = recipes.stream().anyMatch(recipe -> recipe.id().equals(recipeId));
    if (!existsInSelection) {
      return;
    }

    navigationService.navigateToRecipe(recipeId);
  }

  @Override
  public List<String> getCollectionIds() {
    return allCollections.stream()
        .map(CollectionListItem::id)
        .filter(id -> !ALL_RECIPES_ID.equals(id))
        .toList();
  }

  @Override
  public @Nullable String getSelectedCollectionId() {
    return selectedCollectionId;
  }

  @Override
  public String getQuery() {
    return query.get();
  }

  @Override
  public String getRecipeQuery() {
    return recipeQuery.get();
  }

  @Override
  public List<String> getRecipeIds() {
    return recipes.stream().map(RecipeListItem::id).toList();
  }

  @Override
  public boolean isLoading() {
    return loading.get();
  }

  @Override
  public boolean isUndoAvailable() {
    return undoAvailable.get();
  }

  /** Clears collection selection and the corresponding recipe panel state. */
  private void clearSelection() {
    selectedCollectionId = null;
    allRecipes.clear();
  }

  /** Looks up a recipe ID from the global library snapshot. */
  private @Nullable Recipe findRecipeById(String recipeId) {
    for (Recipe recipe : librarianService.listAllRecipes()) {
      if (recipeId.equals(recipe.getId())) {
        return recipe;
      }
    }
    return null;
  }

  /** Converts a selected collection into lightweight recipe list entries for the UI. */
  private List<RecipeListItem> toRecipeItems(RecipeCollection collection) {
    return collection.getRecipes().stream().map(this::toRecipeItem).toList();
  }

  /** Converts a domain recipe into its list row projection. */
  private RecipeListItem toRecipeItem(Recipe recipe) {
    return new RecipeListItem(recipe.getId(), recipe.getTitle());
  }

  /**
   * Handles completion of background refresh by replacing collection cards and preserving selection
   * state when possible.
   */
  private void onRefreshSucceeded(List<RecipeCollection> refreshedCollections) {
    String pendingCollectionId = pendingDelete != null ? pendingDelete.item().id() : null;
    List<CollectionListItem> refreshedItems =
        refreshedCollections.stream()
            .map(this::toCollectionItem)
            .filter(item -> pendingCollectionId == null || !item.id().equals(pendingCollectionId))
            .toList();
    allCollections.setAll(withAllRecipesItem(refreshedItems));
    loading.set(false);

    if (selectedCollectionId != null) {
      selectCollection(selectedCollectionId);
    }

    applyCollectionQuery();
  }

  /** Applies the in-memory collection title filter immediately. */
  private void applyCollectionQuery() {
    String rawQuery = query.get();
    String normalized = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      collections.setPredicate(item -> true);
      return;
    }
    collections.setPredicate(item -> item.title().toLowerCase(Locale.ROOT).contains(normalized));
  }

  /** Applies the in-memory recipe title filter after debounce. */
  private void applyRecipeQuery() {
    String rawQuery = recipeQuery.get();
    String normalized = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      recipes.setPredicate(item -> true);
      return;
    }
    recipes.setPredicate(item -> item.title().toLowerCase(Locale.ROOT).contains(normalized));
  }

  /** Maps a domain collection to a card row model used by the left panel. */
  private CollectionListItem toCollectionItem(RecipeCollection collection) {
    return new CollectionListItem(
        collection.getId(),
        collection.getTitle(),
        collection.getSourceType().name(),
        collection.getRecipes().size());
  }

  /** Builds the synthetic "All recipes" card shown at the top of the collection list. */
  private CollectionListItem allRecipesCollectionItem() {
    return new CollectionListItem(
        ALL_RECIPES_ID, ALL_RECIPES_TITLE, "SYSTEM", safeAllRecipeCount());
  }

  /** Safely computes total recipe count for the synthetic all-recipes card. */
  private int safeAllRecipeCount() {
    try {
      return librarianService.listAllRecipes().size();
    } catch (RuntimeException ignored) {
      return 0;
    }
  }

  /** Prepends the synthetic all-recipes card to a list of real collection cards. */
  private List<CollectionListItem> withAllRecipesItem(List<CollectionListItem> items) {
    List<CollectionListItem> withAll = new java.util.ArrayList<>();
    withAll.add(allRecipesCollectionItem());
    withAll.addAll(items);
    return withAll;
  }

  /** Finds the list index for a collection card by ID, or {@code -1} when absent. */
  private int findCollectionIndexById(String collectionId) {
    for (int i = 0; i < allCollections.size(); i++) {
      if (allCollections.get(i).id().equals(collectionId)) {
        return i;
      }
    }
    return -1;
  }

  /** Restores a pending collection deletion to its original approximate index. */
  private void restorePendingDelete() {
    if (pendingDelete == null) {
      return;
    }

    int restoreIndex = Math.min(pendingDelete.originalIndex(), allCollections.size());
    allCollections.add(restoreIndex, pendingDelete.item());
  }

  /** Restores a pending recipe deletion either to one collection or to all affected collections. */
  private void restorePendingRecipeDelete() {
    if (pendingRecipeDelete == null) {
      return;
    }

    if (pendingRecipeDelete.deletedFromLibrary()) {
      pendingRecipeDelete
          .affectedCollectionIds()
          .forEach(
              collectionId ->
                  librarianService.saveRecipe(pendingRecipeDelete.recipe(), collectionId));
      adjustAllRecipesCount(1);
      pendingRecipeDelete.affectedCollectionIds().forEach(id -> adjustCollectionCount(id, 1));
    } else if (pendingRecipeDelete.targetCollectionId() != null) {
      librarianService.saveRecipe(
          pendingRecipeDelete.recipe(), pendingRecipeDelete.targetCollectionId());
      adjustCollectionCount(pendingRecipeDelete.targetCollectionId(), 1);
    }

    if (selectedCollectionId != null) {
      selectCollection(selectedCollectionId);
    }
  }

  /** Adjusts the recipe count displayed on the synthetic all-recipes card. */
  private void adjustAllRecipesCount(int delta) {
    adjustCollectionCount(ALL_RECIPES_ID, delta);
  }

  /** Adjusts the displayed recipe count for a specific collection card. */
  private void adjustCollectionCount(String collectionId, int delta) {
    for (int i = 0; i < allCollections.size(); i++) {
      CollectionListItem item = allCollections.get(i);
      if (!item.id().equals(collectionId)) {
        continue;
      }
      int updatedCount = Math.max(0, item.recipeCount() + delta);
      allCollections.set(
          i, new CollectionListItem(item.id(), item.title(), item.sourceType(), updatedCount));
      return;
    }
  }

  /**
   * Commits any pending delete once the undo window expires or a new delete replaces it.
   *
   * <p>Recipe deletions are committed eagerly in service calls, so this method only clears their
   * undo state.
   */
  private void commitPendingDelete() {
    undoTimer.stop();
    if (pendingRecipeDelete != null) {
      clearUndoState();
      return;
    }

    if (pendingDelete == null) {
      clearUndoState();
      return;
    }

    librarianService.deleteCollection(pendingDelete.item().id());
    clearUndoState();
  }

  /** Clears all undo-related state and hides the undo banner. */
  private void clearUndoState() {
    undoAvailable.set(false);
    undoMessage.set("");
    pendingDelete = null;
    pendingRecipeDelete = null;
  }

  private record CollectionListItem(String id, String title, String sourceType, int recipeCount) {}

  private record PendingDelete(CollectionListItem item, int originalIndex) {}

  private record PendingRecipeDelete(
      Recipe recipe,
      @Nullable String targetCollectionId,
      List<String> affectedCollectionIds,
      boolean deletedFromLibrary,
      String title) {}

  private record RecipeListItem(String id, String title) {}
}
