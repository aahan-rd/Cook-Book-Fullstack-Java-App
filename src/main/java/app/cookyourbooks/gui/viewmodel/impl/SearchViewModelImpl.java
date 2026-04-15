package app.cookyourbooks.gui.viewmodel.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.util.Duration;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.viewmodel.SearchViewModel;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;

/**
 * Default implementation of {@link SearchViewModel}.
 *
 * <p><b>Responsibilities:</b>
 *
 * <ul>
 *   <li>Manage search query and debounced async search execution
 *   <li>Apply ingredient and collection filters with AND logic
 *   <li>Maintain result list and keyboard navigation state
 *   <li>Handle race conditions via generation counter
 *   <li>Expose observables for JavaFX binding (query, results, filters, searching, status)
 * </ul>
 *
 * <p><b>Search Flow:</b>
 *
 * <ol>
 *   <li>User types query → {@link #setQuery(String)} called
 *   <li>Debounce timer starts (300ms)
 *   <li>If query changes again, timer resets
 *   <li>Timer fires → {@link #executeSearch()} on background thread
 *   <li>Results filtered by active ingredient/collection filters (AND logic)
 *   <li>Results pushed to UI, selection synced
 * </ol>
 *
 * <p><b>Race Condition Handling:</b>
 *
 * <p>Uses {@code searchGeneration} counter. Each new search increments it. Results are only
 * accepted if their generation matches current {@code searchGeneration}. This prevents stale
 * results from overwriting newer ones.
 *
 * @see SearchViewModel
 * @see app.cookyourbooks.services.LibrarianService
 */
public final class SearchViewModelImpl implements SearchViewModel {

  private static final Duration DEFAULT_DEBOUNCE_DELAY = Duration.millis(300);

  private final LibrarianService librarianService;
  private final NavigationService navigationService;
  private final Duration debounceDelay;

  private final StringProperty query = new SimpleStringProperty("");
  private final ObservableList<Recipe> results = FXCollections.observableArrayList();
  private final ObservableList<String> ingredientFilters = FXCollections.observableArrayList();
  private final ObservableList<String> collectionFilters = FXCollections.observableArrayList();
  private final BooleanProperty searching = new SimpleBooleanProperty(false);
  private final StringProperty statusMessage = new SimpleStringProperty("");
  private final ObjectProperty<@Nullable String> selectedResultId = new SimpleObjectProperty<>();

  private final PauseTransition debounceTimer;

  private int searchGeneration;
  private @Nullable Task<?> currentTask;

  /**
   * Constructs a SearchViewModelImpl with default 300ms debounce delay.
   *
   * @param librarianService service for recipe lookup and filtering
   * @param navigationService service for view navigation
   * @throws NullPointerException if either service is null
   */
  public SearchViewModelImpl(
      LibrarianService librarianService, NavigationService navigationService) {
    this(librarianService, navigationService, DEFAULT_DEBOUNCE_DELAY);
  }

  /**
   * Constructs a SearchViewModelImpl with custom debounce delay.
   *
   * <p>Useful for testing with shorter delays.
   *
   * @param librarianService service for recipe lookup and filtering
   * @param navigationService service for view navigation
   * @param debounceDelay time to wait after last keystroke before searching
   * @throws NullPointerException if any parameter is null
   */
  public SearchViewModelImpl(
      LibrarianService librarianService,
      NavigationService navigationService,
      Duration debounceDelay) {
    this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
    this.navigationService = Objects.requireNonNull(navigationService, "navigationService");
    this.debounceDelay = Objects.requireNonNull(debounceDelay, "debounceDelay");

    debounceTimer = new PauseTransition(this.debounceDelay);
    debounceTimer.setOnFinished(e -> executeSearch());

    refreshSearchImmediately();
  }

  @Override
  public StringProperty queryProperty() {
    return query;
  }

  @Override
  public ObservableList<?> resultsProperty() {
    return results;
  }

