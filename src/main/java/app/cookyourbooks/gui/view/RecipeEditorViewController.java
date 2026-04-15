package app.cookyourbooks.gui.view;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import app.cookyourbooks.gui.editor.IngredientEditCell;
import app.cookyourbooks.gui.editor.RecipeEditorViewModelImpl;
import app.cookyourbooks.gui.editor.StepEditCell;
import app.cookyourbooks.gui.shared.EditableIngredient;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModel;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.services.LibrarianService;

/**
 * Controller for RecipeEditorView.fxml.
 *
 * <p>Manages two panels inside a StackPane:
 *
 * <ul>
 *   <li>{@code readPanel} — title, ingredient strings with quantities from A1 domain objects,
 *       numbered steps. Shown when {@code editingProperty()} is false.
 *   <li>{@code editPanel} — editable title field, reorderable/removable ingredient list with ↑ ↓ ×
 *       per row, editable/deletable step list with ✎ × per row, add fields for both. Shown when
 *       {@code editingProperty()} is true.
 * </ul>
 *
 * <p><b>Steps design:</b> Steps are not part of the graded ViewModel interface (E1–E10), so they
 * are managed locally in {@link #editableSteps}. When the user clicks Save, the controller calls
 * {@link RecipeEditorViewModelImpl#setInstructions} to push the updated steps into the ViewModel
 * before calling {@code save()}. On discard, the local list is simply cleared.
 *
 * <p><b>Threading:</b> All methods here run on the FX Application Thread. The ViewModel's save
 * callback also runs on the FX thread (via BackgroundTaskRunner), so property updates and list
 * refreshes are always safe.
 */
@SuppressWarnings("NullAway.Init")
public class RecipeEditorViewController {

  // ── Read panel controls ─────────────────────────────────────────────────
  @FXML private ScrollPane readPanel;
  @FXML private Label titleLabel;
  @FXML private Label statusLabel;
  @FXML private VBox readDescriptionBlock;
  @FXML private Label readServingsLabel;
  @FXML private Label readPrepTimeLabel;
  @FXML private Label readCookTimeLabel;
  @FXML private ListView<String> readIngredientList;
  @FXML private ListView<String> readStepsList;
  @FXML private Button editButton;
  @FXML private Label collectionLabel;

  // ── Edit panel controls ─────────────────────────────────────────────────
  @FXML private ScrollPane editPanel;
  @FXML private Label editStatusLabel;
  @FXML private TextField titleField;
  @FXML private Label titleErrorLabel;
  @FXML private TextField servingsField;
  @FXML private TextField prepTimeField;
  @FXML private TextField cookTimeField;
  @FXML private ListView<EditableIngredient> ingredientListView;
  @FXML private TextField newIngredientField;
  @FXML private Button addIngredientButton;
  @FXML private Button saveButton;
  @FXML private Button discardButton;
  @FXML private ListView<String> stepsListView;
  @FXML private TextField newStepField;
  @FXML private Button addStepButton;

  private final RecipeEditorViewModel viewModel;
  private final LibrarianService librarianService;

  /**
   * Mutable steps list for the current edit session.
   *
   * <p>Loaded from {@code impl.getInstructions()} when entering edit mode. Pushed to the ViewModel
   * via {@code setInstructions()} just before saving. Cleared on discard.
   */
  private final ObservableList<String> editableSteps = FXCollections.observableArrayList();

  /**
   * Constructs the controller with the given ViewModel and librarian service.
   *
   * @param viewModel the recipe editor view model
   * @param librarianService the librarian service used to look up collection membership
   */
  public RecipeEditorViewController(
      RecipeEditorViewModel viewModel, LibrarianService librarianService) {
    this.viewModel = viewModel;
    this.librarianService = librarianService;
  }

