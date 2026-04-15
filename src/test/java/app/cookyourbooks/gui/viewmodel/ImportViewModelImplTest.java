package app.cookyourbooks.gui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.FakeRecipeOcrService;
import app.cookyourbooks.services.ocr.OcrException;
import app.cookyourbooks.services.ocr.RecipeOcrService;

class ImportViewModelImplTest extends ViewModelTestBase {

  private LibrarianService librarianService;
  private NavigationService navigationService;
  private ImportViewModelImpl viewModel;

  @BeforeEach
  void setUp() {
    librarianService = mock(LibrarianService.class);
    navigationService = mock(NavigationService.class);
    viewModel =
        new ImportViewModelImpl(new FakeRecipeOcrService(0), librarianService, navigationService);
  }

  @Test
  void initialState_isIdle() {
    assertEquals("idle", viewModel.getState());
    assertNull(viewModel.getErrorMessage());
    assertNull(viewModel.getImportedRecipeTitle());
  }

  @Test
  void startImport_transitionsToProcessing() {
    ImportViewModelImpl vm =
        new ImportViewModelImpl(new FakeRecipeOcrService(500), librarianService, navigationService);

    vm.startImport(Path.of("test.jpg"));

    assertEquals("processing", vm.getState());
    assertEquals("Extracting recipe...", vm.getStatusMessage());
  }

  @Test
  void successfulOcr_transitionsToReview() throws InterruptedException {
    viewModel.startImport(Path.of("test.jpg"));
    waitUntilState(viewModel, "review");

    assertEquals("review", viewModel.getState());
    assertNotNull(viewModel.getImportedRecipeTitle());
  }

  @Test
  void ocrFailure_transitionsToError() throws InterruptedException {
    RecipeOcrService failingOcr =
        new FakeRecipeOcrService(0) {
          @Override
          public Recipe extractRecipe(Path imagePath) throws OcrException {
            throw new OcrException("OCR failed");
          }
        };
    ImportViewModelImpl vm =
        new ImportViewModelImpl(failingOcr, librarianService, navigationService);

    vm.startImport(Path.of("broken.jpg"));
    waitUntilState(vm, "error");

    assertEquals("error", vm.getState());
    assertNotNull(vm.getErrorMessage());
  }

  @Test
  void cancelImport_duringProcessing_transitionsToIdle() {
    ImportViewModelImpl vm =
        new ImportViewModelImpl(
            new FakeRecipeOcrService(5000), librarianService, navigationService);

    vm.startImport(Path.of("test.jpg"));
    vm.cancelImport();

    assertEquals("idle", vm.getState());
  }

  @Test
  void acceptImport_savesRecipeAndTransitionsToIdle() throws InterruptedException {
    viewModel.startImport(Path.of("test.jpg"));
    waitUntilState(viewModel, "review");
    viewModel.selectTargetCollection("collection-1");

    viewModel.acceptImport();

    verify(librarianService).saveRecipe(any(Recipe.class), eq("collection-1"));
    verify(navigationService).navigateToEditorWithDraft(any(Recipe.class));
    assertEquals("idle", viewModel.getState());
  }

  @Test
  void rejectImport_discardsAndTransitionsToIdle() throws InterruptedException {
    viewModel.startImport(Path.of("test.jpg"));
    waitUntilState(viewModel, "review");

    viewModel.rejectImport();

    assertEquals("idle", viewModel.getState());
    assertNull(viewModel.getImportedRecipeTitle());
  }

  @Test
  void loadCollections_populatesAvailableCollections() {
    RecipeCollection c1 = mock(RecipeCollection.class);
    RecipeCollection c2 = mock(RecipeCollection.class);
    when(c1.getId()).thenReturn("c1");
    when(c1.getTitle()).thenReturn("One");
    when(c2.getId()).thenReturn("c2");
    when(c2.getTitle()).thenReturn("Two");
    when(librarianService.listCollections()).thenReturn(List.of(c1, c2));

    viewModel.loadCollections();

    assertEquals(List.of("c1", "c2"), viewModel.getAvailableCollectionIds());
  }

  @Test
  void preSaveEditing_titleAndIngredientsAreMutable() throws InterruptedException {
    viewModel.startImport(Path.of("test.jpg"));
    waitUntilState(viewModel, "review");

    viewModel.importedTitleProperty().set("Edited Title");
    @SuppressWarnings("unchecked")
    var ingredients = (List<Object>) (List<?>) viewModel.importedIngredientsProperty();
    ingredients.clear();
    ingredients.add("salt");

    assertEquals("Edited Title", viewModel.getImportedRecipeTitle());
    assertEquals(List.of("salt"), viewModel.getImportedIngredientNames());
  }

  @Test
  void acceptImport_withNoCollection_isNoOp() throws InterruptedException {
    viewModel.startImport(Path.of("test.jpg"));
    waitUntilState(viewModel, "review");

    viewModel.acceptImport();

    assertEquals("review", viewModel.getState());
  }

  private static void waitUntilState(ImportViewModelImpl vm, String expectedState)
      throws InterruptedException {
    for (int i = 0; i < 160; i++) {
      waitForFxEvents();
      waitForFxEvents();
      waitForFxEvents();
      if (expectedState.equals(vm.getState())) {
        return;
      }
      Thread.sleep(25);
    }
    fail("Timed out waiting for state: " + expectedState + ", actual: " + vm.getState());
  }
}
