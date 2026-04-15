package app.cookyourbooks.gui;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

import app.cookyourbooks.CybLibrary;
import app.cookyourbooks.adapters.GeminiOcrAdapter;
import app.cookyourbooks.adapters.gemini.RealGeminiClient;
import app.cookyourbooks.gui.editor.RecipeEditorViewModelImpl;
import app.cookyourbooks.gui.view.ImportViewController;
import app.cookyourbooks.gui.view.LibraryViewController;
import app.cookyourbooks.gui.view.MainViewController;
import app.cookyourbooks.gui.view.RecipeEditorViewController;
import app.cookyourbooks.gui.view.SearchViewController;
import app.cookyourbooks.gui.viewmodel.ImportViewModelImpl;
import app.cookyourbooks.gui.viewmodel.LibraryViewModelImpl;
import app.cookyourbooks.gui.viewmodel.impl.SearchViewModelImpl;
import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.LibrarianServiceImpl;

/**
 * Main JavaFX Application class for CookYourBooks.
 *
 * <p>Entry point that orchestrates the startup sequence: loads the recipe library, creates the
 * service layer and ViewModels, wires up all feature views (Library, Search, Import, Recipe
 * Editor), and displays the main application window. Demonstrates the recommended dependency
 * injection and wiring pattern for connecting ViewModels to the service layer and navigation.
 *
 * <h2>Startup sequence</h2>
 *
 * <ol>
 *   <li>Load the recipe library ({@link CybLibrary}) with repositories and conversion registry
 *   <li>Create service-layer objects ({@link LibrarianServiceImpl}, etc.)
 *   <li>Create the shared {@link NavigationService} for cross-feature routing
 *   <li>Create each feature's ViewModel, injecting dependencies via constructor
 *   <li>Load each feature's FXML and wire its ViewModel to the controller
 *   <li>Register all views with {@link MainViewController}
 *   <li>Load and display the main window
 * </ol>
 *
 * <h2>Adding a new feature</h2>
 *
 * <p>To add a new feature (e.g., Planner), follow this pattern:
 *
 * <ol>
 *   <li>Create a {@code PlannerViewModel} interface and {@code PlannerViewModelImpl} concrete class
 *   <li>Inject service dependencies into the ViewModel constructor
 *   <li>Create {@code PlannerViewController.java} to bind to the ViewModel
 *   <li>Create {@code PlannerView.fxml} for the UI layout
 *   <li>In this class's {@code start} method, create the ViewModel, extract a {@code
 *       wirePlannerView} helper, and register it with {@code mainController}
 * </ol>
 *
 * <h2>Threading</h2>
 *
 * <p>All FXML loading and UI operations occur on the JavaFX Application Thread. Async work (like
 * recipe OCR) is handled by the service layer and ViewModels using {@link BackgroundTaskRunner} and
 * {@link javafx.concurrent.Task}.
 */
