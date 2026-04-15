package app.cookyourbooks.gui.viewmodel.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javafx.application.Platform;
import javafx.util.Duration;

import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.RecipeFixtures;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.LibrarianService;

/**
 * Tests for {@link SearchViewModelImpl}.
 *
 * <p>Verifies observable state, async behavior, debouncing, filtering, selection, and navigation.
 *
 * <p><b>Requirements Mapping (S1–S11):</b>
 *
 * <ul>
 *   <li><b>S1, S2, S11, S10:</b> {@code initialLoad_returnsAllRecipesAndStatusMessage} — Empty
 *       query with no filters returns all recipes; debounce fires; status updates.
 *   <li><b>S1, S2, S7, S10:</b> {@code setQuery_isDebouncedAndUsesLatestValue} — Query triggers
 *       search after 300ms debounce; multiple rapid changes use latest value only.
 *   <li><b>S3, S4, S5, S10:</b> {@code ingredientFilters_useAndLogic_andClearFiltersResetsState} —
 *       Multiple ingredient filters intersect (AND); clearing filters resets all state.
 *   <li><b>S6:</b> {@code searchingFlag_isTrueWhileBackgroundSearchRuns} — isSearching flag true
 *       during async search, false after completion.
 *   <li><b>S8, S9:</b> {@code selectionCyclesAndNavigationUsesSelectedRecipeId} — Up/Down arrows
 *       cycle selection; Enter navigates.
 *   <li><b>S9 (again):</b> {@code selectedResultId_persistsWhenResultsRefresh} — Selection survives
 *       result updates.
 *   <li><b>S9 (again):</b> {@code navigateToSelectedResult_callsNavigationService} — Navigation
 *       service receives selected ID.
 * </ul>
 */
class SearchViewModelImplTest extends ViewModelTestBase {

  private SearchViewModelImpl newViewModel(
      LibrarianService librarianService, NavigationService navigationService) {
    return new SearchViewModelImpl(librarianService, navigationService, Duration.millis(25));
  }

  private static Recipe recipe(String id, String title) {
    return RecipeFixtures.makeRecipe(id, title);
  }

  private static void awaitSearch() throws InterruptedException {
    Thread.sleep(150);
  }

