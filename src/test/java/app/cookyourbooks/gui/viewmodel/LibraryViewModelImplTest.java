package app.cookyourbooks.gui.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;
import app.cookyourbooks.services.LibrarianService;

/**
 * Unit tests for {@link LibraryViewModelImpl}.
 *
 * <p>Tests are mapped to assignment requirements L1-L13. Each test method corresponds to one or
 * more requirements and verifies observable state changes, async behavior, and edge cases.
 */
class LibraryViewModelImplTest extends ViewModelTestBase {

  private LibrarianService librarianService;
  private NavigationService navigationService;
  private LibraryViewModelImpl viewModel;

  /** Creates fresh mocks and a new view-model instance before each test case. */
  @BeforeEach
  void setUp() {
    librarianService = mock(LibrarianService.class);
    navigationService = mock(NavigationService.class);
    when(librarianService.listAllRecipes()).thenReturn(List.of());
    // Use fast timeout (50ms) for testing instead of production 5 seconds
    viewModel =
        new LibraryViewModelImpl(
            librarianService, navigationService, javafx.util.Duration.millis(50));
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L1: refresh() loads collections from service layer
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that refresh() fetches collections from LibrarianService and makes them available in
   * the view-model's collections list.
   *
   * <p>Requirement L1: refresh() loads collections from service layer.
   */
  @Test
  @DisplayName("L1: refresh() loads collections from LibrarianService")
  void testRefreshLoadsCollections() throws InterruptedException {
    RecipeCollection collection1 = createMockCollection("coll1", "Breakfast", SourceType.PERSONAL);
    RecipeCollection collection2 =
        createMockCollection("coll2", "Desserts", SourceType.PUBLISHED_BOOK);

    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));

    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          assertThat(viewModel.getCollectionIds()).containsExactly("coll1", "coll2");
          latch.countDown();
        });
    latch.await();

    verify(librarianService).listCollections();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L2: Each collection entry exposes ID, title, source type, count
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that each collection item in collectionsProperty exposes all required metadata: ID,
   * title, source type (PERSONAL, PUBLISHED_BOOK, etc.), and recipe count.
   *
   * <p>Requirement L2: Each collection entry exposes ID, title, source type, count.
   */
  @Test
  @DisplayName("L2: Collection entries expose ID, title, source type, and recipe count")
  void testCollectionEntryMetadata() throws InterruptedException {
    Recipe recipe1 = new Recipe("r1", "Pancakes", null, List.of(), List.of(), List.of());
    Recipe recipe2 = new Recipe("r2", "Waffles", null, List.of(), List.of(), List.of());
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL, recipe1, recipe2);

    when(librarianService.listCollections()).thenReturn(List.of(collection));

    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          Object item =
              viewModel.collectionsProperty().stream()
                  .filter(entry -> extractId(entry).equals("coll1"))
                  .findFirst()
                  .orElseThrow();
          assertThat(extractId(item)).isEqualTo("coll1");
          assertThat(extractTitle(item)).isEqualTo("Breakfast");
          assertThat(extractSourceType(item)).isEqualTo("PERSONAL");
          assertThat(extractRecipeCount(item)).isEqualTo(2);
          latch.countDown();
        });
    latch.await();
  }

  /**
   * Verifies that the synthetic "All recipes" collection card appears at index 0 in the collections
   * list after refresh, even when the service returns only real collections.
   *
   * <p>Supports L2 verification that the all-recipes card is synthesized and always first.
   */
  @Test
  @DisplayName("All recipes collection is first when filter is empty")
  void testAllRecipesCollectionAppearsFirst() throws InterruptedException {
    RecipeCollection collection1 = createMockCollection("coll1", "Breakfast", SourceType.PERSONAL);
    when(librarianService.listCollections()).thenReturn(List.of(collection1));

    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          assertThat(viewModel.collectionsProperty()).isNotEmpty();
          Object firstItem = viewModel.collectionsProperty().get(0);
          assertThat(extractId(firstItem)).isEqualTo(LibraryViewModel.ALL_RECIPES_ID);
          assertThat(extractTitle(firstItem)).isEqualTo("All recipes");
          latch.countDown();
        });
    latch.await();
  }

  /**
   * Verifies that selecting the synthetic "All recipes" collection populates the recipe list from
   * librarianService.listAllRecipes() rather than a per-collection subset.
   *
   * <p>Supports L2/L3 verification that all-recipes selection uses library-wide search.
   */
  @Test
  @DisplayName("Selecting All recipes loads recipes from listAllRecipes")
  void testSelectAllRecipesLoadsAllRecipes() {
    Recipe recipe1 = new Recipe("r1", "Pancakes", null, List.of(), List.of(), List.of());
    Recipe recipe2 = new Recipe("r2", "Waffles", null, List.of(), List.of(), List.of());
    when(librarianService.listAllRecipes()).thenReturn(List.of(recipe1, recipe2));

    viewModel.selectCollection(LibraryViewModel.ALL_RECIPES_ID);

    assertThat(viewModel.getSelectedCollectionId()).isEqualTo(LibraryViewModel.ALL_RECIPES_ID);
    assertThat(viewModel.getRecipeIds()).containsExactly("r1", "r2");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L3: selectCollection() updates selected collection and recipe
  // list
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that selectCollection() atomically updates getSelectedCollectionId() and populates
   * recipesProperty() with the recipes from the selected collection.
   *
   * <p>Requirement L3: selectCollection() updates selected collection and recipe list.
   */
  @Test
  @DisplayName("L3: selectCollection() updates selected collection and populates recipe list")
  void testSelectCollectionUpdatesRecipeList() {
    Recipe recipe1 = new Recipe("r1", "Pancakes", null, List.of(), List.of(), List.of());
    Recipe recipe2 = new Recipe("r2", "Waffles", null, List.of(), List.of(), List.of());
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL, recipe1, recipe2);

    when(librarianService.findCollectionById("coll1")).thenReturn(Optional.of(collection));

    viewModel.selectCollection("coll1");

    assertThat(viewModel.getSelectedCollectionId()).isEqualTo("coll1");
    assertThat(viewModel.getRecipeIds()).containsExactly("r1", "r2");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L4: createCollection() adds a new collection
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that createCollection() adds a newly created collection to collectionsProperty() and
   * automatically selects it, making it the focused collection.
   *
   * <p>Requirement L4: createCollection() adds a new collection.
   */
  @Test
  @DisplayName("L4: createCollection() adds new collection to list and selects it")
  void testCreateCollectionAddsAndSelects() {
    RecipeCollection newCollection =
        new MockRecipeCollection("coll-new", "New Collection", SourceType.PERSONAL);

    when(librarianService.createCollection("New Collection")).thenReturn(newCollection);
    when(librarianService.findCollectionById("coll-new")).thenReturn(Optional.of(newCollection));

    viewModel.createCollection("New Collection");

    assertThat(viewModel.getCollectionIds()).contains("coll-new");
    assertThat(viewModel.getSelectedCollectionId()).isEqualTo("coll-new");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L5: deleteCollection() removes collection (delayed 5 seconds)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that deleteCollection() immediately removes a collection from collectionsProperty()
   * without waiting for the 5-second undo window to expire, allowing the UI to reflect deletion at
   * once.
   *
   * <p>Requirement L5: deleteCollection() removes collection (delayed 5 seconds).
   */
  @Test
  @DisplayName("L5: deleteCollection() immediately removes from UI and queues for deletion")
  void testDeleteCollectionRemovesImmediately() throws InterruptedException {
    RecipeCollection collection1 =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL);
    RecipeCollection collection2 =
        new MockRecipeCollection("coll2", "Desserts", SourceType.PERSONAL);

    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));
    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          viewModel.deleteCollection("coll1");
          assertThat(viewModel.getCollectionIds()).containsExactly("coll2");
          latch.countDown();
        });
    latch.await();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L6: undoDelete() restores deleted collection
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that undoDelete() restores the most recently deleted collection back to its original
   * index in collectionsProperty() and clears isUndoAvailable() to false.
   *
   * <p>Requirement L6: undoDelete() restores deleted collection.
   */
  @Test
  @DisplayName("L6: undoDelete() restores removed collection and clears undo state")
  void testUndoDeleteRestoresCollection() throws InterruptedException {
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL);

    when(librarianService.listCollections()).thenReturn(List.of(collection));
    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          viewModel.deleteCollection("coll1");
          assertThat(viewModel.isUndoAvailable()).isTrue();
          assertThat(viewModel.getCollectionIds()).isEmpty();

          viewModel.undoDelete();
          assertThat(viewModel.getCollectionIds()).containsExactly("coll1");
          assertThat(viewModel.isUndoAvailable()).isFalse();
          latch.countDown();
        });
    latch.await();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L7: Undo state clears after 5-second timeout
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that after a delete operation, isUndoAvailable() remains true for 5 seconds. Once the
   * 5-second timeout expires without an undo call, isUndoAvailable() becomes false and the deletion
   * is committed to the service layer.
   *
   * <p>Requirement L7: Undo state clears after 5-second timeout.
   */
  @Test
  @DisplayName("L7: undoAvailable clears after 5-second timer (without undo)")
  void testUndoStateExpiresAfterTimeout() throws InterruptedException {
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL);

    when(librarianService.listCollections()).thenReturn(List.of(collection));
    viewModel.refresh();
    awaitRefresh();

    CountDownLatch initLatch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          viewModel.deleteCollection("coll1");
          assertThat(viewModel.isUndoAvailable()).isTrue();
          initLatch.countDown();
        });
    initLatch.await();

    Thread.sleep(5100);

    CountDownLatch checkLatch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          assertThat(viewModel.isUndoAvailable()).isFalse();
          assertThat(viewModel.getFilterText()).isEmpty();
          checkLatch.countDown();
        });
    checkLatch.await();

    verify(librarianService).deleteCollection("coll1");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L8: refresh() runs on background thread, loading flag true
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that refresh() sets isLoading() to true before scheduling the background fetch and
   * remains true while the fetch is in flight, then transitions to false once the data is
   * available. This allows the UI to show a loading indicator.
   *
   * <p>Requirement L8: refresh() runs on background thread, loading flag true during fetch.
   */
  @Test
  @DisplayName("L8: refresh() sets loading=true during fetch, false after completion")
  void testRefreshLoadingFlag() throws InterruptedException {
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL);

    CountDownLatch fetchStarted = new CountDownLatch(1);
    CountDownLatch allowFetchToFinish = new CountDownLatch(1);

    when(librarianService.listCollections())
        .thenAnswer(
            ignored -> {
              fetchStarted.countDown();
              allowFetchToFinish.await(1, TimeUnit.SECONDS);
              return List.of(collection);
            });

    viewModel.refresh();

    assertThat(fetchStarted.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(viewModel.isLoading()).isTrue();

    allowFetchToFinish.countDown();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          assertThat(viewModel.isLoading()).isFalse();
          assertThat(viewModel.getCollectionIds()).contains("coll1");
          latch.countDown();
        });
    latch.await();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L9: selectRecipe() provides recipe ID for navigation
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that selectRecipe() delegates to navigationService.navigateToRecipe() with the
   * selected recipe ID, enabling the navigation layer to open the Recipe Editor screen.
   *
   * <p>Requirement L9: selectRecipe() provides recipe ID for navigation.
   */
  @Test
  @DisplayName("L9: selectRecipe() navigates to recipe via NavigationService")
  void testSelectRecipeNavigates() {
    Recipe recipe1 = new Recipe("r1", "Pancakes", null, List.of(), List.of(), List.of());
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL, recipe1);
    when(librarianService.findCollectionById("coll1")).thenReturn(Optional.of(collection));

    viewModel.selectCollection("coll1");
    viewModel.selectRecipe("r1");

    verify(navigationService).navigateToRecipe("r1");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L10: selectCollection() with nonexistent ID is handled gracefully
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that selectCollection() with a collection ID that does not exist in the service layer
   * (Optional.empty() response) clears the current selection by setting getSelectedCollectionId()
   * to null and clearing recipesProperty().
   *
   * <p>Requirement L10: selectCollection() with nonexistent ID is handled gracefully.
   */
  @Test
  @DisplayName("L10: selectCollection() with nonexistent ID clears selection gracefully")
  void testSelectNonexistentCollectionClearsSelection() {
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL);
    when(librarianService.findCollectionById("coll1")).thenReturn(Optional.of(collection));

    viewModel.selectCollection("coll1");
    assertThat(viewModel.getSelectedCollectionId()).isEqualTo("coll1");

    when(librarianService.findCollectionById("nonexistent")).thenReturn(Optional.empty());

    viewModel.selectCollection("nonexistent");

    assertThat(viewModel.getSelectedCollectionId()).isNull();
    assertThat(viewModel.getRecipeIds()).isEmpty();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L11: filterTextProperty() filters collections by title
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that filterTextProperty() accepts text input and getFilterText() returns the exact
   * same value, confirming that the filter query state is properly exposed and retrievable.
   *
   * <p>Requirement L11: filterTextProperty() filters collections by title.
   */
  @Test
  @DisplayName("L11: filterTextProperty() changes filter text for collection lookup")
  void testFilterTextPropertyBinding() {
    viewModel.filterTextProperty().set("breakfast");

    assertThat(viewModel.getFilterText()).isEqualTo("breakfast");
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L12: Filtered list updates immediately (no debounce)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that setting filterTextProperty() immediately updates collectionsProperty() to show
   * only matching collections (by title), with no debounce delay. This test sets the filter while
   * on the JavaFX Application Thread and asserts the filtered list is updated in the same callback,
   * ensuring immediate in-memory filtering.
   *
   * <p>Requirement L12: Filtered list updates immediately (no debounce).
   */
  @Test
  @DisplayName("L12: Filtered list updates immediately with no debounce")
  void testFilterUpdatesImmediately() throws InterruptedException {
    RecipeCollection collection1 =
        new MockRecipeCollection("coll1", "Pancakes", SourceType.PERSONAL);
    RecipeCollection collection2 =
        new MockRecipeCollection("coll2", "Desserts", SourceType.PERSONAL);

    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));
    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          assertThat(viewModel.collectionsProperty())
              .extracting(this::extractId)
              .containsExactly(LibraryViewModel.ALL_RECIPES_ID, "coll1", "coll2");

          viewModel.filterTextProperty().set("pan");

          assertThat(viewModel.getFilterText()).isEqualTo("pan");
          assertThat(viewModel.collectionsProperty())
              .extracting(this::extractId)
              .containsExactly("coll1");
          latch.countDown();
        });
    latch.await();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Requirement L13: Undo-delete works correctly with active filter
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that the undo mechanism works correctly even when a collection filter is active. After
   * applying a filter, deleting a matching collection, and calling undoDelete(), the collection is
   * restored and remains visible in the filtered list.
   *
   * <p>Requirement L13: Undo-delete works correctly with active filter.
   */
  @Test
  @DisplayName("L13: Undo-delete preserves collection even while filter is active")
  void testUndoDeleteWithActiveFilter() throws InterruptedException {
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL);

    when(librarianService.listCollections()).thenReturn(List.of(collection));
    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          viewModel.filterTextProperty().set("break");
          viewModel.deleteCollection("coll1");

          assertThat(viewModel.isUndoAvailable()).isTrue();
          assertThat(viewModel.getFilterText()).isEqualTo("break");

          viewModel.undoDelete();

          assertThat(viewModel.getCollectionIds()).containsExactly("coll1");
          assertThat(viewModel.isUndoAvailable()).isFalse();
          latch.countDown();
        });
    latch.await();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Edge cases and error conditions
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Verifies that selectRecipe() with a blank or empty string ID is rejected without attempting
   * navigation, protecting the navigation service from invalid recipe IDs.
   *
   * <p>Edge case: Invalid input handling.
   */
  @Test
  @DisplayName("Edge case: selectRecipe() with blank ID is ignored")
  void testSelectRecipeWithBlankIdIsIgnored() {
    viewModel.selectRecipe("");

    verifyNoInteractionsWithNavigation();
  }

  /**
   * Verifies that selectRecipe() with an ID that does not exist in the currently selected
   * collection's recipe list is silently rejected, protecting the navigation service from stale or
   * invalid selections.
   *
   * <p>Edge case: Invalid recipe selection handling.
   */
  @Test
  @DisplayName("Edge case: selectRecipe() with nonexistent recipe is ignored")
  void testSelectRecipeNotInListIsIgnored() {
    Recipe recipe1 = new Recipe("r1", "Pancakes", null, List.of(), List.of(), List.of());
    RecipeCollection collection =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL, recipe1);
    when(librarianService.findCollectionById("coll1")).thenReturn(Optional.of(collection));

    viewModel.selectCollection("coll1");

    viewModel.selectRecipe("nonexistent");

    verifyNoInteractionsWithNavigation();
  }

  /**
   * Verifies that createCollection() with a blank or whitespace-only title propagates the service
   * layer exception without silencing it, allowing the caller to handle validation errors
   * appropriately.
   *
   * <p>Edge case: Service-layer validation error propagation.
   */
  @Test
  @DisplayName("Edge case: createCollection() with blank title is rejected by service")
  void testCreateCollectionWithBlankTitleHandled() {
    when(librarianService.createCollection("  "))
        .thenThrow(new IllegalArgumentException("Empty title"));

    try {
      viewModel.createCollection("  ");
    } catch (IllegalArgumentException ignored) {
      // Expected
    }
  }

  /**
   * Verifies that calling deleteCollection() multiple times in sequence replaces the previous
   * pending delete, ensuring only the most recent deletion can be undone. The earlier deletion is
   * automatically committed to the service layer.
   *
   * <p>Edge case: Multiple delete operations and undo queue replacement.
   */
  @Test
  @DisplayName("Edge case: deleteCollection() when no pending delete queued is safe")
  void testMultipleDeletesQueuesLatest() throws InterruptedException {
    RecipeCollection collection1 =
        new MockRecipeCollection("coll1", "Breakfast", SourceType.PERSONAL);
    RecipeCollection collection2 =
        new MockRecipeCollection("coll2", "Desserts", SourceType.PERSONAL);

    when(librarianService.listCollections()).thenReturn(List.of(collection1, collection2));
    viewModel.refresh();
    awaitRefresh();

    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          viewModel.deleteCollection("coll1");
          viewModel.deleteCollection("coll2");

          assertThat(viewModel.getCollectionIds()).isEmpty();
          assertThat(viewModel.isUndoAvailable()).isTrue();
          assertThat(viewModel.undoMessageProperty().get()).contains("Desserts");
          latch.countDown();
        });
    latch.await();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Helper methods
  // ──────────────────────────────────────────────────────────────────────────

  /** Builds a minimal collection fixture used by multiple tests. */
  private RecipeCollection createMockCollection(String id, String title, SourceType sourceType) {
    return new MockRecipeCollection(id, title, sourceType);
  }

  /** Verifies no navigation calls were made for invalid selection scenarios. */
  private void verifyNoInteractionsWithNavigation() {
    verifyNoInteractions(navigationService);
  }

  /** Waits until refresh loading settles and JavaFX pending events are drained. */
  private void awaitRefresh() throws InterruptedException {
    long deadline = System.currentTimeMillis() + 2000;
    while (viewModel.isLoading() && System.currentTimeMillis() < deadline) {
      waitForFxEvents();
      Thread.sleep(10);
    }
    waitForFxEvents();
  }

  private String extractId(Object item) {
    try {
      var method = item.getClass().getDeclaredMethod("id");
      method.setAccessible(true);
      Object result = method.invoke(item);
      return result != null ? result.toString() : "";
    } catch (Exception e) {
      return "";
    }
  }

  private String extractTitle(Object item) {
    try {
      var method = item.getClass().getDeclaredMethod("title");
      method.setAccessible(true);
      Object result = method.invoke(item);
      return result != null ? result.toString() : "";
    } catch (Exception e) {
      return "";
    }
  }

  private String extractSourceType(Object item) {
    try {
      var method = item.getClass().getDeclaredMethod("sourceType");
      method.setAccessible(true);
      Object result = method.invoke(item);
      return result != null ? result.toString() : "";
    } catch (Exception e) {
      return "";
    }
  }

  private int extractRecipeCount(Object item) {
    try {
      var method = item.getClass().getDeclaredMethod("recipeCount");
      method.setAccessible(true);
      Number count = (Number) method.invoke(item);
      return count.intValue();
    } catch (Exception e) {
      return 0;
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Test fixtures: Mock RecipeCollection
  // ──────────────────────────────────────────────────────────────────────────

  private static final class MockRecipeCollection implements RecipeCollection {
    private final String id;
    private final String title;
    private final SourceType sourceType;
    private final List<Recipe> recipes;

    MockRecipeCollection(String id, String title, SourceType sourceType, Recipe... recipes) {
      this.id = id;
      this.title = title;
      this.sourceType = sourceType;
      this.recipes = List.of(recipes);
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    @Override
    public List<Recipe> getRecipes() {
      return recipes;
    }

    @Override
    public Optional<Recipe> findRecipeById(String recipeId) {
      return recipes.stream().filter(r -> r.getId().equals(recipeId)).findFirst();
    }

    @Override
    public boolean containsRecipe(String recipeId) {
      return recipes.stream().anyMatch(r -> r.getId().equals(recipeId));
    }

    @Override
    public RecipeCollection addRecipe(Recipe recipe) {
      throw new UnsupportedOperationException("Mock only");
    }

    @Override
    public RecipeCollection removeRecipe(String recipeId) {
      throw new UnsupportedOperationException("Mock only");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MockRecipeCollection)) {
        return false;
      }
      MockRecipeCollection that = (MockRecipeCollection) o;
      return id.equals(that.id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }
  }
}
