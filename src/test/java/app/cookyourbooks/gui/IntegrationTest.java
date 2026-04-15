package app.cookyourbooks.gui;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import javafx.util.Duration;

import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.viewmodel.ImportViewModelImpl;
import app.cookyourbooks.gui.viewmodel.LibraryViewModelImpl;
import app.cookyourbooks.gui.viewmodel.impl.SearchViewModelImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.FakeRecipeOcrService;

class IntegrationTest extends ViewModelTestBase {

  @Test
  void searchResult_navigateToRecipe_opensRecipeEditor() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);

    Recipe cake = RecipeFixtures.makeRecipe("r1", "Cake");
    when(librarianService.listAllRecipes()).thenReturn(List.of(cake));
    SearchViewModelImpl searchVM =
        new SearchViewModelImpl(librarianService, navigationService, Duration.millis(25));
    Thread.sleep(150);
    waitForFxEvents();
    searchVM.setSelectedResultId("r1");
    searchVM.navigateToSelectedResult();
    verify(navigationService).navigateToRecipe("r1");
  }

  @Test
  void importFlow_navigatesToRecipeEditor() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);

    ImportViewModelImpl importVM =
        new ImportViewModelImpl(new FakeRecipeOcrService(0), librarianService, navigationService);

    importVM.startImport(Path.of("test.jpg"));

    for (int i = 0; i < 160; i++) {
      waitForFxEvents();
      if ("review".equals(importVM.getState())) {
        // Set a collection and accept the import to trigger navigation
        importVM.selectTargetCollection("test-collection");
        importVM.acceptImport();
        // Wait for the navigation callback to complete
        waitForFxEvents();
        waitForFxEvents();
        Thread.sleep(100);
        verify(navigationService).navigateToEditorWithDraft(any());
        return;
      }
      Thread.sleep(25);
    }

    fail("Timed out waiting for state: review, actual: " + importVM.getState());
  }

  @Test
  void libraryView_selectRecipe_navigatesToRecipeEditor() throws Exception {
    LibrarianService librarianService = mock(LibrarianService.class);
    NavigationService navigationService = mock(NavigationService.class);

    // Use fast timeout (50ms) for testing instead of production 5 seconds
    LibraryViewModelImpl libraryVM =
        new LibraryViewModelImpl(
            librarianService, navigationService, javafx.util.Duration.millis(50));

    Recipe cake = RecipeFixtures.makeRecipe("r1", "Cake");
    RecipeCollection desserts = RecipeFixtures.makeCollection("c1", "Desserts", cake);
    when(librarianService.listCollections()).thenReturn(List.of(desserts));
    when(librarianService.listAllRecipes()).thenReturn(List.of(cake));
    when(librarianService.findCollectionById("c1")).thenReturn(java.util.Optional.of(desserts));

    libraryVM.refresh();
    for (int i = 0; i < 160; i++) {
      waitForFxEvents();
      if (!libraryVM.isLoading() && libraryVM.getCollectionIds().contains("c1")) {
        break;
      }
      Thread.sleep(25);
    }

    if (libraryVM.isLoading() || !libraryVM.getCollectionIds().contains("c1")) {
      fail("Timed out waiting for library refresh to complete");
    }

    libraryVM.selectCollection("c1");
    libraryVM.selectRecipe("r1");

    verify(navigationService).navigateToRecipe("r1");
  }
}
