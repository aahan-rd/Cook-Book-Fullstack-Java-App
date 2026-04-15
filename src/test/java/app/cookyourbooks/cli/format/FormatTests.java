package app.cookyourbooks.cli.format;

import static app.cookyourbooks.cli.assertions.OutputAssertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;
import app.cookyourbooks.cli.fixtures.RecipeFixtures;

/** CLI-level tests for formatters via show, cook, scale commands. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class FormatTests extends CliTestBase {

  @Test
  void scale_longIngredientName_truncatesInComparisonTable() throws Exception {
    var recipe =
        RecipeFixtures.recipeWithLongIngredientName(
            "Long Name Recipe", "extraordinarily-long-ingredient-name");
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale \"Long Name Recipe\" 8", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("extraordinarily-long");
    assertThat(getOutput()).contains("Original");
    assertThat(getOutput()).contains("Scaled");
  }

  @Test
  void scale_vagueIngredientNullDescription_displaysToTaste() throws Exception {
    setupRecipeInCollection(
        RecipeFixtures.recipeWithVagueIngredientNullDescription("Vague Recipe"), "Breakfast");

    sendCommands("scale \"Vague Recipe\" 8", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("to taste");
    assertThat(getOutput()).contains("salt");
  }

  @Test
  void cook_singleIngredient_displaysCompactLayout() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSingleIngredient("Single"), "Breakfast");

    sendCommands("cook Single", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("COOKING: Single");
  }

  @Test
  void cook_twoIngredients_displaysTwoColumnLayout() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithTwoIngredients("TwoCol"), "Breakfast");

    sendCommands("cook TwoCol", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("sugar");
  }

  @Test
  void cook_threeIngredients_displaysCompactLayout() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithThreeIngredients("ThreeCol"), "Breakfast");

    sendCommands("cook ThreeCol", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("sugar");
    assertThat(getOutput()).contains("milk");
  }

  @Test
  void cook_stepWithPreparation_displaysIngredientRefWithPrep() throws Exception {
    setupRecipeInCollection(
        RecipeFixtures.recipeWithStepIngredientRefs("Butter Recipe"), "Breakfast");

    sendCommands("cook \"Butter Recipe\"", "quit", "quit");
    runCli();

    assertThat(getOutput()).contains("Uses:");
    assertThat(getOutput()).contains("butter");
    assertThat(getOutput()).contains("melted");
  }

  @Test
  void show_recipeWithPreparation_displaysPreparation() throws Exception {
    setupRecipeInCollection(
        RecipeFixtures.recipeWithPreparedIngredient("Prep Recipe", "butter", "softened"),
        "Breakfast");

    sendCommands("show \"Prep Recipe\"", "quit");
    runCli();

    assertThat(getOutput()).contains("butter");
    assertThat(getOutput()).contains("softened");
  }
}