  /** Called by JavaFX after FXML injection to initialize all bindings. */
  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    bindPanelVisibility();
    bindReadPanel();
    bindEditPanel();
    lockListHeights();
  }

  private void lockListHeights() {
    readIngredientList.setMaxHeight(Region.USE_PREF_SIZE);
    readStepsList.setMaxHeight(Region.USE_PREF_SIZE);
    ingredientListView.setMaxHeight(Region.USE_PREF_SIZE);
    stepsListView.setMaxHeight(Region.USE_PREF_SIZE);
  }

  // ── Panel visibility ────────────────────────────────────────────────────

  /** Binds read and edit panel visibility to the ViewModel's editing state. */
  private void bindPanelVisibility() {
    readPanel.visibleProperty().bind(viewModel.editingProperty().not());
    readPanel.managedProperty().bind(viewModel.editingProperty().not());
    editPanel.visibleProperty().bind(viewModel.editingProperty());
    editPanel.managedProperty().bind(viewModel.editingProperty());
  }

  // ── Read panel ──────────────────────────────────────────────────────────

  /** Binds all read-mode UI controls to ViewModel properties and sets up refresh listeners. */
  private void bindReadPanel() {
    // Title and status
    titleLabel.textProperty().bind(viewModel.titleProperty());
    statusLabel.textProperty().bind(viewModel.statusMessageProperty());

    // Plain cells — no selection highlight for read-only lists
    readIngredientList.setCellFactory(lv -> plainCell());
    readStepsList.setCellFactory(lv -> plainCell());

    // Refresh ingredient list whenever the observable list changes
    viewModel
        .ingredientsProperty()
        .addListener((javafx.collections.ListChangeListener<Object>) c -> refreshReadIngredients());

    // Refresh both lists when a new recipe is loaded (title change signals
    // loadRecipe ran)
    viewModel
        .titleProperty()
        .addListener(
            (obs, old, val) -> {
              refreshReadIngredients();
              refreshReadSteps();
              refreshReadDescription();
              refreshCollectionLabel();
            });

    // Refresh read lists when exiting edit mode (covers both save and discard)
    // Use Platform.runLater so the ViewModel's save callback fully completes first
    viewModel
        .editingProperty()
        .addListener(
            (obs, wasEditing, nowEditing) -> {
              if (wasEditing && !nowEditing) {
                Platform.runLater(
                    () -> {
                      refreshReadIngredients();
                      refreshReadSteps();
                      refreshReadDescription();
                      refreshCollectionLabel();
                    });
              }
            });

    // Initial population
    refreshReadIngredients();
    refreshReadSteps();

    // Edit button — load steps then enter edit mode
    editButton.setOnAction(
        e -> {
          loadStepsForEditing();
          viewModel.toggleEditMode();
        });
    editButton.disableProperty().bind(viewModel.isSavingProperty());
  }

  private static final double LIST_ROW_HEIGHT = 28.0;
  private static final double LIST_BOTTOM_GAP = 22.0;

  /**
   * Rebuilds the read-only ingredient list.
   *
   * <p>Uses {@link EditableIngredient#getDisplayString()} which includes quantity and unit from A1
   * domain objects (e.g. "2 cups flour").
   */
  private void refreshReadIngredients() {
    @SuppressWarnings("unchecked")
    ObservableList<EditableIngredient> list =
        (ObservableList<EditableIngredient>) viewModel.ingredientsProperty();
    long itemCount = list.stream().filter(e -> !e.isBlank()).count();

    readIngredientList
        .getItems()
        .setAll(
            list.stream().filter(e -> !e.isBlank()).map(e -> "• " + e.getDisplayString()).toList());

    readIngredientList.setPrefHeight(
        Math.max(Region.USE_PREF_SIZE, itemCount * LIST_ROW_HEIGHT + LIST_BOTTOM_GAP));
  }

  /**
   * Rebuilds the read-only steps list from the ViewModel's current instructions.
   *
   * <p>After a successful save, {@code originalRecipe} in the impl is updated before {@code
   * editing} is set to false, so this method always sees the latest saved instructions.
   */
  private void refreshReadSteps() {
    if (viewModel instanceof RecipeEditorViewModelImpl impl) {
      List<Instruction> instructions = impl.getInstructions();
      if (instructions.isEmpty()) {
        readStepsList.getItems().setAll("No steps added yet.");
        readStepsList.setPrefHeight(LIST_ROW_HEIGHT + LIST_BOTTOM_GAP);
      } else {
        readStepsList
            .getItems()
            .setAll(
                instructions.stream().map(i -> i.getStepNumber() + ". " + i.getText()).toList());
        readStepsList.setPrefHeight(instructions.size() * LIST_ROW_HEIGHT + LIST_BOTTOM_GAP);
      }
    }
  }

  /**
   * Updates the description block in read mode from the local description fields. Hides the block
   * entirely if all three fields are empty.
   */
  private void refreshReadDescription() {
    String servings = servingsField.getText().strip();
    String prep = prepTimeField.getText().strip();
    String cook = cookTimeField.getText().strip();

    boolean hasAny = !servings.isEmpty() || !prep.isEmpty() || !cook.isEmpty();
    readDescriptionBlock.setVisible(hasAny);
    readDescriptionBlock.setManaged(hasAny);

    readServingsLabel.setText(servings.isEmpty() ? "" : "Serves: " + servings);
    readPrepTimeLabel.setText(prep.isEmpty() ? "" : "Prep: " + prep);
    readCookTimeLabel.setText(cook.isEmpty() ? "" : "Cook: " + cook);
  }

  /**
   * Updates the collection label in read mode by looking up which collection contains the current
   * recipe. Clears the label if no recipe is loaded or no matching collection is found.
   */
  private void refreshCollectionLabel() {
    String recipeId = viewModel.getRecipeId();
    if (recipeId == null) {
      collectionLabel.setText("");
      return;
    }
    librarianService.listCollections().stream()
        .filter(c -> c.containsRecipe(recipeId))
        .findFirst()
        .ifPresentOrElse(
            c -> collectionLabel.setText("Collection: " + c.getTitle()),
            () -> collectionLabel.setText(""));
  }

  // ── Edit panel ──────────────────────────────────────────────────────────

  /** Binds all edit-mode UI controls to ViewModel properties and wires button actions. */
  private void bindEditPanel() {
    // Title — bidirectional so typing updates the ViewModel
    titleField.textProperty().bindBidirectional(viewModel.titleProperty());
    titleField.disableProperty().bind(viewModel.isSavingProperty());

    // Show error message when title is empty
    titleErrorLabel.setVisible(false);
    titleErrorLabel.setManaged(false);
    viewModel
        .titleProperty()
        .addListener(
            (obs, old, val) -> {
              boolean blank = val == null || val.isBlank();
              titleErrorLabel.setVisible(blank);
              titleErrorLabel.setManaged(blank);
            });

    // Description fields — stored locally, mark dirty when changed
    servingsField.disableProperty().bind(viewModel.isSavingProperty());
    prepTimeField.disableProperty().bind(viewModel.isSavingProperty());
    cookTimeField.disableProperty().bind(viewModel.isSavingProperty());

    // Any change to description fields marks the recipe as dirty
    // so the Save button enables and changes persist on save
    servingsField
        .textProperty()
        .addListener((obs, old, val) -> viewModel.isDirtyProperty().set(true));
    prepTimeField
        .textProperty()
        .addListener((obs, old, val) -> viewModel.isDirtyProperty().set(true));
    cookTimeField
        .textProperty()
        .addListener((obs, old, val) -> viewModel.isDirtyProperty().set(true));

    // Edit status
    editStatusLabel.textProperty().bind(viewModel.statusMessageProperty());

    // Save button
    saveButton
        .disableProperty()
        .bind(
            viewModel
                .isDirtyProperty()
                .not()
                .or(viewModel.isValidProperty().not())
                .or(viewModel.isSavingProperty()));
    saveButton
        .textProperty()
        .bind(Bindings.when(viewModel.isSavingProperty()).then("Saving...").otherwise("Save"));
    saveButton.setOnAction(
        e -> {
          pushStepsToViewModel(); // must happen before save() builds the recipe
          viewModel.save();
        });

    // Cancel / Exit button
    discardButton.disableProperty().bind(viewModel.isSavingProperty());
    discardButton.setOnAction(
        e -> {
          editableSteps.clear(); // discard local step edits
          viewModel.discardChanges();
        });

    // Ingredient list — IngredientEditCell provides ↑ ↓ × per row
    @SuppressWarnings("unchecked")
    ObservableList<EditableIngredient> ingredientItems =
        (ObservableList<EditableIngredient>) viewModel.ingredientsProperty();
    ingredientListView.setItems(ingredientItems);
    ingredientListView.setCellFactory(lv -> new IngredientEditCell(viewModel));

    // Add ingredient via text field — Enter key or Add button
    addIngredientButton.disableProperty().bind(viewModel.isSavingProperty());
    addIngredientButton.setOnAction(e -> addIngredientFromField());
    newIngredientField.setOnAction(e -> addIngredientFromField());

    // Steps list — backed by local editableSteps, not the ViewModel
    stepsListView.setItems(editableSteps);
    stepsListView.setCellFactory(
        lv -> new StepEditCell(this::onStepEdit, this::onStepDelete, viewModel.isSavingProperty()));

    // Add step via text field — Enter key or Add button
    addStepButton.disableProperty().bind(viewModel.isSavingProperty());
    addStepButton.setOnAction(e -> addStepFromField());
    newStepField.setOnAction(e -> addStepFromField());
  }

  // ── Ingredient helpers ──────────────────────────────────────────────────

  /** Reads the new ingredient text field and adds a new ingredient to the ViewModel. */
  private void addIngredientFromField() {
    String text = newIngredientField.getText().strip();
    if (text.isBlank()) {
      return;
    }
    EditableIngredient ingredient = new EditableIngredient();
    ingredient.setName(text);
    @SuppressWarnings("unchecked")
    ObservableList<EditableIngredient> list =
        (ObservableList<EditableIngredient>) viewModel.ingredientsProperty();
    list.add(ingredient);
    newIngredientField.clear();
  }

  // ── Step helpers ────────────────────────────────────────────────────────

  /** Loads the current recipe's instructions into the local editable steps list. */
  private void loadStepsForEditing() {
    editableSteps.clear();
    if (viewModel instanceof RecipeEditorViewModelImpl impl) {
      impl.getInstructions().stream().map(Instruction::getText).forEach(editableSteps::add);
    }
  }

  /** Reads the new step text field, appends it to the steps list, and syncs to ViewModel. */
  private void addStepFromField() {
    String text = newStepField.getText().strip();
    if (text.isBlank()) {
      return;
    }
    editableSteps.add(text);
    newStepField.clear();
    pushStepsToViewModel(); // mark dirty immediately
  }

  /**
   * Handles an in-place edit of an existing step.
   *
   * @param index the index of the step to edit
   * @param newText the updated step text
   */
  private void onStepEdit(int index, String newText) {
    if (index >= 0 && index < editableSteps.size()) {
      editableSteps.set(index, newText);
      pushStepsToViewModel(); // mark dirty
    }
  }

  /**
   * Handles deletion of a step at the given index.
   *
   * @param index the index of the step to delete
   */
  private void onStepDelete(int index) {
    if (index >= 0 && index < editableSteps.size()) {
      editableSteps.remove(index);
      pushStepsToViewModel(); // mark dirty
    }
  }

  /** Pushes the current editable steps list to the ViewModel as an instruction list. */
  private void pushStepsToViewModel() {
    if (viewModel instanceof RecipeEditorViewModelImpl impl) {
      impl.setInstructions(buildInstructionList());
    }
  }

  /**
   * Builds an immutable instruction list from the current editable steps.
   *
   * @return list of {@link Instruction} objects with sequential step numbers
   */
  private List<Instruction> buildInstructionList() {
    List<Instruction> result = new ArrayList<>();
    for (int i = 0; i < editableSteps.size(); i++) {
      result.add(new Instruction(i + 1, editableSteps.get(i), List.of()));
    }
    return result;
  }

  // ── Utility ─────────────────────────────────────────────────────────────

  /**
   * Creates a plain read-only ListCell with no selection highlight box. Used for read-mode
   * ingredient and step display lists.
   */
  private static ListCell<String> plainCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : item);
        setStyle("-fx-background-color: transparent; -fx-padding: 3 0 3 4;");
      }
    };
  }
}