  /** S1, S2, S11, S10: Empty query returns all recipes; status message updates. */
  @Test
  void initialLoad_returnsAllRecipesAndStatusMessage() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);
    Recipe cake = recipe("r1", "Cake");
    Recipe cookies = recipe("r2", "Cookies");
    when(librarianService.listAllRecipes()).thenReturn(List.of(cake, cookies));

    SearchViewModelImpl vm = newViewModel(librarianService, navigationService);
    awaitSearch();
    waitForFxEvents();

    assertThat(vm.getResultIds()).containsExactly("r1", "r2");
    assertThat(vm.getStatusMessage()).isEqualTo("2 results");
    assertThat(vm.isSearching()).isFalse();
  }

  /** S1, S2, S7, S10: Query debounced (25ms); only latest value searches; status updates. */
  @Test
  void setQuery_isDebouncedAndUsesLatestValue() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);
    Recipe cake = recipe("r1", "Cake");
    Recipe cookies = recipe("r2", "Cookies");
    when(librarianService.listAllRecipes()).thenReturn(List.of(cake));
    when(librarianService.resolveRecipes("cake")).thenReturn(List.of(cake));
    when(librarianService.resolveRecipes("cookies")).thenReturn(List.of(cookies));

    SearchViewModelImpl vm = newViewModel(librarianService, navigationService);
    awaitSearch();
    clearInvocations(librarianService);

    vm.setQuery("cake");
    Thread.sleep(5);
    vm.setQuery("cookies");

    awaitSearch();
    waitForFxEvents();

    verify(librarianService, never()).resolveRecipes("cake");
    verify(librarianService).resolveRecipes("cookies");
    assertThat(vm.getResultIds()).containsExactly("r2");
    assertThat(vm.getStatusMessage()).isEqualTo("1 result");
  }

  /** S3, S4, S5, S10: Ingredient filters use AND logic; clearing filters resets results. */
  @Test
  void ingredientFilters_useAndLogic_andClearFiltersResetsState() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);
    Recipe cake = recipe("r1", "Cake");
    Recipe cookies = recipe("r2", "Cookies");
    Recipe pie = recipe("r3", "Pie");

    when(librarianService.listAllRecipes()).thenReturn(List.of(cake, cookies, pie));
    when(librarianService.searchByIngredient("flour")).thenReturn(List.of(cake, cookies));
    when(librarianService.searchByIngredient("chocolate")).thenReturn(List.of(cake, pie));

    SearchViewModelImpl vm = newViewModel(librarianService, navigationService);
    awaitSearch();
    clearInvocations(librarianService);

    // S3: Add first ingredient filter
    Platform.runLater(() -> vm.addIngredientFilter("flour"));
    awaitSearch();
    waitForFxEvents();
    assertThat(vm.getResultIds()).containsExactly("r1", "r2");
    assertThat(vm.getIngredientFilters()).containsExactly("flour");

    // S4: Add second ingredient filter (AND logic — only r1 matches both)
    Platform.runLater(() -> vm.addIngredientFilter("chocolate"));
    awaitSearch();
    waitForFxEvents();
    assertThat(vm.getResultIds()).containsExactly("r1");
    assertThat(vm.getIngredientFilters()).containsExactly("flour", "chocolate");

    // S5: Clear filters resets all state and returns all recipes
    Platform.runLater(vm::clearFilters);
    awaitSearch();
    waitForFxEvents();

    assertThat(vm.getQuery()).isEmpty();
    assertThat(vm.getIngredientFilters()).isEmpty();
    assertThat(vm.getResultIds()).containsExactly("r1", "r2", "r3");
    assertThat(vm.getStatusMessage()).isEqualTo("3 results");
  }

  /** S6: isSearching true during async search; false after completion. */
  @Test
  void searchingFlag_isTrueWhileBackgroundSearchRuns() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);
    Recipe cake = recipe("r1", "Cake");
    when(librarianService.listAllRecipes()).thenReturn(List.of(cake));
    when(librarianService.resolveRecipes("cake"))
        .thenAnswer(
            inv -> {
              try {
                Thread.sleep(200);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              return List.of(cake);
            });

    SearchViewModelImpl vm = newViewModel(librarianService, navigationService);
    awaitSearch();
    clearInvocations(librarianService);

    vm.setQuery("cake");
    Thread.sleep(40);
    waitForFxEvents();

    assertThat(vm.isSearching()).isTrue();

    Thread.sleep(250);
    waitForFxEvents();

    assertThat(vm.isSearching()).isFalse();
    assertThat(vm.getResultIds()).containsExactly("r1");
  }

  /** S8, S9: selectNext/selectPrevious cycle results; navigateToSelectedResult uses selection. */
  @Test
  void selectionCyclesAndNavigationUsesSelectedRecipeId() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);
    Recipe cake = recipe("r1", "Cake");
    Recipe cookies = recipe("r2", "Cookies");
    Recipe pie = recipe("r3", "Pie");
    when(librarianService.listAllRecipes()).thenReturn(List.of(cake, cookies, pie));

    SearchViewModelImpl vm = newViewModel(librarianService, navigationService);
    awaitSearch();
    clearInvocations(navigationService);

    // S8: Selection starts at first result
    assertThat(vm.getSelectedResultId()).isEqualTo("r1");

    // S8: Up/Down arrows cycle selection
    vm.selectNextResult();
    assertThat(vm.getSelectedResultId()).isEqualTo("r2");

    vm.selectNextResult();
    assertThat(vm.getSelectedResultId()).isEqualTo("r3");

    vm.selectPreviousResult();
    assertThat(vm.getSelectedResultId()).isEqualTo("r2");

    // S9: Enter key navigates to selected result
    vm.navigateToSelectedResult();
    verify(navigationService).navigateToRecipe("r2");
  }

  /** S9: Selected result ID persists across result refreshes. */
  @Test
  void selectedResultId_persistsWhenResultsRefresh() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);
    Recipe cake = recipe("r1", "Cake");
    Recipe cookies = recipe("r2", "Cookies");

    when(librarianService.listAllRecipes()).thenReturn(List.of(cake, cookies));

    SearchViewModelImpl vm = newViewModel(librarianService, navigationService);
    awaitSearch();

    vm.setSelectedResultId("r1");
    assertThat(vm.getSelectedResultId()).isEqualTo("r1");

    // Refresh search without changing results
    vm.setQuery("");
    awaitSearch();
    waitForFxEvents();

    assertThat(vm.getSelectedResultId()).isEqualTo("r1");
  }

  /** S9: navigateToSelectedResult passes selected ID to NavigationService. */
  @Test
  void navigateToSelectedResult_callsNavigationService() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);
    Recipe cake = recipe("r1", "Cake");

    when(librarianService.listAllRecipes()).thenReturn(List.of(cake));

    SearchViewModelImpl vm = newViewModel(librarianService, navigationService);
    awaitSearch();

    vm.setSelectedResultId("r1");
    vm.navigateToSelectedResult();

    verify(navigationService).navigateToRecipe("r1");
  }
}
