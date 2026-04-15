package app.cookyourbooks.gui.view;

import java.lang.reflect.Method;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import app.cookyourbooks.gui.viewmodel.SearchViewModel;
import app.cookyourbooks.model.Recipe;

/**
 * Controller for the Search & Filter view.
 *
 * <p><b>Responsibilities:</b>
 *
 * <ul>
 *   <li>Bind JavaFX UI components to {@link SearchViewModel} properties
 *   <li>Handle user interactions (typing, filtering, navigation)
 *   <li>Display search results with keyboard and mouse navigation
 *   <li>Render filter badges with remove buttons
 *   <li>Sync UI selection state with ViewModel
 * </ul>
 *
 * <p><b>Key Features:</b>
 *
 * <ul>
 *   <li><b>Tab Navigation:</b> queryField → ingredientComboBox → collectionComboBox → resultsList
 *   <li><b>Keyboard Navigation:</b> Arrow keys cycle through results, Enter opens recipe
 *   <li><b>Filter Pills:</b> Active ingredient and collection filters display as removable badges
 *   <li><b>Live Status:</b> Result count updates as search executes
 * </ul>
 *
 * @see SearchViewModel
 * @see app.cookyourbooks.gui.viewmodel.impl.SearchViewModelImpl
 */
@SuppressWarnings({"NullAway.Init", "unchecked"})
public final class SearchViewController {

  @FXML private TextField queryField;
  @FXML private Button searchButton;
  @FXML private ComboBox<String> ingredientComboBox;
  @FXML private Button addIngredientButton;
  @FXML private Button clearFiltersButton;
  @FXML private FlowPane ingredientFiltersList;
  @FXML private ComboBox<String> collectionComboBox;
  @FXML private Button addCollectionButton;
  @FXML private Button clearCollectionButton;
  @FXML private FlowPane collectionFiltersList;
  @FXML private ListView<Object> resultsList;
  @FXML private Label statusLabel;
  @FXML private ProgressIndicator loadingSpinner;

  private final SearchViewModel viewModel;

  /**
   * Constructs a SearchViewController with the given ViewModel.
   *
   * @param viewModel the SearchViewModel providing data and commands
   */
  public SearchViewController(SearchViewModel viewModel) {
    this.viewModel = viewModel;
  }

