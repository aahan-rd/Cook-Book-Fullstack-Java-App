package app.cookyourbooks.gui.view;

import java.lang.reflect.Method;
import java.nio.file.Path;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import app.cookyourbooks.gui.viewmodel.ImportViewModelImpl;

/**
 * JavaFX controller for the import view.
 *
 * <p>Binds UI controls to {@link ImportViewModelImpl}, handles file selection, and keeps pane
 * visibility in sync with the import state machine.
 */
@SuppressWarnings("NullAway.Init")
public final class ImportViewController {

  @FXML private VBox idlePane;
  @FXML private Label idleStatusLabel;
  @FXML private Button chooseFileButton;
  @FXML private ComboBox<Object> idleCollectionCombo;

  @FXML private VBox processingPane;
  @FXML private ProgressBar progressBar;
  @FXML private Label processingStatusLabel;
  @FXML private Button cancelButton;

  @FXML private VBox reviewPane;
  @FXML private TextField titleField;
  @FXML private TextArea descriptionField;
  @FXML private ListView<String> ingredientsList;
  @FXML private ListView<String> stepsList;
  @FXML private ComboBox<Object> reviewCollectionCombo;
  @FXML private Button acceptButton;
  @FXML private Button rejectButton;

  @FXML private VBox errorPane;
  @FXML private Label errorMessageLabel;
  @FXML private Button resetButton;

  private final ImportViewModelImpl viewModel;

  /**
   * Creates a controller instance with injected dependencies.
   *
   * @param viewModel import view model used for state and commands
   * @param navigationService navigation service used to reset state on re-entry
   */
  public ImportViewController(ImportViewModelImpl viewModel) {
    this.viewModel = viewModel;
  }

  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    viewModel.loadCollections();

    idleStatusLabel.textProperty().bind(viewModel.statusMessageProperty());
    processingStatusLabel.textProperty().bind(viewModel.statusMessageProperty());
    errorMessageLabel.textProperty().bind(viewModel.errorMessageProperty());
    progressBar.progressProperty().bind(viewModel.importProgressProperty());
    titleField.textProperty().bindBidirectional(viewModel.importedTitleProperty());
    descriptionField.textProperty().bindBidirectional(viewModel.importedDescriptionProperty());

    ingredientsList.setItems(castList(viewModel.importedIngredientsProperty()));
    stepsList.setItems(castList(viewModel.importedInstructionsProperty()));
    ingredientsList.setEditable(true);
    stepsList.setEditable(true);
    ingredientsList.setCellFactory(TextFieldListCell.forListView());
    stepsList.setCellFactory(TextFieldListCell.forListView());
    ingredientsList.setOnEditCommit(
        event ->
            viewModel.importedIngredientsProperty().set(event.getIndex(), event.getNewValue()));
    stepsList.setOnEditCommit(
        event ->
            viewModel.importedInstructionsProperty().set(event.getIndex(), event.getNewValue()));
    idleCollectionCombo.setItems(castList(viewModel.availableCollectionsProperty()));
    reviewCollectionCombo.setItems(castList(viewModel.availableCollectionsProperty()));

    idleCollectionCombo.setCellFactory(
        list ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : titleOf(item));
              }
            });
    idleCollectionCombo.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : titleOf(item));
          }
        });

    reviewCollectionCombo.setCellFactory(
        list ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : titleOf(item));
              }
            });
    reviewCollectionCombo.setButtonCell(
        new javafx.scene.control.ListCell<>() {
          @Override
          protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : titleOf(item));
          }
        });

    idleCollectionCombo
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue != null) {
                String id = idOf(newValue);
                if (!id.isBlank()) {
                  viewModel.selectTargetCollection(id);
                  reviewCollectionCombo.getSelectionModel().select(newValue);
                }
              }
            });

    reviewCollectionCombo
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue != null) {
                String id = idOf(newValue);
                if (!id.isBlank()) {
                  viewModel.selectTargetCollection(id);
                  idleCollectionCombo.getSelectionModel().select(newValue);
                }
              }
            });

    idleCollectionCombo
        .valueProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue != null) {
                String id = idOf(newValue);
                if (!id.isBlank()) {
                  viewModel.selectTargetCollection(id);
                }
              }
            });

    reviewCollectionCombo
        .valueProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue != null) {
                String id = idOf(newValue);
                if (!id.isBlank()) {
                  viewModel.selectTargetCollection(id);
                }
              }
            });

    chooseFileButton.setOnAction(
        e -> {
          FileChooser chooser = new FileChooser();
          chooser.setTitle("Select recipe image");
          chooser
              .getExtensionFilters()
              .add(
                  new FileChooser.ExtensionFilter(
                      "Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));

          Window window =
              chooseFileButton.getScene() == null ? null : chooseFileButton.getScene().getWindow();
          java.io.File selected = chooser.showOpenDialog(window);
          if (selected != null) {
            viewModel.startImport(Path.of(selected.toURI()));
          }
        });

    cancelButton.setOnAction(e -> viewModel.cancelImport());
    acceptButton.setOnAction(e -> viewModel.acceptImport());
    rejectButton.setOnAction(e -> viewModel.rejectImport());
    resetButton.setOnAction(e -> viewModel.resetToIdle());

    BooleanBinding isIdle =
        Bindings.createBooleanBinding(
            () -> viewModel.stateProperty().get().equals("idle"), viewModel.stateProperty());
    BooleanBinding isProcessing =
        Bindings.createBooleanBinding(
            () -> viewModel.stateProperty().get().equals("processing"), viewModel.stateProperty());
    BooleanBinding isReview =
        Bindings.createBooleanBinding(
            () -> viewModel.stateProperty().get().equals("review"), viewModel.stateProperty());
    BooleanBinding isError =
        Bindings.createBooleanBinding(
            () -> viewModel.stateProperty().get().equals("error"), viewModel.stateProperty());

    idlePane.visibleProperty().bind(isIdle);
    idlePane.managedProperty().bind(isIdle);

    processingPane.visibleProperty().bind(isProcessing);
    processingPane.managedProperty().bind(isProcessing);

    reviewPane.visibleProperty().bind(isReview);
    reviewPane.managedProperty().bind(isReview);

    errorPane.visibleProperty().bind(isError);
    errorPane.managedProperty().bind(isError);
  }

  @SuppressWarnings("unchecked")
  private static <T> ObservableList<T> castList(ObservableList<?> source) {
    return (ObservableList<T>) source;
  }

  private static String idOf(Object item) {
    return invokeStringGetter(item, "id");
  }

  private static String titleOf(Object item) {
    String title = invokeStringGetter(item, "title");
    return title.isBlank() ? String.valueOf(item) : title;
  }

  private static String invokeStringGetter(Object item, String methodName) {
    if (item == null) {
      return "";
    }
    try {
      Method method = item.getClass().getDeclaredMethod(methodName);
      method.setAccessible(true);
      Object value = method.invoke(item);
      return value == null ? "" : value.toString();
    } catch (ReflectiveOperationException ignored) {
      return "";
    }
  }
}
