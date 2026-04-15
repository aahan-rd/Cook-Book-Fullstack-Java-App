package app.cookyourbooks.gui.editor;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Custom ListCell for one step row in edit mode.
 *
 * <p>Two states per row:
 *
 * <ul>
 *   <li><b>Display state:</b> "1. Mix dry ingredients" label + ✎ + × buttons
 *   <li><b>Editing state:</b> TextArea pre-filled with step text + Save + Cancel
 * </ul>
 *
 * <p>Clicking ✎ switches this cell to editing state. Clicking Save commits the edit via {@code
 * onEdit} callback. Clicking × deletes the step via {@code onDelete} callback. Clicking Cancel
 * reverts to display state.
 *
 * <p>Callbacks are provided by {@link RecipeEditorViewController} which owns the mutable steps
 * list.
 */
public class StepEditCell extends ListCell<String> {

  /** Called with (index, newText) when the user saves an edit. */
  private final BiConsumer<Integer, String> onEdit;

  /** Called with the index when the user clicks ×. */
  private final IntConsumer onDelete;

  /** Bound to isSavingProperty() to disable buttons during async save. */
  private final BooleanProperty saving;

  // ── Display state layout ──
  private final HBox displayLayout = new HBox(6);
  private final Label stepLabel = new Label();
  private final Button editBtn = new Button("✎");
  private final Button deleteBtn = new Button("×");

  // ── Editing state layout ──
  private final VBox editLayout = new VBox(6);
  private final TextArea textArea = new TextArea();
  private final HBox editButtons = new HBox(6);
  private final Button saveEditBtn = new Button("Save");
  private final Button cancelEditBtn = new Button("Cancel");

  /** Whether this cell is currently in inline editing state. */
  private boolean inlineEditing = false;

  /**
   * Creates a StepEditCell.
   *
   * @param onEdit called with (index, newText) when edit is saved
   * @param onDelete called with (index) when step is deleted
   * @param saving bound to isSavingProperty() to disable during async save
   */
  public StepEditCell(
      BiConsumer<Integer, String> onEdit, IntConsumer onDelete, BooleanProperty saving) {
    this.onEdit = onEdit;
    this.onDelete = onDelete;
    this.saving = saving;

    buildDisplayLayout();
    buildEditLayout();
  }

  private void buildDisplayLayout() {
    HBox.setHgrow(stepLabel, Priority.ALWAYS);
    stepLabel.setMaxWidth(Double.MAX_VALUE);
    stepLabel.setWrapText(true);
    stepLabel.setStyle("-fx-font-size: 13px;");

    String btnStyle =
        "-fx-background-color: transparent;" + "-fx-font-size: 13px;" + "-fx-padding: 2 6 2 6;";
    editBtn.setStyle(btnStyle + "-fx-text-fill: #4A90D9;");
    deleteBtn.setStyle(btnStyle + "-fx-text-fill: #cc0000;");

    displayLayout.setAlignment(Pos.CENTER_LEFT);
    displayLayout.getChildren().addAll(stepLabel, editBtn, deleteBtn);
  }

  private void buildEditLayout() {
    textArea.setWrapText(true);
    textArea.setPrefRowCount(3);
    textArea.setStyle("-fx-font-size: 13px;");

    saveEditBtn.setStyle(
        "-fx-background-color: #4CAF50;" + "-fx-text-fill: white;" + "-fx-padding: 4 14 4 14;");
    cancelEditBtn.setStyle("-fx-background-color: #eee;" + "-fx-padding: 4 14 4 14;");

    editButtons.setAlignment(Pos.CENTER_RIGHT);
    editButtons.getChildren().addAll(cancelEditBtn, saveEditBtn);

    editLayout.getChildren().addAll(textArea, editButtons);
  }

  @Override
  protected void updateItem(String item, boolean empty) {
    super.updateItem(item, empty);

    // Reset to display state on cell reuse
    inlineEditing = false;
    editBtn.setOnAction(null);
    deleteBtn.setOnAction(null);
    saveEditBtn.setOnAction(null);
    cancelEditBtn.setOnAction(null);

    if (empty || item == null) {
      setGraphic(null);
      setText(null);
      return;
    }

    stepLabel.setText(item);

    // ✎ — switch this cell to inline editing state
    editBtn.setOnAction(e -> enterEditState(item));
    editBtn.disableProperty().bind(saving);

    // × — delete this step
    deleteBtn.setOnAction(e -> onDelete.accept(getIndex()));
    deleteBtn.disableProperty().bind(saving);

    setGraphic(displayLayout);
    setText(null);
  }

  /**
   * Switches this cell to inline editing state. Pre-fills the TextArea with the current step text.
   *
   * @param currentText the current step text to pre-fill
   */
  private void enterEditState(String currentText) {
    inlineEditing = true;
    textArea.setText(currentText);
    textArea.positionCaret(currentText.length());

    // Save — commit edit and return to display state
    saveEditBtn.setOnAction(
        e -> {
          String newText = textArea.getText().strip();
          if (!newText.isBlank()) {
            onEdit.accept(getIndex(), newText);
          }
          inlineEditing = false;
          stepLabel.setText(newText.isBlank() ? currentText : newText);
          setGraphic(displayLayout);
        });

    // Cancel — revert to display state without saving
    cancelEditBtn.setOnAction(
        e -> {
          inlineEditing = false;
          setGraphic(displayLayout);
        });

    setGraphic(editLayout);
  }
}
