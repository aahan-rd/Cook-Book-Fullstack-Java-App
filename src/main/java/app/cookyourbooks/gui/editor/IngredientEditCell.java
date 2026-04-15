package app.cookyourbooks.gui.editor;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import app.cookyourbooks.gui.shared.EditableIngredient;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModel;

/**
 * Custom ListCell for one ingredient row in edit mode.
 *
 * <p>Layout per row (matching wireframe):
 *
 * <pre>
 *   [1/2 cup flour          ] [↑] [↓] [×]
 * </pre>
 *
 * <p>The display label shows the full ingredient string including quantity and unit via {@link
 * EditableIngredient#getDisplayString()}. ↑ and ↓ call {@link
 * RecipeEditorViewModelImpl#moveIngredientUp} / {@link
 * RecipeEditorViewModelImpl#moveIngredientDown}. × calls {@link
 * RecipeEditorViewModel#removeIngredient(int)}.
 */
public class IngredientEditCell extends ListCell<EditableIngredient> {

  private final RecipeEditorViewModel viewModel;

  // Reused across updateItem calls — created once per cell instance
  private final HBox layout = new HBox(6);
  private final Label displayLabel = new Label();
  private final Button upBtn = new Button("↑");
  private final Button downBtn = new Button("↓");
  private final Button removeBtn = new Button("×");

  /** Creates an IngredientEditCell. */
  public IngredientEditCell(RecipeEditorViewModel viewModel) {
    this.viewModel = viewModel;

    // Label grows to fill available width
    HBox.setHgrow(displayLabel, Priority.ALWAYS);
    displayLabel.setMaxWidth(Double.MAX_VALUE);
    displayLabel.setStyle("-fx-font-size: 13px;");

    String btnStyle =
        "-fx-background-color: #f0f0f0;"
            + "-fx-font-size: 12px;"
            + "-fx-padding: 3 8 3 8;"
            + "-fx-min-width: 30px;";
    upBtn.setStyle(btnStyle);
    downBtn.setStyle(btnStyle);
    removeBtn.setStyle(
        "-fx-background-color: transparent;"
            + "-fx-text-fill: #cc0000;"
            + "-fx-font-size: 14px;"
            + "-fx-padding: 2 6 2 6;");

    layout.setAlignment(Pos.CENTER_LEFT);
    layout.getChildren().addAll(displayLabel, upBtn, downBtn, removeBtn);
  }

  @Override
  protected void updateItem(EditableIngredient item, boolean empty) {
    super.updateItem(item, empty);

    // Clear previous handlers to avoid stale actions on recycled cells
    upBtn.setOnAction(null);
    downBtn.setOnAction(null);
    removeBtn.setOnAction(null);

    if (empty || item == null) {
      setGraphic(null);
      setText(null);
      return;
    }

    // Show full ingredient string: "2 cups flour", "salt (to taste)", etc.
    displayLabel.setText(item.getDisplayString());

    // Also update label when name is edited (e.g. user types in another cell)
    item.nameProperty()
        .addListener((obs, old, val) -> displayLabel.setText(item.getDisplayString()));

    upBtn.setOnAction(
        e -> {
          if (viewModel instanceof RecipeEditorViewModelImpl impl) {
            impl.moveIngredientUp(getIndex());
          }
        });

    downBtn.setOnAction(
        e -> {
          if (viewModel instanceof RecipeEditorViewModelImpl impl) {
            impl.moveIngredientDown(getIndex());
          }
        });

    removeBtn.setOnAction(e -> viewModel.removeIngredient(getIndex()));

    // Disable all buttons while saving
    upBtn.disableProperty().bind(viewModel.isSavingProperty());
    downBtn.disableProperty().bind(viewModel.isSavingProperty());
    removeBtn.disableProperty().bind(viewModel.isSavingProperty());

    setGraphic(layout);
    setText(null);
  }
}
