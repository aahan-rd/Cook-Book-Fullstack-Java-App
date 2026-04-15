package app.cookyourbooks.gui.viewmodel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.gui.BackgroundTaskRunner;
import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.RecipeOcrService;

/**
 * Concrete implementation of {@link ImportViewModel}.
 *
 * <p>Coordinates OCR import flow state, exposes JavaFX-bindable properties, and persists accepted
 * imports through {@link LibrarianService}.
 */
public final class ImportViewModelImpl implements ImportViewModel {

  /** Lightweight collection entry used by the import collection selectors. */
  public record CollectionSummary(String id, String title) {}

  private enum ImportState {
    IDLE,
    PROCESSING,
    REVIEW,
    ERROR
  }

  private static final Pattern SERVINGS_PATTERN = Pattern.compile("(?i)servings?\\s*:\\s*(\\d+)");

  private final RecipeOcrService ocrService;
  private final LibrarianService librarianService;
  private final NavigationService navigationService;

  private final StringProperty statusMessage = new SimpleStringProperty("");
  private final StringProperty errorMessage = new SimpleStringProperty("");
  private final DoubleProperty importProgress = new SimpleDoubleProperty(-1.0);
  private final StringProperty stateProperty =
      new SimpleStringProperty(ImportState.IDLE.name().toLowerCase(Locale.ROOT));
  private final StringProperty importedTitle = new SimpleStringProperty("");
  private final StringProperty importedDescription = new SimpleStringProperty("");
  private final ObservableList<String> importedIngredients = FXCollections.observableArrayList();
  private final ObservableList<String> importedInstructions = FXCollections.observableArrayList();
  private final ObservableList<CollectionSummary> availableCollections =
      FXCollections.observableArrayList();

  private ImportState state = ImportState.IDLE;
  private @Nullable Task<?> currentTask;
  private @Nullable String selectedCollectionId;
  private List<Instruction> importedInstructionObjects = List.of();
  private boolean isCancelling;

  /**
   * Creates a new import ViewModel.
   *
   * @param ocrService OCR service used to extract recipes from images
   * @param librarianService service used for loading collections and saving recipes
   * @param navigationService app navigation service for routing with draft recipes
   */
  public ImportViewModelImpl(
      RecipeOcrService ocrService,
      LibrarianService librarianService,
      NavigationService navigationService) {
    this.ocrService = ocrService;
    this.librarianService = librarianService;
    this.navigationService = navigationService;
    importedInstructions.addListener(
        (ListChangeListener<String>) change -> syncInstructionObjects());
  }

  /** {@inheritDoc} */
  @Override
  public StringProperty statusMessageProperty() {
    return statusMessage;
  }

  /** {@inheritDoc} */
  @Override
  public StringProperty errorMessageProperty() {
    return errorMessage;
  }

  /** {@inheritDoc} */
  @Override
  public DoubleProperty importProgressProperty() {
    return importProgress;
  }

  /** {@inheritDoc} */
  @Override
  public StringProperty stateProperty() {
    return stateProperty;
  }

  /** {@inheritDoc} */
  @Override
  public StringProperty importedTitleProperty() {
    return importedTitle;
  }

  /** {@inheritDoc} */
  @Override
  public StringProperty importedDescriptionProperty() {
    return importedDescription;
  }

  /** {@inheritDoc} */
  @Override
  public ObservableList<String> importedIngredientsProperty() {
    return importedIngredients;
  }

  /** {@inheritDoc} */
  @Override
  public ObservableList<String> importedInstructionsProperty() {
    return importedInstructions;
  }

  /** {@inheritDoc} */
  @Override
  public ObservableList<?> availableCollectionsProperty() {
    return availableCollections;
  }

  /** {@inheritDoc} */
  @Override
  public void startImport(Path imagePath) {
    if (state != ImportState.IDLE) {
      return;
    }

    isCancelling = false;
    setState(ImportState.PROCESSING);
    statusMessage.set("Extracting recipe...");
    importProgress.set(-1.0);

    currentTask =
        BackgroundTaskRunner.run(
            () -> ocrService.extractRecipe(imagePath),
            recipe -> {
              importedTitle.set(recipe.getTitle());
              String servingsText =
                  recipe.getServings() == null ? "" : recipe.getServings().toString();
              importedDescription.set(
                  String.join(
                      System.lineSeparator(), "Servings: " + servingsText, "Prep: ", "Cook: "));
              importedIngredients.setAll(
                  recipe.getIngredients().stream()
                      .map(ingredient -> ingredient.getName())
                      .toList());
              importedInstructions.setAll(
                  recipe.getInstructions().stream().map(Instruction::getText).toList());
              setState(ImportState.REVIEW);
              statusMessage.set("Review the extracted recipe.");
              importProgress.set(1.0);
            },
            exception -> {
              if (isCancelling) {
                return;
              }
              errorMessage.set(exception.getMessage());
              setState(ImportState.ERROR);
              statusMessage.set("Import failed.");
              importProgress.set(0.0);
            });
  }

