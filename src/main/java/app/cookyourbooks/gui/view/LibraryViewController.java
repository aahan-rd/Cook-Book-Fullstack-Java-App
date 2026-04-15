package app.cookyourbooks.gui.view;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.viewmodel.LibraryViewModel;
import app.cookyourbooks.model.Recipe;

/** Controller for LibraryView.fxml. */
@SuppressWarnings("NullAway.Init")
public final class LibraryViewController {

  private static final String DEFAULT_RECIPES_SECTION_TITLE = "All Recipes";
  private static final KeyCombination FOCUS_SEARCH_SHORTCUT =
      new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
  private static final KeyCombination FOCUS_FILTER_SHORTCUT =
      new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN);

  @FXML private BorderPane root;
  @FXML private Button newCollectionButton;
  @FXML private TextField queryField;
  @FXML private Button searchButton;
  @FXML private Button filterButton;
  @FXML private Button recipeOptionsButton;
  @FXML private ScrollPane collectionsScrollPane;
  @FXML private VBox collectionsContainer;
  @FXML private ListView<Object> recipesListView;
  @FXML private Label recipesSectionTitleLabel;
  @FXML private Label recipeCountLabel;
  @FXML private Label emptyStateLabel;
  @FXML private TextField recipeQueryField;
  @FXML private Button recipeSearchButton;
  @FXML private Button recipeFilterButton;
  @FXML private HBox undoBar;
  @FXML private Label undoMessageLabel;
  @FXML private Button undoButton;
  @FXML private Label liveRegionLabel;

  private final LibraryViewModel viewModel;
  private final NavigationService navigationService;
  private ContextMenu recipeOptionsMenu;
  private String activeRecipesSectionTitle = DEFAULT_RECIPES_SECTION_TITLE;

  /**
   * Creates the controller for the Library screen.
   *
   * @param viewModel library screen view-model
   * @param navigationService app navigation service
   */
  public LibraryViewController(LibraryViewModel viewModel, NavigationService navigationService) {
    this.viewModel = viewModel;
    this.navigationService = navigationService;
  }

  /** Initializes bindings, layout behavior, handlers, and triggers first refresh. */
  @SuppressWarnings("UnusedMethod")
  @FXML
  private void initialize() {
    bindViewModel();
    configureLayout();
    configureActions();
    configureKeyboardShortcuts();
    viewModel.refresh();
  }

  /** Binds JavaFX controls and listeners to view-model state and events. */
  @SuppressWarnings("unchecked")
  private void bindViewModel() {
    queryField
        .textProperty()
        .addListener((obs, oldValue, newValue) -> viewModel.setQuery(newValue));
    queryField.setOnAction(e -> viewModel.setQuery(queryField.getText()));
    searchButton.setOnAction(e -> viewModel.setQuery(queryField.getText()));
    queryField.setText(viewModel.getQuery());

    recipeQueryField
        .textProperty()
        .addListener((obs, oldValue, newValue) -> viewModel.setRecipeQuery(newValue));
    recipeQueryField.setOnAction(e -> viewModel.setRecipeQuery(recipeQueryField.getText()));
    recipeSearchButton.setOnAction(e -> viewModel.setRecipeQuery(recipeQueryField.getText()));
    recipeQueryField.setText(viewModel.getRecipeQuery());

    viewModel
        .collectionsProperty()
        .addListener((ListChangeListener<Object>) change -> refreshCollectionCards());

    ObservableList<Object> recipeItems =
        (ObservableList<Object>) (ObservableList<?>) viewModel.recipesProperty();
    recipesListView.setItems(recipeItems);
    recipeItems.addListener((ListChangeListener<Object>) change -> handleRecipesChanged());

    viewModel
        .loadingProperty()
        .addListener((obs, wasLoading, isLoading) -> collectionsScrollPane.setDisable(isLoading));

    undoMessageLabel.textProperty().bind(viewModel.undoMessageProperty());
    undoBar.visibleProperty().bind(viewModel.undoAvailableProperty());
    undoBar.managedProperty().bind(viewModel.undoAvailableProperty());
    viewModel
        .undoAvailableProperty()
        .addListener(
            (obs, wasAvailable, isAvailable) -> {
              String announcement =
                  isAvailable
                      ? viewModel.undoMessageProperty().get() + ". Undo available for 5 seconds."
                      : "Undo expired.";
              liveRegionLabel.setText(announcement);
              liveRegionLabel.setAccessibleText(announcement);
            });

    refreshCollectionCards();
    updateRecipesSectionTitle();
    handleRecipesChanged();
  }

  /** Registers action handlers for toolbar and undo controls. */
  private void configureActions() {
    newCollectionButton.setOnAction(e -> promptCreateCollection());
    filterButton.setOnAction(e -> navigationService.navigateTo(NavigationService.View.SEARCH));
    recipeFilterButton.setOnAction(
        e -> navigationService.navigateTo(NavigationService.View.SEARCH));
    recipeOptionsButton.setOnAction(e -> showRecipeOptionsMenu());
    undoButton.setOnAction(e -> viewModel.undoDelete());
  }

  /** Opens or closes the recipe options context menu anchored to the options button. */
  private void showRecipeOptionsMenu() {
    if (recipeOptionsMenu == null) {
      recipeOptionsMenu = buildRecipeOptionsMenu();
    }
    if (recipeOptionsMenu.isShowing()) {
      recipeOptionsMenu.hide();
    } else {
      recipeOptionsMenu.show(recipeOptionsButton, javafx.geometry.Side.BOTTOM, 0, 4);
    }
  }

  /** Builds the recipe options menu for add/delete/import actions. */
  private ContextMenu buildRecipeOptionsMenu() {
    MenuItem addRecipe = new MenuItem("Add Recipe");
    addRecipe.setOnAction(e -> onAddRecipeRequested());

    MenuItem deleteRecipe = new MenuItem("Delete Recipe");
    deleteRecipe.setOnAction(e -> onDeleteRecipeRequested());

    MenuItem importRecipe = new MenuItem("Import");
    importRecipe.setOnAction(e -> onImportRequested());

    return new ContextMenu(addRecipe, deleteRecipe, importRecipe);
  }

  /** Starts Recipe Editor with a new draft and current collection context. */
  private void onAddRecipeRequested() {
    String selectedCollectionId = viewModel.getSelectedCollectionId();
    if (selectedCollectionId == null
        || LibraryViewModel.ALL_RECIPES_ID.equals(selectedCollectionId)) {
      navigationService.setPendingTargetCollectionId(null);
    } else {
      navigationService.setPendingTargetCollectionId(selectedCollectionId);
    }
    Recipe draftRecipe = new Recipe(null, "New Recipe", null, List.of(), List.of(), List.of());
    navigationService.navigateToEditorWithDraft(draftRecipe);
  }

  /** Navigates to the import screen. */
  private void onImportRequested() {
    navigationService.navigateTo(NavigationService.View.IMPORT);
  }

  /** Deletes the selected recipe after validation and confirmation. */
  private void onDeleteRecipeRequested() {
    Object selectedItem = recipesListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      String message = "Select a recipe to delete.";
      liveRegionLabel.setText(message);
      liveRegionLabel.setAccessibleText(message);
      return;
    }

    String recipeId = extractId(selectedItem);
    String recipeTitle = extractTitle(selectedItem);
    if (recipeId.isBlank()) {
      String message = "Unable to delete the selected recipe.";
      liveRegionLabel.setText(message);
      liveRegionLabel.setAccessibleText(message);
      return;
    }

    confirmAndDeleteRecipe(recipeId, recipeTitle);
  }

  /** Configures focus behavior, recipe cell rendering, and keyboard activation. */
  private void configureLayout() {
    collectionsContainer.setFillWidth(true);
    collectionsContainer.setFocusTraversable(true);
    recipesListView.setFocusTraversable(true);
    recipesListView.setCellFactory(listView -> createRecipeCell());
    recipesListView.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if ((event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE)
              && activateRecipe(recipesListView.getSelectionModel().getSelectedItem())) {
            event.consume();
          }
        });
  }

  /** Configures screen-level keyboard shortcuts for search and filter focus. */
  private void configureKeyboardShortcuts() {
    root.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (FOCUS_SEARCH_SHORTCUT.match(event)) {
            queryField.requestFocus();
            queryField.selectAll();
            event.consume();
          } else if (FOCUS_FILTER_SHORTCUT.match(event)) {
            filterButton.requestFocus();
            event.consume();
          }
        });
  }

  /** Shows dialog for creating a collection and forwards valid names to the view-model. */
  private void promptCreateCollection() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("New Collection");
    dialog.setHeaderText("Create a new recipe collection");
    dialog.setContentText("Collection name:");

    Optional<String> result = dialog.showAndWait();
    result.map(String::trim).filter(name -> !name.isBlank()).ifPresent(viewModel::createCollection);
  }

  /** Updates recipe count label and empty-state visibility for the recipe list panel. */
  private void updateRecipeAreaState() {
    int size = recipesListView.getItems().size();
    recipeCountLabel.setText(size == 1 ? "1 recipe" : size + " recipes");
    boolean hasRecipes = size > 0;
    emptyStateLabel.setVisible(!hasRecipes);
    emptyStateLabel.setManaged(!hasRecipes);
  }

  /** Announces recipe load results to the live region for accessibility feedback. */
  private void announceRecipeLoad() {
    int size = recipesListView.getItems().size();
    String announcement = size == 0 ? "0 results found" : "Loaded " + size + " recipes";
    liveRegionLabel.setText(announcement);
    liveRegionLabel.setAccessibleText(announcement);
  }

  /** Focuses the first recipe when available, otherwise focuses the empty-state label. */
  private void focusFirstRecipeOrEmptyState() {
    Platform.runLater(
        () -> {
          if (!recipesListView.getItems().isEmpty()) {
            recipesListView.getSelectionModel().selectFirst();
            recipesListView.scrollTo(0);
            recipesListView.requestFocus();
          } else {
            emptyStateLabel.requestFocus();
          }
        });
  }

  /** Rebuilds collection cards from the view-model projection and refreshes section title. */
  private void refreshCollectionCards() {
    collectionsContainer
        .getChildren()
        .setAll(createCollectionCards(viewModel.collectionsProperty()));
    updateRecipesSectionTitle();
  }

  /** Applies standard UI refresh work when the recipe list changes. */
  private void handleRecipesChanged() {
    recipesListView.getSelectionModel().clearSelection();
    updateRecipeAreaState();
    announceRecipeLoad();
  }

  /** Creates card nodes for each collection item emitted by the view-model. */
  private java.util.List<Node> createCollectionCards(javafx.collections.ObservableList<?> items) {
    String selectedCollectionId = viewModel.getSelectedCollectionId();
    java.util.List<Node> cards = new java.util.ArrayList<>();
    for (Object item : items) {
      String collectionId = extractId(item);
      String title = extractTitle(item);
      int recipeCount = extractRecipeCount(item);
      cards.add(
          createCollectionCardButton(
              collectionId,
              title,
              recipeCount == 1 ? "1 recipe" : recipeCount + " recipes",
              title + ", " + recipeCount + (recipeCount == 1 ? " recipe" : " recipes"),
              collectionId.equals(selectedCollectionId),
              () -> {
                viewModel.selectCollection(collectionId);
                activeRecipesSectionTitle = title;
                refreshCollectionCards();
                focusFirstRecipeOrEmptyState();
              }));
    }
    return cards;
  }

  /** Resolves and updates the active recipes section title based on current selection. */
  private void updateRecipesSectionTitle() {
    String selectedCollectionId = viewModel.getSelectedCollectionId();
    if (selectedCollectionId == null || selectedCollectionId.isBlank()) {
      activeRecipesSectionTitle = DEFAULT_RECIPES_SECTION_TITLE;
    } else {
      String selectedTitle =
          findCollectionTitleById(selectedCollectionId, viewModel.collectionsProperty());
      if (!selectedTitle.isBlank()) {
        activeRecipesSectionTitle = selectedTitle;
      }
    }

    recipesSectionTitleLabel.setText(activeRecipesSectionTitle);
    recipesSectionTitleLabel.setAccessibleText(activeRecipesSectionTitle);
  }

  /** Finds a collection title by ID from the currently displayed collection list items. */
  private static String findCollectionTitleById(String collectionId, ObservableList<?> items) {
    for (Object item : items) {
      if (collectionId.equals(extractId(item))) {
        return extractTitle(item);
      }
    }
    return "";
  }

  /** Extracts an item ID via reflective access to support view-model item projections. */
  private static String extractId(Object item) {
    return extractString(item, "id");
  }

  /** Extracts a user-facing title via reflection with a stable string fallback. */
  private static String extractTitle(Object item) {
    String title = extractString(item, "title");
    return title.isBlank() ? String.valueOf(item) : title;
  }

  /** Extracts recipe count via direct method or recipes collection fallback. */
  private static int extractRecipeCount(Object item) {
    try {
      Optional<Method> countMethod = findMethod(item.getClass(), "recipeCount", "getRecipeCount");
      if (countMethod.isPresent()) {
        Method method = countMethod.get();
        method.setAccessible(true);
        Object countValue = method.invoke(item);
        if (countValue instanceof Number count) {
          return count.intValue();
        }
      }

      Optional<Method> recipesMethod = findMethod(item.getClass(), "recipes", "getRecipes");
      if (recipesMethod.isPresent()) {
        Method method = recipesMethod.get();
        method.setAccessible(true);
        Object recipesValue = method.invoke(item);
        if (recipesValue instanceof java.util.Collection<?> recipes) {
          return recipes.size();
        }
      }
    } catch (ReflectiveOperationException ignored) {
      // Keep fallback to 0 for unknown list item types.
    }
    return 0;
  }

  /** Extracts a string-valued property by method name from a projection item. */
  private static String extractString(Object item, String methodName) {
    if (item == null) {
      return "";
    }
    try {
      Optional<Method> method =
          findMethod(item.getClass(), methodName, "get" + capitalize(methodName));
      if (method.isEmpty()) {
        return "";
      }
      Method reflectedMethod = method.get();
      reflectedMethod.setAccessible(true);
      Object value = reflectedMethod.invoke(item);
      return value == null ? "" : value.toString();
    } catch (ReflectiveOperationException ignored) {
      return "";
    }
  }

  /** Finds a no-arg method by searching declared methods first, then public methods. */
  private static Optional<Method> findMethod(Class<?> type, String... candidateNames) {
    for (String candidateName : candidateNames) {
      try {
        return Optional.of(type.getDeclaredMethod(candidateName));
      } catch (NoSuchMethodException ignored) {
        // Try next candidate.
      }

      try {
        return Optional.of(type.getMethod(candidateName));
      } catch (NoSuchMethodException ignored) {
        // Try next candidate.
      }
    }
    return Optional.empty();
  }

  /** Capitalizes first character of a string or returns empty string for null/empty input. */
  private static String capitalize(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  /** Creates the recipe list cell renderer and click activation behavior. */
  private ListCell<Object> createRecipeCell() {
    ListCell<Object> cell =
        new ListCell<>() {
          @Override
          protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
              setText(null);
              setAccessibleText(null);
            } else {
              String title = extractTitle(item);
              setText(title);
              setAccessibleText(title);
            }
          }
        };
    cell.setOnMouseClicked(
        event -> {
          if (event.getButton() == MouseButton.PRIMARY && !cell.isEmpty()) {
            activateRecipe(cell.getItem());
          }
        });
    return cell;
  }

  /** Attempts to activate a recipe item by ID and navigate through the view-model. */
  private boolean activateRecipe(Object item) {
    if (item == null) {
      return false;
    }

    String recipeId = extractId(item);
    if (recipeId.isBlank()) {
      return false;
    }

    viewModel.selectRecipe(recipeId);
    return true;
  }

  /** Builds one collection card node and wires its primary and optional delete actions. */
  private Node createCollectionCardButton(
      String collectionId,
      String title,
      String subtitle,
      String accessibleText,
      boolean selected,
      Runnable action) {
    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("card-title");
    titleLabel.setWrapText(true);

    Label subtitleLabel = new Label(subtitle);
    subtitleLabel.getStyleClass().add("card-subtitle");
    subtitleLabel.setWrapText(true);

    VBox content = new VBox(4, titleLabel, subtitleLabel);
    content.getStyleClass().add("card-content");

    Button card = new Button();
    card.setGraphic(content);
    card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    card.setMaxWidth(Double.MAX_VALUE);
    card.setMinWidth(0);
    card.prefWidthProperty().bind(collectionsContainer.widthProperty());
    card.setFocusTraversable(true);
    card.setAccessibleText(accessibleText);
    card.getStyleClass().add("library-card");
    card.getStyleClass().add("collection-card");
    if (selected) {
      card.getStyleClass().add("selected-card");
    }
    card.setOnAction(e -> action.run());

    StackPane wrapper = new StackPane(card);
    wrapper.getStyleClass().add("collection-card-wrapper");

    if (!LibraryViewModel.ALL_RECIPES_ID.equals(collectionId)) {
      Button deleteButton = new Button("x");
      deleteButton.getStyleClass().add("collection-delete-button");
      deleteButton.setFocusTraversable(false);
      deleteButton.setAccessibleText("Delete collection " + title);
      deleteButton.setOnAction(
          e -> {
            e.consume();
            confirmAndDeleteCollection(collectionId, title);
          });
      wrapper.getChildren().add(deleteButton);
      StackPane.setAlignment(deleteButton, Pos.TOP_RIGHT);
    }

    return wrapper;
  }

  /** Shows confirmation dialog and deletes a collection when confirmed. */
  private void confirmAndDeleteCollection(String collectionId, String title) {
    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
    confirmation.setTitle("Delete Collection");
    confirmation.setHeaderText("Delete collection?");
    confirmation.setContentText("Delete \"" + title + "\"? You can undo for 5 seconds.");

    Optional<ButtonType> result = confirmation.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      viewModel.deleteCollection(collectionId);
    }
  }

  /** Shows confirmation dialog and deletes recipe from scope when confirmed. */
  private void confirmAndDeleteRecipe(String recipeId, String recipeTitle) {
    boolean deletingFromLibrary =
        LibraryViewModel.ALL_RECIPES_ID.equals(viewModel.getSelectedCollectionId());

    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
    confirmation.setTitle("Delete Recipe");
    confirmation.setHeaderText(
        deletingFromLibrary ? "Delete recipe from library?" : "Delete recipe from collection?");
    confirmation.setContentText(
        (deletingFromLibrary
                ? "Delete \"" + recipeTitle + "\" from the library?"
                : "Remove \"" + recipeTitle + "\" from this collection?")
            + " You can undo for 5 seconds.");

    Optional<ButtonType> result = confirmation.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      viewModel.deleteRecipe(recipeId);
    }
  }
}
