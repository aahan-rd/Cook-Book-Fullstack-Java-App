package app.cookyourbooks.gui.viewmodel;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jspecify.annotations.Nullable;

/**
 * ViewModel interface for the Search &amp; Filter feature.
 *
 * <p>Find recipes across all collections by title and ingredient. This ViewModel handles debounced
 * async search, ingredient filter intersection, and keyboard navigation through results.
 *
 * <h2>Search behavior</h2>
 *
 * <ul>
 *   <li><b>Title search</b> uses {@code LibrarianService.resolveRecipes(query)}
 *   <li><b>Ingredient filter</b> uses {@code LibrarianService.searchByIngredient(ingredient)}
 *   <li>When multiple ingredient filters are active, results must match <b>all</b> of them (AND
 *       logic, not OR)
 *   <li>Search is <b>debounced</b>: wait 300ms after the user stops typing before firing
 *   <li>Search runs on a background thread via {@code BackgroundTaskRunner}
 * </ul>
 *
 * <h2>Race condition to consider</h2>
 *
 * <p>If the user types "cake" and your debounce fires a search, then types "cookies" while the
 * first search is still running, the second search might return <i>before</i> the first. If you
 * blindly accept whichever result arrives, you'll show stale results. Think about how to handle
 * this.
 *
 * <h2>Observable list types</h2>
 *
 * <p>{@link #resultsProperty()} returns {@code ObservableList<?>} — you choose the entry type. Each
 * result entry should make the recipe's ID and title accessible.
 *
 * <h2>Requirement mapping</h2>
 *
 * <ul>
 *   <li><b>S1:</b> Setting the query triggers a search and populates results
 *   <li><b>S2:</b> Title search returns matching recipes via resolveRecipes()
 *   <li><b>S3:</b> Adding an ingredient filter narrows results via searchByIngredient()
 *   <li><b>S4:</b> Multiple ingredient filters use AND logic (intersection)
 *   <li><b>S5:</b> Clearing filters/query resets results
 *   <li><b>S6:</b> Search runs on a background thread; isSearching is true while running
 *   <li><b>S7:</b> Search is debounced (300ms after last keystroke)
 *   <li><b>S8:</b> {@link #selectNextResult()} / {@link #selectPreviousResult()} cycle through
 *       results
 *   <li><b>S9:</b> {@link #navigateToSelectedResult()} provides the selected recipe ID
 *   <li><b>S10:</b> Status message reflects result count
 *   <li><b>S11:</b> Empty query with no filters returns all recipes
 * </ul>
 */
public interface SearchViewModel {

  // ──────────────────────────────────────────────────────────────────────────
  // Observable properties (for JavaFX binding in the View)
  // ──────────────────────────────────────────────────────────────────────────

  /** The search query text (bound to a TextField in the View). */
  StringProperty queryProperty();

  /**
   * The search results. You choose the entry type. Each entry should expose the recipe's ID and
   * title.
   */
  ObservableList<?> resultsProperty();

  /**
   * The list of active ingredient filter terms. These are user-entered strings, not a fixed list.
   */
  ObservableList<String> ingredientFiltersProperty();

  /**
   * The list of active collection/tag filter terms. These are user-entered strings, not a fixed
   * list.
   */
  ObservableList<String> collectionFiltersProperty();

  /** Whether a search is currently running on a background thread. */
  BooleanProperty searchingProperty();

  /** A status message (e.g., "5 results", "No results found", "Searching..."). */
  StringProperty statusMessageProperty();

  // ──────────────────────────────────────────────────────────────────────────
  // Commands (user actions)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Sets the search query and triggers a debounced search.
   *
   * <p>The search fires 300ms after the last call to this method (debounce). If called again within
   * 300ms, the timer resets.
   *
   * <p><b>Important:</b> {@code LibrarianService.resolveRecipes("")} returns an empty list — it is
   * designed for non-empty queries. When the query is empty and no ingredient filters are active
   * (S11), use {@code LibrarianService.listAllRecipes()} instead to return all recipes.
   *
   * @param query the search text
   */
  void setQuery(String query);

  /**
   * Adds an ingredient filter term. Results must match all active ingredient filters (AND logic).
   *
   * @param ingredient the ingredient name to filter by
   */
  void addIngredientFilter(String ingredient);

  /**
   * Removes an ingredient filter term.
   *
   * @param ingredient the ingredient name to remove from filters
   */
  void removeIngredientFilter(String ingredient);

  /**
   * Adds a collection/tag filter term. Results must match all active collection/tag filters (AND
   * logic).
   *
   * @param collection the collection/tag name to filter by
   */
  void addCollectionFilter(String collection);

  /**
   * Sets the ID of the currently selected result.
   *
   * @param recipeId the recipe ID to select, or null to clear selection
   */
  void setSelectedResultId(@Nullable String recipeId);

  /**
   * Removes a collection/tag filter term.
   *
   * @param collection the collection/tag name to remove from filters
   */
  void removeCollectionFilter(String collection);

  /** Clears all filters (ingredient and collection) and the search query, resetting results. */
  void clearFilters();

  /** Moves the selection to the next result in the list (for keyboard navigation). */
  void selectNextResult();

  /** Moves the selection to the previous result in the list (for keyboard navigation). */
  void selectPreviousResult();

  /**
   * Navigates to the currently selected result (opens it in the Recipe Editor via {@code
   * NavigationService}).
   */
  void navigateToSelectedResult();

  // ──────────────────────────────────────────────────────────────────────────
  // Non-JavaFX accessors (for grading tests)
  // ──────────────────────────────────────────────────────────────────────────

  /** Returns the current query text. */
  String getQuery();

  /** Returns the IDs of recipes in the current result list. */
  List<String> getResultIds();

  /** Returns the currently active ingredient filter terms. */
  List<String> getIngredientFilters();

  /** Returns the currently active collection/tag filter terms. */
  List<String> getCollectionFilters();

  /** Returns whether a search is currently in progress. */
  boolean isSearching();

  /**
   * Returns th @Override public List<String> getAvailableIngredients() { return
   * librarianService.listAllIngredients(); // You'll need to add this to LibrarianService
   * } @Override public List<String> getAvailableCollections() { return
   * librarianService.listCollections() .stream() .map(Collection::getTitle) .toList(); }e current
   * status message.
   */
  String getStatusMessage();

  /** Returns the ID of the currently selected result, or null if none. */
  @Nullable String getSelectedResultId();

  /** Returns list of all available ingredients for filters to pick up from. */
  List<String> getAvailableIngredients();

  /** Returns list of all available collectionss for filters to pick up from. */
  List<String> getAvailableCollections();
}