  /** {@inheritDoc} */
  @Override
  public void cancelImport() {
    if (state != ImportState.PROCESSING) {
      return;
    }

    isCancelling = true;
    if (currentTask != null) {
      currentTask.cancel();
    }
    currentTask = null;

    setState(ImportState.IDLE);
    isCancelling = false;
    statusMessage.set("Import cancelled.");
    importProgress.set(0.0);
    importedTitle.set("");
    importedDescription.set("");
    importedIngredients.clear();
    importedInstructions.clear();
    importedInstructionObjects = List.of();
    errorMessage.set("");
  }

  /** {@inheritDoc} */
  @Override
  public void acceptImport() {
    if (state != ImportState.REVIEW) {
      return;
    }

    String recipeTitle = importedTitle.get();
    if (selectedCollectionId == null || recipeTitle.isBlank()) {
      return;
    }

    @Nullable Servings servings = parseServingsFromDescription(importedDescription.get());

    Recipe recipe =
        new Recipe(
            recipeTitle,
            servings,
            importedIngredients.stream()
                .map(name -> (Ingredient) new VagueIngredient(name, null, null, null))
                .toList(),
            importedInstructionObjects,
            List.of());
    librarianService.saveRecipe(recipe, selectedCollectionId);
    navigationService.navigateToEditorWithDraft(recipe);

    importedTitle.set("");
    importedDescription.set("");
    importedIngredients.clear();
    importedInstructions.clear();
    importedInstructionObjects = List.of();
    errorMessage.set("");
    setState(ImportState.IDLE);
    statusMessage.set("Recipe imported successfully.");
  }

  /** {@inheritDoc} */
  @Override
  public void rejectImport() {
    if (state != ImportState.REVIEW) {
      return;
    }

    importedTitle.set("");
    importedDescription.set("");
    importedIngredients.clear();
    importedInstructions.clear();
    importedInstructionObjects = List.of();
    errorMessage.set("");
    setState(ImportState.IDLE);
    statusMessage.set("Ready.");
  }

  /** {@inheritDoc} */
  @Override
  public void selectTargetCollection(String collectionId) {
    selectedCollectionId = collectionId;
  }

  /** {@inheritDoc} */
  @Override
  public void loadCollections() {
    availableCollections.setAll(
        librarianService.listCollections().stream()
            .map(collection -> new CollectionSummary(collection.getId(), collection.getTitle()))
            .toList());
  }

  /** {@inheritDoc} */
  @Override
  public void resetToIdle() {
    if (currentTask != null) {
      currentTask.cancel();
    }
    currentTask = null;

    importedTitle.set("");
    importedDescription.set("");
    importedIngredients.clear();
    importedInstructions.clear();
    importedInstructionObjects = List.of();
    errorMessage.set("");
    importProgress.set(0.0);
    statusMessage.set("Ready");
    setState(ImportState.IDLE);
  }

  /** {@inheritDoc} */
  @Override
  public String getState() {
    return stateProperty.get();
  }

  /** {@inheritDoc} */
  @Override
  public String getStatusMessage() {
    return statusMessage.get();
  }

  /** {@inheritDoc} */
  @Override
  public @Nullable String getErrorMessage() {
    if (state == ImportState.ERROR) {
      return errorMessage.get();
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public @Nullable String getImportedRecipeTitle() {
    if (state == ImportState.REVIEW) {
      return importedTitle.get();
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getImportedIngredientNames() {
    if (state != ImportState.REVIEW) {
      return List.of();
    }
    return importedIngredients.stream().toList();
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getAvailableCollectionIds() {
    return availableCollections.stream().map(CollectionSummary::id).toList();
  }

  /** {@inheritDoc} */
  @Override
  public @Nullable String getSelectedCollectionId() {
    return selectedCollectionId;
  }

  private void syncInstructionObjects() {
    List<Instruction> instructions = new ArrayList<>();
    for (int index = 0; index < importedInstructions.size(); index++) {
      instructions.add(new Instruction(index + 1, importedInstructions.get(index), List.of()));
    }
    importedInstructionObjects = List.copyOf(instructions);
  }

  private static @Nullable Servings parseServingsFromDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    Matcher matcher = SERVINGS_PATTERN.matcher(description);
    if (!matcher.find()) {
      return null;
    }
    try {
      int amount = Integer.parseInt(matcher.group(1));
      return amount > 0 ? new Servings(amount) : null;
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private void setState(ImportState nextState) {
    state = nextState;
    stateProperty.set(nextState.name().toLowerCase(Locale.ROOT));
  }
}
