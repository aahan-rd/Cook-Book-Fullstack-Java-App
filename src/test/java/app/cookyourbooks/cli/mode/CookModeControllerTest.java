package app.cookyourbooks.cli.mode;

import static app.cookyourbooks.cli.assertions.OutputAssertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;
import app.cookyourbooks.cli.fixtures.RecipeFixtures;

/** E2E tests for CookModeController via the cook command. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class CookModeControllerTest extends CliTestBase {

  @Test
  void cookMode_navigatesWithNext() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 4), "Breakfast");

    sendCommands("cook Pancakes", "next", "next", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("COOKING: Pancakes");
    assertThat(getOutput()).contains("Step 1 of 4");
    assertThat(getOutput()).contains("Step 2 of 4");
    assertThat(getOutput()).contains("Step 3 of 4");
    assertThat(getOutput()).contains("[next] [prev] [ingredients] [quit]");
  }

  @Test
  void cookMode_displayStep_showsSeparatorsHintsAndInstructionText() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 2), "Breakfast");

    sendCommands("cook Pancakes", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 1 of 2");
    assertThat(getOutput()).contains("Step 1 instruction");
    assertThat(getOutput()).contains("[next] [prev] [ingredients] [quit]");
    assertThat(getOutput()).contains("──");
  }

  @Test
  void cookMode_navigatesWithShortNext() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 3), "Breakfast");

    sendCommands("cook Pancakes", "n", "n", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 1 of 3");
    assertThat(getOutput()).contains("Step 2 of 3");
    assertThat(getOutput()).contains("Step 3 of 3");
  }

  @Test
  void cookMode_navigatesWithPrev() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 4), "Breakfast");

    sendCommands("cook Pancakes", "next", "next", "prev", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 2 of 4");
    assertThat(getOutput()).contains("Step 3 of 4");
    assertThat(getOutput()).contains("Step 2 of 4"); // After prev
  }

  @Test
  void cookMode_navigatesWithShortPrev() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 3), "Breakfast");

    sendCommands("cook Pancakes", "next", "p", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 1 of 3");
    assertThat(getOutput()).contains("Step 2 of 3");
  }

  @Test
  void cookMode_prevOnFirstStep_displaysAlreadyAtBeginning() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 3), "Breakfast");

    sendCommands("cook Pancakes", "prev", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Already at the beginning.");
  }

  @Test
  void cookMode_nextOnLastStep_displaysFinishedAndExits() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 2), "Breakfast");

    sendCommands("cook Pancakes", "next", "next", "quit");
    runCli();

    assertThat(getOutput()).contains("Finished cooking Pancakes! Enjoy!");
    assertThat(getOutput()).contains("Step 1 of 2");
    assertThat(getOutput()).contains("Step 2 of 2");
  }

  @Test
  void cookMode_ingredientsCommand_displaysFullIngredientList() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");

    sendCommands("cook Pancakes", "ingredients", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Ingredients:");
    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("milk");
    assertThat(getOutput()).contains("salt");
  }

  @Test
  void cookMode_shortIngredientsCommand_displaysFullIngredientList() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");

    sendCommands("cook Pancakes", "i", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Ingredients:");
    assertThat(getOutput()).contains("flour");
  }

  @Test
  void cookMode_quitExitsToMainPrompt() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 3), "Breakfast");

    sendCommands("cook Pancakes", "quit", "help", "quit");
    runCli();

    assertThat(getOutput()).contains("COOKING: Pancakes");
    assertThat(getOutput()).contains("CookYourBooks Commands");
  }

  @Test
  void cookMode_shortQuitExitsToMainPrompt() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 3), "Breakfast");

    sendCommands("cook Pancakes", "q", "quit");
    runCli();

    assertThat(getOutput()).contains("COOKING: Pancakes");
  }

  @Test
  void cookMode_emptyLinesSkipped() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 2), "Breakfast");

    sendCommands("cook Pancakes", "", "  ", "next", "", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 1 of 2");
    assertThat(getOutput()).contains("Step 2 of 2");
  }

  @Test
  void cookMode_displaysStepWithNoIngredientsUsed() throws Exception {
    var recipe = RecipeFixtures.recipeWithSteps("Simple", 2);
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("cook Simple", "next", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("(no ingredients used in this step)");
  }

  @Test
  void cookMode_displaysCookHeaderAndCompactIngredients() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");

    sendCommands("cook Pancakes", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("COOKING: Pancakes");
    assertThat(getOutput()).contains("Serves 4");
    assertThat(getOutput()).contains("Ingredients:");
    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("milk");
  }

  @Test
  void cookMode_singleStepRecipe_nextFinishes() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Quick", 1), "Breakfast");

    sendCommands("cook Quick", "next", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 1 of 1");
    assertThat(getOutput()).contains("Finished cooking Quick! Enjoy!");
  }

  @Test
  void cookMode_stepWithIngredientRefs_displaysUsesWithPreparation() throws Exception {
    setupRecipeInCollection(
        RecipeFixtures.recipeWithStepIngredientRefs("Butter Recipe"), "Breakfast");

    sendCommands("cook \"Butter Recipe\"", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Uses:");
    assertThat(getOutput()).contains("butter");
    assertThat(getOutput()).contains("melted");
  }

  @Test
  void cookMode_unknownCommand_ignoresAndShowsHintsAgain() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 2), "Breakfast");

    sendCommands("cook Pancakes", "foo", "next", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 1 of 2");
    assertThat(getOutput()).contains("[next] [prev] [ingredients] [quit]");
    assertThat(getOutput()).contains("Step 2 of 2");
  }

  @Test
  void cook_recipeWithNoInstructions_displaysError() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithNoInstructions("Empty Recipe"), "Breakfast");

    sendCommands("cook \"Empty Recipe\"", "quit");
    runCli();

    assertThat(getOutput()).contains("Error");
    assertThat(getOutput()).contains("no instructions");
  }

  @Test
  void cookMode_prevAfterNext_resetsFinishedState() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 2), "Breakfast");

    sendCommands("cook Pancakes", "next", "prev", "next", "next", "quit");
    runCli();

    assertThat(getOutput()).contains("Step 1 of 2");
    assertThat(getOutput()).contains("Step 2 of 2");
    assertThat(getOutput()).contains("Step 1 of 2"); // After prev
    assertThat(getOutput()).contains("Finished cooking Pancakes! Enjoy!");
  }
}
