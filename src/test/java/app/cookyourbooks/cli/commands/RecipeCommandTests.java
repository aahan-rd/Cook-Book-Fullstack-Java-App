package app.cookyourbooks.cli.commands;

import static app.cookyourbooks.cli.assertions.OutputAssertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;
import app.cookyourbooks.cli.fixtures.RecipeFixtures;

/** CLI tests for recipe commands: show, search, import json, delete. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class RecipeCommandTests extends CliTestBase {

  @Test
  void show_displaysFullRecipeDetails() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("show pancakes", "quit");
    runCli();

    assertThat(getOutput()).contains("Pancakes");
    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("milk");
    assertThat(getOutput()).contains("Mix ingredients");
  }

  @Test
  void show_recipeNotFound_displaysNotFoundError() throws Exception {
    sendCommands("show nonexistent", "quit");
    runCli();

    assertThat(getOutput()).contains("Recipe not found");
    assertThat(getOutput()).contains("nonexistent");
    assertThat(getOutput()).contains("search");
  }

  @Test
  void show_withBlankArg_displaysUsage() throws Exception {
    sendCommands("show", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: show <recipe>");
  }

  @Test
  void search_findsRecipesByIngredient() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Pancakes", "flour"), "Breakfast");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Cake", "flour"), "Desserts");

    sendCommands("search flour", "quit");
    runCli();

    assertThat(getOutput()).contains("Recipes containing 'flour'");
    assertThat(getOutput()).contains("Pancakes");
    assertThat(getOutput()).contains("Cake");
    assertThat(getOutput()).contains("Found 2 recipes");
  }

  @Test
  void search_noMatches_displaysNoRecipesMessage() throws Exception {
    sendCommands("search unicorn", "quit");
    runCli();

    assertThat(getOutput()).contains("No recipes found containing 'unicorn'");
  }

  @Test
  void search_withBlankArg_displaysUsage() throws Exception {
    sendCommands("search", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: search <ingredient>");
  }

  @Test
  void importJson_importsRecipeIntoCollection() throws Exception {
    setupCollection("My Cookbook");
    Path jsonPath = tempDir.resolve("pancakes.json");
    Files.writeString(
        jsonPath,
        """
        {
          "title": "Imported Pancakes",
          "servings": { "amount": 4, "description": "servings" },
          "ingredients": [
            {
              "type": "measured",
              "name": "flour",
              "quantity": { "type": "exact", "amount": 2.0, "unit": "CUP" },
              "preparation": null,
              "notes": null
            }
          ],
          "instructions": [
            { "stepNumber": 1, "text": "Mix", "ingredientRefs": [] }
          ],
          "conversionRules": []
        }
        """);

    sendCommands("import json " + jsonPath.toAbsolutePath() + " \"My Cookbook\"", "quit");
    runCli();

    assertThat(getOutput()).contains("Imported 'Imported Pancakes' into 'My Cookbook'");
  }

  @Test
  void importJson_collectionNotFound_displaysNotFoundError() throws Exception {
    Path jsonPath = tempDir.resolve("pancakes.json");
    Files.writeString(
        jsonPath,
        """
        {
          "title": "Test",
          "servings": { "amount": 1, "description": "serving" },
          "ingredients": [],
          "instructions": [],
          "conversionRules": []
        }
        """);

    sendCommands("import json " + jsonPath.toAbsolutePath() + " Nonexistent", "quit");
    runCli();

    assertThat(getOutput()).contains("Collection not found");
    assertThat(getOutput()).contains("Nonexistent");
  }

  @Test
  void importJson_withTooFewArgs_displaysUsage() throws Exception {
    sendCommands("import json", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: import json <file> <collection>");
  }

  @Test
  void delete_withConfirmation_removesRecipe() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("delete pancakes", "y", "quit");
    runCli();

    assertThat(getOutput()).contains("Deleted recipe 'Pancakes'");
  }

  @Test
  void delete_persistsRemoval_recipeNotInRecipesListing() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("delete pancakes", "y", "recipes Breakfast", "quit");
    runCli();

    assertThat(getOutput()).contains("Deleted recipe 'Pancakes'");
    assertThat(getOutput()).contains("Breakfast (0 recipes)");
  }

  @Test
  void delete_withoutConfirmation_keepsRecipe() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("delete pancakes", "n", "show pancakes", "quit");
    runCli();

    assertThat(getOutput()).contains("Pancakes");
    assertThat(getOutput()).doesNotContain("Deleted recipe");
  }

  @Test
  void delete_recipeNotFound_displaysNotFoundError() throws Exception {
    sendCommands("delete nonexistent", "quit");
    runCli();

    assertThat(getOutput()).contains("Recipe not found");
    assertThat(getOutput()).contains("nonexistent");
  }

  @Test
  void delete_withBlankArg_displaysUsage() throws Exception {
    sendCommands("delete", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: delete <recipe>");
  }

  @Test
  void show_ambiguousMatch_displaysAmbiguousFormat() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cake", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cookies", "flour"), "B");

    sendCommands("show chocolate", "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
    assertThat(getOutput()).contains("Chocolate Cake");
    assertThat(getOutput()).contains("Chocolate Cookies");
    assertThat(getOutput()).contains("Please specify");
  }

  @Test
  void show_ambiguousMatch_sameTitle_displaysShortIdHint() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Same Name", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Same Name", "sugar"), "B");

    sendCommands("show same", "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
    assertThat(getOutput()).contains("Same Name");
    assertThat(getOutput()).contains("Please specify a short ID");
  }

  @Test
  void show_recipeWithoutServings_omitsServesLine() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithoutServings("No Servings"), "Breakfast");

    sendCommands("show \"No Servings\"", "quit");
    runCli();

    assertThat(getOutput()).contains("No Servings");
    assertThat(getOutput()).contains("Ingredients:");
    assertThat(getOutput()).doesNotContain("Serves");
  }

  @Test
  void delete_ambiguousMatch_displaysAmbiguousFormat() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cake", "flour"), "A");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Chocolate Cookies", "flour"), "B");

    sendCommands("delete chocolate", "quit");
    runCli();

    assertThat(getOutput()).contains("Multiple recipes match");
    assertThat(getOutput()).contains("Chocolate Cake");
  }

  @Test
  void delete_withNonYResponse_keepsRecipe() throws Exception {
    var recipe = RecipeFixtures.pancakes();
    setupRecipeInCollection(recipe, "Breakfast");

    sendCommands("delete pancakes", "x", "show pancakes", "quit");
    runCli();

    assertThat(getOutput()).contains("Pancakes");
    assertThat(getOutput()).doesNotContain("Deleted recipe");
  }

  @Test
  void search_withMultiWordIngredient_findsRecipes() throws Exception {
    setupRecipeInCollection(
        RecipeFixtures.recipeWithIngredient("Pancakes", "baking powder"), "Breakfast");

    sendCommands("search \"baking powder\"", "quit");
    runCli();

    assertThat(getOutput()).contains("Recipes containing 'baking powder'");
    assertThat(getOutput()).contains("Pancakes");
  }

  @Test
  void search_displaysCollectionNameForEachRecipe() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Pancakes", "flour"), "Breakfast");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Cake", "flour"), "Desserts");

    sendCommands("search flour", "quit");
    runCli();

    assertThat(getOutput()).contains("Breakfast");
    assertThat(getOutput()).contains("Desserts");
  }
}
