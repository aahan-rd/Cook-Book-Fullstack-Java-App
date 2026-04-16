package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import app.cookyourbooks.gui.darkmode.DarkMode;

/**
 * Dark mode tests for the application shell.
 *
 * <p>These tests are isolated from the general navigation suite so dark mode behavior can be
 * validated independently. Because the implementation persists preference state, the tests clear
 * the stored preference before and after each case to avoid cross-test leakage.
 */
@ExtendWith(ApplicationExtension.class)
class DarkModeGuiTest {

  private final Preferences preferences = Preferences.userNodeForPackage(DarkMode.class);

  @SuppressWarnings("UnusedMethod")
  @Start
  private void start(Stage stage) {
    clearPreference();
    CookYourBooksGuiApp app = new CookYourBooksGuiApp();
    app.start(stage);
  }

  @AfterEach
  void clearPreferenceAfterTest() throws BackingStoreException {
    clearPreference();
  }

  @Test
  void appStartsInLightModeByDefault(FxRobot robot) {
    BorderPane root = robot.lookup("#rootPane").queryAs(BorderPane.class);
    Button toggle = robot.lookup("#themeToggleButton").queryAs(Button.class);

    assertThat(root.getStyleClass()).doesNotContain("dark-mode");
    assertThat(toggle.getText()).isEqualTo("☀ Light Mode");
  }

  @Test
  void clickingToggleEnablesDarkModeAndSavesPreference(FxRobot robot) {
    BorderPane root = robot.lookup("#rootPane").queryAs(BorderPane.class);
    Button toggle = robot.lookup("#themeToggleButton").queryAs(Button.class);

    robot.clickOn("#themeToggleButton");
    org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

    assertThat(root.getStyleClass()).contains("dark-mode");
    assertThat(toggle.getText()).isEqualTo("☾ Dark Mode");
    assertThat(preferences.getBoolean(DarkMode.PREF_KEY_DARK_MODE, false)).isTrue();
  }

  @Test
  void clickingToggleTwiceReturnsToLightMode(FxRobot robot) {
    BorderPane root = robot.lookup("#rootPane").queryAs(BorderPane.class);
    Button toggle = robot.lookup("#themeToggleButton").queryAs(Button.class);

    robot.clickOn("#themeToggleButton");
    org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
    robot.clickOn("#themeToggleButton");
    org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

    assertThat(root.getStyleClass()).doesNotContain("dark-mode");
    assertThat(toggle.getText()).isEqualTo("☀ Light Mode");
    assertThat(preferences.getBoolean(DarkMode.PREF_KEY_DARK_MODE, true)).isFalse();
  }

  private void clearPreference() {
    try {
      preferences.clear();
      preferences.flush();
    } catch (BackingStoreException e) {
      throw new RuntimeException("Failed to clear dark mode preference", e);
    }
  }
}
