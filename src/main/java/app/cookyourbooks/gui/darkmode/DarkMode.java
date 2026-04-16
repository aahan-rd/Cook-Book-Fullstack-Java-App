package app.cookyourbooks.gui.darkmode;

import java.util.prefs.Preferences;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

/**
 * Manages dark mode state and styling for the application shell.
 *
 * <p>Encapsulates theme toggling, persistence via Java Preferences, and style class management.
 * This class is responsible for maintaining the dark mode state and applying the theme to the root
 * pane and theme toggle button.
 */
public class DarkMode {

  private static final String DARK_MODE_CLASS = "dark-mode";
  public static final String PREF_KEY_DARK_MODE = "darkModeEnabled";

  private final BorderPane rootPane;
  private final Button themeToggleButton;
  private final Preferences preferences;
  private boolean darkModeEnabled;

  /**
   * Constructs a DarkMode manager for the given root pane and toggle button.
   *
   * @param rootPane the main BorderPane to apply dark-mode styling to
   * @param themeToggleButton the button that toggles dark mode; its text will be updated to reflect
   *     the current mode
   */
  public DarkMode(BorderPane rootPane, Button themeToggleButton) {
    this.rootPane = rootPane;
    this.themeToggleButton = themeToggleButton;
    this.preferences = Preferences.userNodeForPackage(DarkMode.class);
  }

  /** Initializes the theme from persistent preferences and applies it. */
  public void initialize() {
    darkModeEnabled = preferences.getBoolean(PREF_KEY_DARK_MODE, false);
    applyTheme(darkModeEnabled);
  }

  /** Toggles dark mode and persists the new state. */
  public void toggle() {
    darkModeEnabled = !darkModeEnabled;
    applyTheme(darkModeEnabled);
    preferences.putBoolean(PREF_KEY_DARK_MODE, darkModeEnabled);
  }

  /** Returns whether dark mode is currently enabled. */
  public boolean isDarkModeEnabled() {
    return darkModeEnabled;
  }

  private void applyTheme(boolean darkMode) {
    if (darkMode) {
      if (!rootPane.getStyleClass().contains(DARK_MODE_CLASS)) {
        rootPane.getStyleClass().add(DARK_MODE_CLASS);
      }
      themeToggleButton.setText("☾ Dark Mode");
    } else {
      rootPane.getStyleClass().remove(DARK_MODE_CLASS);
      themeToggleButton.setText("☀ Light Mode");
    }
  }
}