  @Override
  public ObservableList<String> ingredientFiltersProperty() {
    return ingredientFilters;
  }

  @Override
  public void setSelectedResultId(@Nullable String recipeId) {
    selectedResultId.set(recipeId);
  }

  @Override
  public ObservableList<String> collectionFiltersProperty() {
    return collectionFilters;
  }

  @Override
  public BooleanProperty searchingProperty() {
    return searching;
  }

  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  @Override
  public void setQuery(String queryText) {
    query.set(queryText == null ? "" : queryText.trim());
    debounceTimer.stop();
    debounceTimer.playFromStart();
  }

  @Override
  public void addIngredientFilter(String ingredient) {
    String normalized = normalizeFilter(ingredient);
    if (normalized.isEmpty() || containsFilter(normalized)) {
      return;
    }

    ingredientFilters.add(normalized);
    executeSearch();
  }

  @Override
  public void removeIngredientFilter(String ingredient) {
    String normalized = normalizeFilter(ingredient);
    if (normalized.isEmpty()) {
      return;
    }

    boolean removed = ingredientFilters.removeIf(existing -> existing.equalsIgnoreCase(normalized));
    if (removed) {
      executeSearch();
    }
  }

  @Override
  public void addCollectionFilter(String collection) {
    String normalized = normalizeFilter(collection);
    if (normalized.isEmpty() || containsFilter(normalized)) {
      return;
    }

    collectionFilters.add(normalized);
    executeSearch();
  }

  @Override
  public void removeCollectionFilter(String collection) {
    String normalized = normalizeFilter(collection);
    if (normalized.isEmpty()) {
      return;
    }

    boolean removed = collectionFilters.removeIf(existing -> existing.equalsIgnoreCase(normalized));
    if (removed) {
      executeSearch();
    }
  }

  @Override
  public void clearFilters() {
    debounceTimer.stop();
    cancelCurrentTask();
    query.set("");
    ingredientFilters.clear();
    collectionFilters.clear();
    selectedResultId.set(null);
    executeSearch();
  }

  @Override
  public void selectNextResult() {
    if (results.isEmpty()) {
      selectedResultId.set(null);
      return;
    }

    int currentIndex = indexOfSelectedResult();
    int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % results.size();
    selectedResultId.set(results.get(nextIndex).getId());
  }

  @Override
  public void selectPreviousResult() {
    if (results.isEmpty()) {
      selectedResultId.set(null);
      return;
    }

    int currentIndex = indexOfSelectedResult();
    int previousIndex =
        currentIndex < 0
            ? results.size() - 1
            : (currentIndex - 1 + results.size()) % results.size();
    selectedResultId.set(results.get(previousIndex).getId());
  }

  @Override
  public void navigateToSelectedResult() {
    String id = selectedResultId.get();
    System.out.println("DEBUG: navigateToSelectedResult selectedResultId=" + id);
    if (id != null && !id.isBlank()) {
      System.out.println("DEBUG: navigateToRecipe id=" + id);
      navigationService.navigateToRecipe(id);
    }
  }

  @Override
  public String getQuery() {
    return query.get();
  }

  @Override
  public List<String> getResultIds() {
    return results.stream().map(Recipe::getId).toList();
  }

  @Override
  public List<String> getIngredientFilters() {
    return List.copyOf(ingredientFilters);
  }

  @Override
  public List<String> getCollectionFilters() {
    return List.copyOf(collectionFilters);
  }

  @Override
  public boolean isSearching() {
    return searching.get();
  }

  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  @Override
  public @Nullable String getSelectedResultId() {
    return selectedResultId.get();
  }

  private void refreshSearchImmediately() {
    executeSearch();
  }

