package app.cookyourbooks.gui.view;

import java.util.EnumMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import app.cookyourbooks.gui.NavigationService;
import app.cookyourbooks.gui.NavigationService.View;

/**
 * Controller for the main application layout ({@code MainView.fxml}).
 *
 * <p>
 * Manages the sidebar navigation buttons and swaps the content area when the
 * user navigates
 * between features. Each feature's view is provided via
 * {@link #setViewNode(View, Node)} during
 * application startup.
 *
 * <h2>How navigation works</h2>
 *
 * <ol>
 * <li>The user clicks a sidebar button (e.g., "Library")
 * <li>The button handler calls {@link NavigationService#navigateTo(View)}
 * <li>The navigation listener in this controller swaps the content area to show
 * the corresponding
 * feature view
 * </ol>
 */
@SuppressWarnings("NullAway.Init") // FXML fields are injected by the FXMLLoader, not the constructor
public class MainViewController {

  private static final String DARK_MODE_CLASS = "dark-mode";
  private static final String PREF_KEY_DARK_MODE = "darkModeEnabled";

  @FXML
  private BorderPane rootPane;
  @FXML
  private StackPane contentArea;
  @FXML
  private Button libraryButton;
  @FXML
  private Button editorButton;
  @FXML
  private Button importButton;
  @FXML
  private Button searchButton;
  @FXML
  private Button themeToggleButton;

  private final NavigationService navigationService;
  private final Map<View, Node> viewNodes = new EnumMap<>(View.class);
  private final Preferences preferences = Preferences.userNodeForPackage(MainViewController.class);
  private boolean darkModeEnabled;

  /**
   * Constructs the main view controller.
   *
   * @param navigationService the shared navigation service
   */
  public MainViewController(NavigationService navigationService) {
    this.navigationService = navigationService;
  }

  /**
   * Registers a feature view's root node for a given navigation view.
   *
   * <p>
   * Call this during app startup for each feature that has been implemented.
   * Views that are not
   * registered will show a placeholder when navigated to.
   *
   * @param view the navigation view
   * @param node the root node of the feature's FXML view
   */
  public void setViewNode(View view, Node node) {
    viewNodes.put(view, node);
  }

  /**
   * Called by FXML after the layout is loaded. Sets up button handlers and
   * navigation listener.
   */
  @SuppressWarnings("UnusedMethod") // Called reflectively by FXMLLoader
  @FXML
  private void initialize() {
    libraryButton.setOnAction(e -> navigationService.navigateTo(View.LIBRARY));
    editorButton.setOnAction(e -> navigationService.navigateTo(View.RECIPE_EDITOR));
    importButton.setOnAction(e -> navigationService.navigateTo(View.IMPORT));
    searchButton.setOnAction(e -> navigationService.navigateTo(View.SEARCH));
    themeToggleButton.setOnAction(e -> toggleTheme());

    darkModeEnabled = preferences.getBoolean(PREF_KEY_DARK_MODE, false);
    applyTheme(darkModeEnabled);

    // Listen for navigation changes and swap the content area
    navigationService
        .currentViewProperty()
        .addListener((obs, oldView, newView) -> showView(newView));

    // Show the initial view
    showView(navigationService.getCurrentView());
  }

  private void toggleTheme() {
    darkModeEnabled = !darkModeEnabled;
    applyTheme(darkModeEnabled);
    preferences.putBoolean(PREF_KEY_DARK_MODE, darkModeEnabled);
  }

  private void applyTheme(boolean darkMode) {
    if (darkMode) {
      if (!rootPane.getStyleClass().contains(DARK_MODE_CLASS)) {
        rootPane.getStyleClass().add(DARK_MODE_CLASS);
      }
      themeToggleButton.setText("Switch to Light");
    } else {
      rootPane.getStyleClass().remove(DARK_MODE_CLASS);
      themeToggleButton.setText("Switch to Dark");
    }
  }

  private void showView(View view) {
    contentArea.getChildren().clear();
    Node node = viewNodes.get(view);
    if (node != null) {
      contentArea.getChildren().add(node);
    } else {
      // Placeholder for features not yet implemented
      Label placeholder = new Label(view.name() + " — not yet implemented");
      placeholder.setStyle("-fx-text-fill: #888; -fx-font-size: 16;");
      contentArea.getChildren().add(placeholder);
    }

    // Update button styles to highlight the active view
    libraryButton.getStyleClass().remove("nav-active");
    editorButton.getStyleClass().remove("nav-active");
    importButton.getStyleClass().remove("nav-active");
    searchButton.getStyleClass().remove("nav-active");

    switch (view) {
      case LIBRARY -> libraryButton.getStyleClass().add("nav-active");
      case RECIPE_EDITOR -> editorButton.getStyleClass().add("nav-active");
      case IMPORT -> importButton.getStyleClass().add("nav-active");
      case SEARCH -> searchButton.getStyleClass().add("nav-active");
      default -> {
        /* no additional views */
      }
    }
  }
}
