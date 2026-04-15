package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/** Integration tests validating cross-feature navigation flows. */
@ExtendWith(ApplicationExtension.class)
class GuiEndToEndExampleTest {

  @SuppressWarnings("UnusedMethod")
  @Start
  private void start(Stage stage) {
    CookYourBooksGuiApp app = new CookYourBooksGuiApp();
    app.start(stage);
  }

  @Test
  void appLaunchesWithExpectedTitle(FxRobot robot) {
    Stage stage = (Stage) robot.listWindows().get(0);
    assertThat(stage.getTitle()).isEqualTo("CookYourBooks");
  }

  @Test
  void librarySelectingRecipeNavigatesToEditor(FxRobot robot) throws Exception {
    robot.clickOn("#libraryButton");
    robot.clickOn("All recipes");

    @SuppressWarnings("rawtypes")
    ListView recipesList = robot.lookup("#recipesListView").queryAs(ListView.class);
    WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> !recipesList.getItems().isEmpty());

    robot.interact(
        () -> {
          recipesList.requestFocus();
          recipesList.getSelectionModel().select(0);
        });
    robot.type(KeyCode.ENTER);

    Label titleLabel = robot.lookup("#titleLabel").queryAs(Label.class);
    assertThat(titleLabel.getText()).isNotBlank();
  }

  @Test
  void searchResultNavigationOpensSelectedRecipeInEditor(FxRobot robot) throws Exception {
    robot.clickOn("#searchButton");
    robot.clickOn("#queryField");
    robot.write("cake");

    @SuppressWarnings("rawtypes")
    ListView resultsList = robot.lookup("#resultsList").queryAs(ListView.class);
    WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> !resultsList.getItems().isEmpty());

    AtomicReference<String> expectedTitle = new AtomicReference<>("");
    robot.interact(
        () -> {
          Object firstItem = resultsList.getItems().get(0);
          expectedTitle.set(titleOf(firstItem));
          resultsList.requestFocus();
          resultsList.getSelectionModel().select(0);
        });

    robot.type(KeyCode.ENTER);

    Label titleLabel = robot.lookup("#titleLabel").queryAs(Label.class);
    assertThat(titleLabel.getText()).isEqualTo(expectedTitle.get());
  }

  @Test
  void libraryAddRecipeShortcutOpensEditorWithDraft(FxRobot robot) {
    robot.clickOn("#libraryButton");
    robot.clickOn("#recipeOptionsButton");
    robot.clickOn("Add Recipe");

    Label titleLabel = robot.lookup("#titleLabel").queryAs(Label.class);
    assertThat(titleLabel.getText()).isEqualTo("New Recipe");
  }

  @Test
  void sidebarNavigationButtonsArePresent(FxRobot robot) {
    Button libraryBtn = robot.lookup("#libraryButton").queryAs(Button.class);
    assertThat(libraryBtn.getText()).isEqualTo("Library");

    Button editorBtn = robot.lookup("#editorButton").queryAs(Button.class);
    assertThat(editorBtn.getText()).isEqualTo("Recipe Editor");

    Button importBtn = robot.lookup("#importButton").queryAs(Button.class);
    assertThat(importBtn.getText()).isEqualTo("Import");

    Button searchBtn = robot.lookup("#searchButton").queryAs(Button.class);
    assertThat(searchBtn.getText()).isEqualTo("Search");
  }

  private static String titleOf(Object item) {
    if (item == null) {
      return "";
    }
    try {
      Method method = item.getClass().getMethod("getTitle");
      Object value = method.invoke(item);
      return value == null ? "" : value.toString();
    } catch (ReflectiveOperationException ignored) {
      return String.valueOf(item);
    }
  }
}