  private void executeSearch() {
    cancelCurrentTask();

    final int generation = ++searchGeneration;
    final String currentQuery = query.get();
    final List<String> currentIngredientFilters = List.copyOf(ingredientFilters);
    final List<String> currentCollectionFilters = List.copyOf(collectionFilters);

    searching.set(true);
    statusMessage.set("Searching...");

    currentTask =
        BackgroundTaskRunner.run(
            () -> performSearch(currentQuery, currentIngredientFilters, currentCollectionFilters),
            resultList -> {
              if (generation != searchGeneration) {
                return;
              }

              updateResults(resultList);
              searching.set(false);
              updateStatusMessage(resultList.size());
            },
            error -> {
              if (generation != searchGeneration) {
                return;
              }

              searching.set(false);
              statusMessage.set("Search failed: " + error.getMessage());
            });
  }

  private List<Recipe> performSearch(
      String queryText, List<String> ingredientFilters, List<String> collectionFilters) {
    List<Recipe> titleMatches;
    String trimmedQuery = queryText.trim();

    if (trimmedQuery.isEmpty()) {
      titleMatches = librarianService.listAllRecipes();
    } else {
      titleMatches = librarianService.resolveRecipes(trimmedQuery);
    }

    if (ingredientFilters.isEmpty() && collectionFilters.isEmpty()) {
      return titleMatches;
    }

    if (titleMatches.isEmpty()) {
      return List.of();
    }

    Set<String> matchingIds = new LinkedHashSet<>();
    for (Recipe recipe : titleMatches) {
      matchingIds.add(recipe.getId());
    }

    // Apply ingredient filters with AND logic
    for (String filter : ingredientFilters) {
      Set<String> ingredientMatchIds =
          librarianService.searchByIngredient(filter).stream()
              .map(Recipe::getId)
              .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
      matchingIds.retainAll(ingredientMatchIds);
      if (matchingIds.isEmpty()) {
        return List.of();
      }
    }

    // Apply collection filters with AND logic
    for (String filter : collectionFilters) {
      Set<String> collectionMatchIds =
          librarianService.listRecipes(filter).stream()
              .map(Recipe::getId)
              .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
      matchingIds.retainAll(collectionMatchIds);
      if (matchingIds.isEmpty()) {
        return List.of();
      }
    }

    return titleMatches.stream().filter(recipe -> matchingIds.contains(recipe.getId())).toList();
  }

  private void updateResults(List<Recipe> newResults) {
    String previouslySelected = selectedResultId.get();
    results.setAll(newResults);

    if (newResults.isEmpty()) {
      selectedResultId.set(null);
      return;
    }

    if (previouslySelected != null && containsRecipeId(newResults, previouslySelected)) {
      selectedResultId.set(previouslySelected);
      return;
    }

    selectedResultId.set(newResults.get(0).getId());
  }

  private void updateStatusMessage(int count) {
    statusMessage.set(
        switch (count) {
          case 0 -> "No results found";
          case 1 -> "1 result";
          default -> count + " results";
        });
  }

  private void cancelCurrentTask() {
    Task<?> task = currentTask;
    if (task != null && task.isRunning()) {
      task.cancel();
    }
    currentTask = null;
  }

  private int indexOfSelectedResult() {
    String selectedId = selectedResultId.get();
    if (selectedId == null) {
      return -1;
    }

    for (int index = 0; index < results.size(); index++) {
      if (selectedId.equals(results.get(index).getId())) {
        return index;
      }
    }
    return -1;
  }

  private boolean containsFilter(String candidate) {
    return ingredientFilters.stream().anyMatch(existing -> existing.equalsIgnoreCase(candidate));
  }

  private static boolean containsRecipeId(List<Recipe> recipes, String recipeId) {
    return recipes.stream().anyMatch(recipe -> recipeId.equals(recipe.getId()));
  }

  @Override
  public List<String> getAvailableIngredients() {
    return librarianService.listAllIngredients();
  }

  @Override
  public List<String> getAvailableCollections() {
    return librarianService.listCollections().stream()
        .map(collection -> collection.getTitle())
        .toList();
  }

  private static String normalizeFilter(String value) {
    return value == null ? "" : value.trim();
  }
}