public class CookYourBooksGuiApp extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(CookYourBooksGuiApp.class);

  @Override
  public void start(Stage primaryStage) {
    // ── 1. Load the recipe library ──
    CybLibrary library = CybLibrary.load(Path.of("cyb-library.json"));

    // ── 2. Create services ──
    var librarianService = createLibrarianService(library);
    LOG.info("Loaded {} collections", librarianService.listCollections().size());

    // ── 3. Create shared navigation ──
    var navigationService = new NavigationService();

    // ── 4. Create the main layout ──
    var mainController = new MainViewController(navigationService);

    // ── 5. Create feature ViewModels ──
    var libraryViewModel =
        new LibraryViewModelImpl(
            librarianService,
            navigationService,
            javafx.util.Duration.seconds(5)); // 5-second undo timeout for production
    LOG.info(
        "Library ViewModel ready with {} initial collections",
        libraryViewModel.getCollectionIds().size());

    var searchViewModel = new SearchViewModelImpl(librarianService, navigationService);
    var importViewModel = createImportViewModel(librarianService, navigationService);
    LOG.info(
        "Search ViewModel ready with {} initial results", searchViewModel.getResultIds().size());

    // ── 6. Wire all feature views ──
    Parent libraryView =
        loadView(
            "/fxml/LibraryView.fxml",
            clazz -> {
              if (clazz == LibraryViewController.class) {
                return new LibraryViewController(libraryViewModel, navigationService);
              }
              throw new IllegalArgumentException("Unknown controller: " + clazz);
            },
            "LibraryView.fxml");
    mainController.setViewNode(NavigationService.View.LIBRARY, libraryView);

    wireSearchView(mainController, searchViewModel);
    wireImportView(mainController, importViewModel);

    seedRecipeIfEmpty(library);

    var recipeEditorViewModel =
        wireRecipeEditor(library, mainController, librarianService, navigationService);

    // ── 7. Load and show main window ──
    showMainStage(primaryStage, mainController);
  }

  /**
   * Creates and initializes the {@link LibrarianServiceImpl}.
   *
   * <p>Extracts the recipe and collection repositories from the library and creates a librarian
   * service instance. The service provides high-level operations like searching recipes, managing
   * collections, and performing imports.
   *
   * @param library the loaded {@link CybLibrary} containing repositories and conversion registry
   * @return a new {@link LibrarianServiceImpl} instance
   */
  private LibrarianServiceImpl createLibrarianService(CybLibrary library) {
    return new LibrarianServiceImpl(
        library.getRecipeRepository(), library.getCollectionRepository(), library);
  }

  /**
   * Creates and initializes the {@link ImportViewModelImpl}.
   *
   * <p>Instantiates the import ViewModel with a {@link GeminiOcrAdapter} for recipe extraction,
   * resolving the Google API key from environment, system properties, or .env file. The ViewModel
   * manages the full import workflow: image → OCR → review → save.
   *
   * @param librarianService the librarian service for saving imported recipes
   * @param navigationService the navigation service for routing after import completion
   * @return a new {@link ImportViewModelImpl} instance
   */
  private ImportViewModelImpl createImportViewModel(
      LibrarianServiceImpl librarianService, NavigationService navigationService) {
    String googleApiKey = resolveGoogleApiKey();
    return new ImportViewModelImpl(
        new GeminiOcrAdapter(new RealGeminiClient(googleApiKey)),
        librarianService,
        navigationService);
  }

  /**
   * Wires the Search feature view to the main controller.
   *
   * <p>Loads {@code SearchView.fxml}, injects the provided SearchViewModel into the
   * SearchViewController, and registers the loaded view with the main controller under the {@link
   * NavigationService.View#SEARCH} identifier.
   *
   * @param mainController the main view controller to register this view with
   * @param searchViewModel the search ViewModel to inject into the controller
   */
  private void wireSearchView(
      MainViewController mainController, SearchViewModelImpl searchViewModel) {
    FXMLLoader searchLoader = new FXMLLoader(getClass().getResource("/fxml/SearchView.fxml"));
    searchLoader.setControllerFactory(
        clazz -> {
          if (clazz == SearchViewController.class) {
            return new SearchViewController(searchViewModel);
          }
          throw new IllegalArgumentException("Unknown controller: " + clazz);
        });
    Parent searchView;
    try {
      searchView = searchLoader.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load SearchView.fxml", e);
    }
    mainController.setViewNode(NavigationService.View.SEARCH, searchView);
  }

  /**
   * Wires the Import feature view to the main controller.
   *
   * <p>Loads {@code ImportView.fxml}, injects the provided ImportViewModel into the
   * ImportViewController, and registers the loaded view with the main controller under the {@link
   * NavigationService.View#IMPORT} identifier.
   *
   * @param mainController the main view controller to register this view with
   * @param importViewModel the import ViewModel to inject into the controller
   */
  private void wireImportView(
      MainViewController mainController, ImportViewModelImpl importViewModel) {
    FXMLLoader importLoader = new FXMLLoader(getClass().getResource("/fxml/ImportView.fxml"));
    importLoader.setControllerFactory(
        clazz -> {
          if (clazz == ImportViewController.class) {
            return new ImportViewController(importViewModel);
          }
          throw new IllegalArgumentException("Unknown controller: " + clazz);
        });
    Parent importView;
    try {
      importView = importLoader.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load ImportView.fxml", e);
    }
    mainController.setViewNode(NavigationService.View.IMPORT, importView);
  }

  /**
   * Loads the main layout FXML and displays the primary stage.
   *
   * @param primaryStage the primary stage
   * @param mainController the wired main layout controller
   */
  private void showMainStage(Stage primaryStage, MainViewController mainController) {
    URL mainViewUrl =
        Objects.requireNonNull(
            getClass().getResource("/fxml/MainView.fxml"), "MainView.fxml not found");
    FXMLLoader loader = new FXMLLoader(mainViewUrl);
    loader.setController(mainController);
    Parent root;
    try {
      root = loader.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load MainView.fxml", e);
    }
    Scene scene = new Scene(root, 960, 640);
    primaryStage.setTitle("CookYourBooks");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  /**
   * Seeds the recipe repository with a test recipe if empty.
   *
   * <p>Checks if the recipe repository is empty; if so, creates and saves a test "Pancakes" recipe
   * with sample ingredients and cooking steps. This ensures a non-empty initial state for UI
   * testing and demonstration purposes.
   *
   * @param library the loaded {@link CybLibrary} containing the recipe repository
   */
  private void seedRecipeIfEmpty(CybLibrary library) {
    if (library.getRecipeRepository().findAll().isEmpty()) {
      var testRecipe =
          new Recipe(
              null,
              "Test Pancakes",
              new Servings(4),
              List.of(
                  new MeasuredIngredient("flour", new ExactQuantity(2, Unit.CUP), null, null),
                  new MeasuredIngredient("milk", new ExactQuantity(1, Unit.CUP), null, null),
                  new VagueIngredient("salt", "to taste", null, null)),
              List.of(
                  new Instruction(1, "Mix dry ingredients", List.of()),
                  new Instruction(2, "Add wet ingredients", List.of()),
                  new Instruction(3, "Cook on griddle", List.of())),
              List.of());
      library.getRecipeRepository().save(testRecipe);
      LOG.info("Seeded test recipe: {}", testRecipe.getId());
    }
  }

  /**
   * Wires the Recipe Editor feature view and navigation listeners to the main controller.
   *
   * <p>Creates the RecipeEditorViewModel with a save callback, loads {@code RecipeEditorView.fxml},
   * injects the ViewModel into the RecipeEditorViewController, and sets up navigation listeners
   * for:
   *
   * <ul>
   *   <li>Initial recipe load when entering the editor without a selected recipe
   *   <li>Loading selected recipes when the user navigates to a recipe
   *   <li>Loading draft recipes from the import workflow
   * </ul>
   *
   * @param library the loaded {@link CybLibrary} containing the recipe repository
   * @param mainController the main view controller to register this view with
   * @param librarianService the librarian service for saving edited recipes
   * @param navigationService the navigation service for routing and state management
   * @return the created {@link RecipeEditorViewModelImpl} instance
   */
  private RecipeEditorViewModelImpl wireRecipeEditor(
      CybLibrary library,
      MainViewController mainController,
      LibrarianServiceImpl librarianService,
      NavigationService navigationService) {
    var recipeEditorViewModel =
        new RecipeEditorViewModelImpl(
            library.getRecipeRepository(),
            savedRecipe -> {
              String targetCollectionId = navigationService.getPendingTargetCollectionId();
              if (targetCollectionId == null || targetCollectionId.isBlank()) {
                return;
              }
              librarianService.saveRecipe(savedRecipe, targetCollectionId);
              navigationService.setPendingTargetCollectionId(null);
            });

    wireRecipeEditorNavigation(library, recipeEditorViewModel, navigationService);

    FXMLLoader editorLoader = new FXMLLoader(getClass().getResource("/fxml/RecipeEditorView.fxml"));
    editorLoader.setControllerFactory(
        clazz -> {
          if (clazz == RecipeEditorViewController.class) {
            return new RecipeEditorViewController(recipeEditorViewModel, librarianService);
          }
          throw new IllegalArgumentException("Unknown controller: " + clazz);
        });
    Parent editorView;
    try {
      editorView = editorLoader.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load RecipeEditorView.fxml", e);
    }
    mainController.setViewNode(NavigationService.View.RECIPE_EDITOR, editorView);
    LOG.info("Recipe Editor wired");

    return recipeEditorViewModel;
  }

  /**
   * Sets up navigation listeners for the Recipe Editor view.
   *
   * <p>Registers listeners on the NavigationService to handle three scenarios:
   *
   * <ol>
   *   <li>Entering edit mode without a selected recipe — loads the first recipe from the repository
   *   <li>Selecting a recipe by ID — loads the recipe into the editor
   *   <li>Loading a draft recipe — loads the draft (typically from the import workflow) and clears
   *       the draft property
   * </ol>
   *
   * @param library the loaded {@link CybLibrary} containing the recipe repository
   * @param recipeEditorViewModel the recipe editor ViewModel to load recipes into
   * @param navigationService the navigation service to listen to for routing events
   */
  private void wireRecipeEditorNavigation(
      CybLibrary library,
      RecipeEditorViewModelImpl recipeEditorViewModel,
      NavigationService navigationService) {
    navigationService
        .currentViewProperty()
        .addListener(
            (obs, oldView, newView) -> {
              if (newView == NavigationService.View.RECIPE_EDITOR
                  && recipeEditorViewModel.getRecipeId() == null) {
                var recipes = library.getRecipeRepository().findAll();
                if (!recipes.isEmpty()) {
                  recipeEditorViewModel.loadRecipe(recipes.get(0).getId());
                }
              }
            });

    navigationService
        .selectedRecipeIdProperty()
        .addListener(
            (obs, oldId, newId) -> {
              if (newId != null) {
                recipeEditorViewModel.loadRecipe(newId);
              }
            });

    navigationService
        .draftRecipeProperty()
        .addListener(
            (obs, oldDraft, draftRecipe) -> {
              if (draftRecipe != null) {
                recipeEditorViewModel.loadDraft(draftRecipe);
                navigationService.draftRecipeProperty().set(null);
              }
            });
  }

  /**
   * Loads a JavaFX view from an FXML resource file.
   *
   * <p>Loads the FXML file at the specified resource path, configures it with a controller factory
   * for dependency injection, and returns the loaded root node. If loading fails, throws a
   * RuntimeException with the view name for debugging.
   *
   * @param resourcePath the FXML resource path (e.g., {@code "/fxml/MyView.fxml"})
   * @param controllerFactory the factory function to create controllers on demand
   * @param viewName a friendly name for debugging error messages
   * @return the loaded root {@link Parent} node
   * @throws RuntimeException if FXML loading fails (wraps {@link IOException})
   */
  private Parent loadView(
      String resourcePath, Callback<Class<?>, Object> controllerFactory, String viewName) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
    loader.setControllerFactory(controllerFactory);
    try {
      return loader.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load " + viewName, e);
    }
  }

  /**
   * Resolves the Google API key for Gemini OCR service integration.
   *
   * <p>Attempts to resolve the key in the following order (first non-blank value wins):
   *
   * <ol>
   *   <li>Environment variables loaded from a {@code .env} file (via Dotenv)
   *   <li>System property {@code GOOGLE_API_KEY}
   *   <li>OS environment variable {@code GOOGLE_API_KEY}
   * </ol>
   *
   * <p>Returns null or empty string if the key cannot be resolved; callers should handle this
   * gracefully (e.g., fall back to FakeRecipeOcrService for testing).
   *
   * @return the resolved Google API key, or null/blank if not found
   */
  private static String resolveGoogleApiKey() {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    String googleApiKey = dotenv.get("GOOGLE_API_KEY");
    if (googleApiKey == null || googleApiKey.isBlank()) {
      googleApiKey = System.getProperty("GOOGLE_API_KEY");
    }
    if (googleApiKey == null || googleApiKey.isBlank()) {
      googleApiKey = System.getenv("GOOGLE_API_KEY");
    }
    return googleApiKey;
  }

  /**
   * Application entry point.
   *
   * @param args command-line arguments (unused)
   */
  public static void main(String[] args) {
    launch(args);
  }
}