  /**
   * Initializes the controller.
   *
   * <p>Called by JavaFX after FXML components are injected. Wires up:
   *
   * <ul>
   *   <li>Query field listener to trigger search
   *   <li>Tab navigation between filter controls
   *   <li>Ingredient and collection combo boxes with available options
   *   <li>Result list rendering and keyboard/mouse handling
   *   <li>Status and loading indicator bindings
   * </ul>
   */
  @FXML
  private void initialize() {
    // Query field and search button
    queryField
        .textProperty()
        .addListener((obs, oldValue, newValue) -> viewModel.setQuery(newValue));
    searchButton.setOnAction(e -> viewModel.setQuery(queryField.getText()));

    // Tab navigation from queryField to ingredientComboBox
    queryField.setOnKeyPressed(
        e -> {
          if (e.getCode() == javafx.scene.input.KeyCode.TAB) {
            ingredientComboBox.requestFocus();
            e.consume();
          }
        });

    // Populate ingredient combo box
    ingredientComboBox.setItems(
        FXCollections.observableArrayList(viewModel.getAvailableIngredients()));
    addIngredientButton.setOnAction(e -> addIngredientFilter());

    // Tab navigation from ingredientComboBox to collectionComboBox
    ingredientComboBox.setOnKeyPressed(
        e -> {
          if (e.getCode() == javafx.scene.input.KeyCode.TAB) {
            collectionComboBox.requestFocus();
            e.consume();
          }
        });

    // Populate collection combo box
    collectionComboBox.setItems(
        FXCollections.observableArrayList(viewModel.getAvailableCollections()));
    addCollectionButton.setOnAction(e -> addCollectionFilter());

    // Tab navigation from collectionComboBox back to queryField
    collectionComboBox.setOnKeyPressed(
        e -> {
          if (e.getCode() == javafx.scene.input.KeyCode.TAB && e.isShiftDown()) {
            queryField.requestFocus();
            e.consume();
          }
        });

    // Ingredient filters as FlowPane with pill-style badges
    viewModel
        .ingredientFiltersProperty()
        .addListener((ListChangeListener<String>) change -> updateIngredientFilterDisplay());
    updateIngredientFilterDisplay();

    clearFiltersButton.setOnAction(e -> viewModel.clearFilters());

    // Collection filters as FlowPane with pill-style badges
    viewModel
        .collectionFiltersProperty()
        .addListener((ListChangeListener<String>) change -> updateCollectionFilterDisplay());
    updateCollectionFilterDisplay();

    clearCollectionButton.setOnAction(e -> viewModel.clearFilters());

    // Results list
    resultsList.setItems((ObservableList<Object>) viewModel.resultsProperty());
    resultsList.setCellFactory(
        lv ->
            new javafx.scene.control.ListCell<>() {
              @Override
              protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                  return;
                }
                setText(titleOf(item));
              }
            });

    resultsList.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            Object selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
              viewModel.setSelectedResultId(idOf(selected));
              System.out.println(
                  "DEBUG: Search double-click title="
                      + titleOf(selected)
                      + ", clickedId="
                      + idOf(selected)
                      + ", vmSelectedId="
                      + viewModel.getSelectedResultId());
            }
            viewModel.navigateToSelectedResult();
          }
        });

    resultsList.setOnKeyPressed(
        e -> {
          switch (e.getCode()) {
            case ENTER -> {
              Object selected = resultsList.getSelectionModel().getSelectedItem();
              if (selected != null) {
                viewModel.setSelectedResultId(idOf(selected));
              }
              viewModel.navigateToSelectedResult();
              e.consume();
            }
            case DOWN -> {
              viewModel.selectNextResult();
              syncSelectionFromViewModel();
              e.consume();
            }
            case UP -> {
              viewModel.selectPreviousResult();
              syncSelectionFromViewModel();
              e.consume();
            }
            default -> {
              /* no action */
            }
          }
        });

    ((ObservableList<Object>) viewModel.resultsProperty())
        .addListener((ListChangeListener<Object>) c -> syncSelectionFromViewModel());

    statusLabel.textProperty().bind(viewModel.statusMessageProperty());
    loadingSpinner.visibleProperty().bind(viewModel.searchingProperty());
    loadingSpinner.managedProperty().bind(viewModel.searchingProperty());

    queryField.setText(viewModel.getQuery());
    syncSelectionFromViewModel();
  }

  /**
   * Adds the selected ingredient from the combo box as a filter.
   *
   * <p>Resets the combo box after adding.
   */
  private void addIngredientFilter() {
    String selected = ingredientComboBox.getValue();
    if (selected != null && !selected.isBlank()) {
      viewModel.addIngredientFilter(selected);
      ingredientComboBox.setValue(null); // Reset combo box
    }
  }

  /**
   * Updates the ingredient filter display with badge pills.
   *
   * <p>Each badge shows the filter text with a remove button.
   */
  private void updateIngredientFilterDisplay() {
    ingredientFiltersList.getChildren().clear();
    for (String filter : viewModel.ingredientFiltersProperty()) {
      javafx.scene.layout.HBox badge = createFilterBadge(filter, true);
      ingredientFiltersList.getChildren().add(badge);
    }
  }

  /**
   * Adds the selected collection from the combo box as a filter.
   *
   * <p>Resets the combo box after adding.
   */
  private void addCollectionFilter() {
    String selected = collectionComboBox.getValue();
    if (selected != null && !selected.isBlank()) {
      viewModel.addCollectionFilter(selected);
      collectionComboBox.setValue(null);
    }
  }

  /**
   * Updates the collection filter display with badge pills.
   *
   * <p>Each badge shows the filter text with a remove button.
   */
  private void updateCollectionFilterDisplay() {
    collectionFiltersList.getChildren().clear();
    for (String filter : viewModel.collectionFiltersProperty()) {
      javafx.scene.layout.HBox badge = createFilterBadge(filter, false);
      collectionFiltersList.getChildren().add(badge);
    }
  }

  /**
   * Creates a styled filter badge with a remove button.
   *
   * @param filterText the filter label to display
   * @param isIngredient true for ingredient badge (blue), false for collection badge (purple)
   * @return an HBox containing the badge and remove button
   */
  private javafx.scene.layout.HBox createFilterBadge(String filterText, boolean isIngredient) {
    javafx.scene.layout.HBox badge = new javafx.scene.layout.HBox(6);
    javafx.scene.control.Label label = new javafx.scene.control.Label(filterText);
    javafx.scene.control.Button removeBtn = new javafx.scene.control.Button("×");

    badge.setAlignment(javafx.geometry.Pos.CENTER);
    if (isIngredient) {
      badge.setStyle(
          "-fx-background-color: #e3f2fd; "
              + "-fx-border-color: #90caf9; "
              + "-fx-border-radius: 16; "
              + "-fx-background-radius: 16; "
              + "-fx-padding: 4 10;");
      removeBtn.setStyle(
          "-fx-font-size: 14; "
              + "-fx-padding: 0; "
              + "-fx-min-width: 22; "
              + "-fx-min-height: 22; "
              + "-fx-pref-width: 22; "
              + "-fx-pref-height: 22; "
              + "-fx-border-radius: 11; "
              + "-fx-text-fill: #1976d2;");
      removeBtn.setOnAction(e -> viewModel.removeIngredientFilter(filterText));
    } else {
      badge.setStyle(
          "-fx-background-color: #f3e5f5; "
              + "-fx-border-color: #ce93d8; "
              + "-fx-border-radius: 16; "
              + "-fx-background-radius: 16; "
              + "-fx-padding: 4 10;");
      removeBtn.setStyle(
          "-fx-font-size: 14; "
              + "-fx-padding: 0; "
              + "-fx-min-width: 22; "
              + "-fx-min-height: 22; "
              + "-fx-pref-width: 22; "
              + "-fx-pref-height: 22; "
              + "-fx-border-radius: 11; "
              + "-fx-text-fill: #7b1fa2;");
      removeBtn.setOnAction(e -> viewModel.removeCollectionFilter(filterText));
    }

    label.setStyle("-fx-font-size: 12;");
    badge.getChildren().addAll(label, removeBtn);
    return badge;
  }

  /**
   * Syncs the ListView selection with the currently selected result from ViewModel.
   *
   * <p>If the selected recipe ID is not in the current results, clears selection.
   */
  private void syncSelectionFromViewModel() {
    String selectedId = viewModel.getSelectedResultId();
    if (selectedId == null) {
      resultsList.getSelectionModel().clearSelection();
      return;
    }

    ObservableList<Object> items = resultsList.getItems();
    for (int i = 0; i < items.size(); i++) {
      if (selectedId.equals(idOf(items.get(i)))) {
        resultsList.getSelectionModel().select(i);
        resultsList.scrollTo(i);
        return;
      }
    }
    resultsList.getSelectionModel().clearSelection();
  }

  /**
   * Extracts the recipe ID from a result object using reflection.
   *
   * <p>Handles both Recipe instances and generic objects with a getId() method.
   *
   * @param item the result object
   * @return the recipe ID, or empty string if not found
   */
  private static String idOf(Object item) {
    if (item instanceof Recipe recipe) {
      return recipe.getId();
    }
    try {
      Method m = item.getClass().getMethod("getId");
      Object value = m.invoke(item);
      return value == null ? "" : value.toString();
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Extracts the recipe title from a result object using reflection.
   *
   * <p>Handles both Recipe instances and generic objects with a getTitle() method.
   *
   * @param item the result object
   * @return the recipe title, or item.toString() if not found
   */
  private static String titleOf(Object item) {
    if (item instanceof Recipe recipe) {
      return recipe.getTitle();
    }
    try {
      Method m = item.getClass().getMethod("getTitle");
      Object value = m.invoke(item);
      return value == null ? item.toString() : value.toString();
    } catch (Exception e) {
      return item.toString();
    }
  }
}
