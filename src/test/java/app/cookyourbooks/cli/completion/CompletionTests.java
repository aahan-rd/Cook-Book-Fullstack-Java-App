package app.cookyourbooks.cli.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;
import app.cookyourbooks.cli.fixtures.RecipeFixtures;

/** CLI-level tests for tab completion via getCompletionValues. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class CompletionTests extends CliTestBase {

  @Test
  void commandCompleter_emptyLine_offersAllCommands() throws Exception {
    var completions = getCompletionValues("");
    assertThat(completions).contains("show", "scale", "search", "collections", "help", "quit");
  }

  @Test
  void commandCompleter_prefixSh_offersShowAndShoppingList() throws Exception {
    var completions = getCompletionValues("sh");
    assertThat(completions).contains("show", "shopping-list");
  }

  @Test
  void commandCompleter_prefixSh_offersShowAndShoppingList_caseInsensitive() throws Exception {
    var completions = getCompletionValues("SH");
    assertThat(completions).contains("show", "shopping-list");
  }

  @Test
  void commandCompleter_prefixCol_offersCollectionCreateAndCollections() throws Exception {
    var completions = getCompletionValues("col");
    assertThat(completions).contains("collection create", "collections");
  }

  @Test
  void commandCompleter_prefixCo_offersConvertCookCollectionCollections() throws Exception {
    var completions = getCompletionValues("co");
    assertThat(completions).contains("convert", "cook", "collection create", "collections");
  }

  @Test
  void commandCompleter_wordIndex0_offersCommands() throws Exception {
    var completions = getCompletionValues("sho");
    assertThat(completions).contains("show", "shopping-list");
  }

  @Test
  void recipeTitleCompleter_showRecipe_offersRecipeTitles() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("show pan");
    assertThat(completions).contains("Pancakes");
  }

  @Test
  void recipeTitleCompleter_showRecipeWithSpaces_offersQuotedTitle() throws Exception {
    setupRecipeInCollection(
        RecipeFixtures.recipeWithIngredient("Chocolate Chip Cookies", "flour"), "Desserts");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("show cho");
    assertThat(completions).contains("\"Chocolate Chip Cookies\"");
  }

  @Test
  void recipeTitleCompleter_showRecipe_offersShortId() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("show ");
    assertThat(completions).contains("Pancakes");
  }

  @Test
  void recipeTitleCompleter_scaleCommand_offersRecipeTitles() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("scale pan");
    assertThat(completions).contains("Pancakes");
  }

  @Test
  void recipeTitleCompleter_convertCommand_offersRecipeAtWord1() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("convert pan");
    assertThat(completions).contains("Pancakes");
  }

  @Test
  void recipeTitleCompleter_deleteCommand_offersRecipeTitles() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("delete pan");
    assertThat(completions).contains("Pancakes");
  }

  @Test
  void recipeTitleCompleter_cookCommand_offersRecipeTitles() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("cook pan");
    assertThat(completions).contains("Pancakes");
  }

  @Test
  void collectionNameCompleter_recipesCommand_offersCollectionNames() throws Exception {
    setupCollection("Holiday Favorites");
    setupCollection("Happy Meals");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("recipes hol");
    assertThat(completions).contains("\"Holiday Favorites\"");
  }

  @Test
  void collectionNameCompleter_recipesCommand_partialMatch_offersQuotedTitle() throws Exception {
    setupCollection("Holiday Favorites");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("recipes \"Holiday F");
    assertThat(completions).contains("\"Holiday Favorites\"");
  }

  @Test
  void unitCompleter_convertCommand_offersUnitNames() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("convert Pancakes g");
    assertThat(completions).contains("g");
  }

  @Test
  void unitCompleter_convertCommand_offersCupForCu() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("convert Pancakes cu");
    assertThat(completions).contains("cup");
  }

  @Test
  void cookModeCompleter_whenInCookMode_offersNextPrevIngredientsQuit() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithSteps("Pancakes", 2), "Breakfast");
    sendCommands("quit");
    runCli();

    setCookMode(true);
    var completions = getCompletionValues("n");
    assertThat(completions).contains("next");
  }

  @Test
  void cookModeCompleter_whenInCookMode_offersPrev() throws Exception {
    setCookMode(true);
    var completions = getCompletionValues("p");
    assertThat(completions).contains("prev");
  }

  @Test
  void cookModeCompleter_whenInCookMode_offersIngredients() throws Exception {
    setCookMode(true);
    var completions = getCompletionValues("i");
    assertThat(completions).contains("ingredients");
  }

  @Test
  void cookModeCompleter_whenInCookMode_offersQuit() throws Exception {
    setCookMode(true);
    var completions = getCompletionValues("q");
    assertThat(completions).contains("quit");
  }

  @Test
  void cookModeCompleter_whenNotInCookMode_returnsEmpty() throws Exception {
    setCookMode(false);
    var completions = getCompletionValues("n");
    assertThat(completions).doesNotContain("next");
  }

  @Test
  void cybCompleter_importJson_offersCollectionsAtWord3() throws Exception {
    setupCollection("My Cookbook");
    Path jsonPath = tempDir.resolve("test.json");
    Files.writeString(jsonPath, "{}");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("import json " + jsonPath.toAbsolutePath() + " my");
    assertThat(completions).contains("\"My Cookbook\"");
  }

  @Test
  void cybCompleter_shoppingList_offersRecipesForAllArgs() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Breakfast");
    setupRecipeInCollection(RecipeFixtures.recipeWithIngredient("Pie", "flour"), "Desserts");
    sendCommands("quit");
    runCli();

    var completions = getCompletionValues("shopping-list ");
    assertThat(completions).contains("Pancakes", "Pie");
  }

  @Test
  void cybCompleter_unknownCommand_offersNoCompletion() throws Exception {
    var completions = getCompletionValues("xyz foo");
    assertThat(completions).isEmpty();
  }
}
