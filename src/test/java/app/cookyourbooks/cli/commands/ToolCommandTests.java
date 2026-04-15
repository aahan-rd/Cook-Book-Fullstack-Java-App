package app.cookyourbooks.cli.commands;

import static app.cookyourbooks.cli.assertions.OutputAssertions.assertThat;

import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;
import app.cookyourbooks.cli.fixtures.RecipeFixtures;

/** CLI tests for tool commands: scale, convert, shopping-list, export. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ToolCommandTests extends CliTestBase {

  @Test
  void scale_displaysComparisonAndSavesWhenConfirmed() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 8", "y", "quit");
    runCli();

    assertThat(getOutput()).contains("Pancakes");
    assertThat(getOutput()).contains("8");
    assertThat(getOutput()).contains("Saved scaled recipe");
  }

  @Test
  void scale_persistsRecipeWhenConfirmed_appearsInRecipesListing() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 8", "y", "recipes Breakfast", "quit");
    runCli();

    assertThat(getOutput()).contains("Saved scaled recipe");
    assertThat(getOutput()).contains("scaled to 8");
    assertThat(getOutput()).contains("Breakfast");
  }

  @Test
  void scale_displaysComparisonAndDiscardsWhenDeclined() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 8", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("Scaling discarded");
  }

  @Test
  void scale_recipeNotFound_displaysNotFoundError() throws Exception {
    sendCommands("scale nonexistent 4", "quit");
    runCli();

    assertThat(getOutput()).contains("Recipe not found");
    assertThat(getOutput()).contains("nonexistent");
  }

  @Test
  void scale_invalidServings_displaysError() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes abc", "quit");
    runCli();

    assertThat(getOutput()).contains("Invalid servings");
  }

  @Test
  void scale_withTooFewArgs_displaysUsage() throws Exception {
    sendCommands("scale pancakes", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: scale <recipe> <servings>");
  }

  @Test
  void convert_displaysComparisonAndSavesWhenConfirmed() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("convert pancakes ml", "y", "quit");
    runCli();

    assertThat(getOutput()).contains("Pancakes");
    assertThat(getOutput()).contains("Saved converted recipe");
  }

  @Test
  void convert_persistsRecipeWhenConfirmed_appearsInRecipesListing() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("convert pancakes ml", "y", "recipes Breakfast", "quit");
    runCli();

    assertThat(getOutput()).contains("Saved converted recipe");
    assertThat(getOutput()).contains("converted to");
    assertThat(getOutput()).contains("Breakfast");
  }

  @Test
  void convert_displaysComparisonAndDiscardsWhenDeclined() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("convert pancakes milliliter", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("Conversion discarded");
  }

  @Test
  void convert_unknownUnit_displaysError() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("convert pancakes foobar", "quit");
    runCli();

    assertThat(getOutput()).contains("Unknown unit");
    assertThat(getOutput()).contains("foobar");
  }

  @Test
  void convert_withTooFewArgs_displaysUsage() throws Exception {
    sendCommands("convert pancakes", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: convert <recipe> <unit>");
  }

  @Test
  void shoppingList_generatesAggregatedList() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Brownie", "flour"), "Desserts");

    sendCommands("shopping-list Pancakes Brownie", "quit");
    runCli();

    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("milk");
  }

  @Test
  void shoppingList_recipeNotFound_displaysNotFoundError() throws Exception {
    sendCommands("shopping-list nonexistent", "quit");
    runCli();

    assertThat(getOutput()).contains("Recipe not found");
    assertThat(getOutput()).contains("nonexistent");
  }

  @Test
  void shoppingList_withNoArgs_displaysUsage() throws Exception {
    sendCommands("shopping-list", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: shopping-list <recipe1> [recipe2] ...");
  }

  @Test
  void export_writesMarkdownFile() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");
    var outPath = tempDir.resolve("pancakes.md");

    sendCommands("export pancakes " + outPath.toAbsolutePath(), "quit");
    runCli();

    assertThat(getOutput()).contains("Exported 'Pancakes'");
    Assertions.assertThat(outPath).exists();
    Assertions.assertThat(Files.readString(outPath)).contains("Pancakes");
  }

  @Test
  void export_recipeNotFound_displaysNotFoundError() throws Exception {
    var outPath = tempDir.resolve("out.md");

    sendCommands("export nonexistent " + outPath.toAbsolutePath(), "quit");
    runCli();

    assertThat(getOutput()).contains("Recipe not found");
  }

  @Test
  void export_withTooFewArgs_displaysUsage() throws Exception {
    sendCommands("export pancakes", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: export <recipe> <file>");
  }

  @Test
  void scale_ambiguousMatch_displaysAmbiguousFormat() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cake", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cookies", "flour"), "B");

    sendCommands("scale chocolate 8", "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
  }

  @Test
  void convert_ambiguousMatch_displaysAmbiguousFormat() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cake", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cookies", "flour"), "B");

    sendCommands("convert chocolate gram", "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
  }

  @Test
  void scale_recipeWithNoServings_displaysError() throws Exception {
    var recipe = RecipeFixtures.recipeWithoutServings("No Servings Recipe");
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale \"No Servings Recipe\" 8", "quit");
    runCli();

    assertThat(getOutput()).contains("no serving information");
    assertThat(getOutput()).contains("No Servings Recipe");
  }

  @Test
  void scale_savePromptNullResponse_discards() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 8", "", "quit");
    runCli();

    assertThat(getOutput()).contains("Scaling discarded");
  }

  @Test
  void convert_unsupportedConversion_displaysError() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("convert pancakes whole", "quit");
    runCli();

    assertThat(getOutput()).contains("Cannot convert");
  }

  @Test
  void convert_appliesHouseConversionRules() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    // Add house conversions for all measured ingredients in pancakes:
    // - 2 cups flour, 1 cup milk (vague salt is skipped)
    // Interactive prompts: from amount, from unit, ingredient, to amount, to unit
    sendCommands(
        "conversion add",
        "1",
        "cup",
        "flour",
        "120",
        "g",
        "conversion add",
        "1",
        "cup",
        "milk",
        "245",
        "g",
        "convert pancakes g",
        "n",
        "quit");
    runCli();

    // 2 cups flour should convert to 240g using house rule
    assertThat(getOutput()).contains("240");
    assertThat(getOutput()).contains("flour");
  }

  @Test
  void cook_recipeNotFound_displaysNotFoundError() throws Exception {
    sendCommands("cook nonexistent", "quit");
    runCli();

    assertThat(getOutput()).contains("Recipe not found");
    assertThat(getOutput()).contains("nonexistent");
  }

  @Test
  void cook_withBlankArg_displaysUsage() throws Exception {
    sendCommands("cook", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: cook <recipe>");
  }

  @Test
  void cook_ambiguousMatch_displaysAmbiguousFormat() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cake", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cookies", "flour"), "B");

    sendCommands("cook chocolate", "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
    assertThat(getOutput()).contains("Chocolate Cake");
  }

  @Test
  void shoppingList_ambiguousMatch_displaysAmbiguousFormat() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cake", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cookies", "flour"), "B");

    sendCommands("shopping-list chocolate", "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
  }

  @Test
  void shoppingList_skipsBlankRecipeNames() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");

    sendCommands("shopping-list Pancakes  ", "quit");
    runCli();

    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("milk");
  }

  @Test
  void export_ambiguousMatch_displaysAmbiguousFormat() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cake", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cookies", "flour"), "B");
    var outPath = tempDir.resolve("out.md");

    sendCommands("export chocolate " + outPath.toAbsolutePath(), "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
    assertThat(getOutput()).doesNotContain("Exported");
  }

  @Test
  void scale_displaysFactorInOutput() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 8", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("2.0x");
  }

  @Test
  void scale_2xFactor_producesDoubledQuantities() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 8", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("2 cups");
    assertThat(getOutput()).contains("4 cups");
    assertThat(getOutput()).contains("1 cup");
    assertThat(getOutput()).contains("2 cups");
  }

  @Test
  void scale_halfFactor_producesHalvedQuantities() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 2", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("0.5x");
    assertThat(getOutput()).contains("1 cup");
  }

  @Test
  void scale_sameServings_1xFactor() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 4", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("1.0x");
  }

  @Test
  void scale_vagueIngredient_displaysToTaste() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 8", "n", "quit");
    runCli();

    assertThat(getOutput()).contains("to taste");
  }

  @Test
  void shoppingList_aggregatesRangeQuantities() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithRangeQuantity("Range Recipe"), "Breakfast");
    setupRecipeInCollection(RecipeFixtures.recipeWithRangeQuantity("Another Range"), "Desserts");

    sendCommands("shopping-list \"Range Recipe\" \"Another Range\"", "quit");
    runCli();

    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("sugar");
  }

  @Test
  void shoppingList_deduplicatesVagueIngredients() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Brownies", "flour"), "B");

    sendCommands("shopping-list Pancakes Brownies", "quit");
    runCli();

    assertThat(getOutput()).contains("Also needed");
    assertThat(getOutput()).contains("salt");
  }

  @Test
  void scale_zeroServings_displaysError() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes 0", "quit");
    runCli();

    assertThat(getOutput()).contains("Invalid servings");
  }

  @Test
  void scale_negativeServings_displaysError() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("scale pancakes -4", "quit");
    runCli();

    assertThat(getOutput()).contains("Invalid servings");
  }
}
