package app.cookyourbooks.cli.commands;

import static app.cookyourbooks.cli.assertions.OutputAssertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;
import app.cookyourbooks.cli.fixtures.RecipeFixtures;

/** Tests for library commands: collections, collection create, recipes, conversions. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class LibraryCommandTests extends CliTestBase {

  @Test
  void collections_whenEmpty_displaysHelpfulMessage() throws Exception {
    sendCommands("collections", "quit");
    runCli();

    assertThat(getOutput()).contains("No collections");
    assertThat(getOutput()).contains("collection create");
  }

  @Test
  void collections_listsAllCollectionsWithTypeAndCount() throws Exception {
    setupCollection("Holiday Favorites");
    setupCollection("Joy of Cooking");

    sendCommands("collections", "quit");
    runCli();

    assertThat(getOutput()).contains("Collections:");
    assertThat(getOutput()).contains("Holiday Favorites");
    assertThat(getOutput()).contains("Joy of Cooking");
    assertThat(getOutput()).contains("[Personal]");
  }

  @Test
  void collectionCreate_createsPersonalCollection_displaysSuccessMessage() throws Exception {
    sendCommands("collection create \"My Recipes\"", "collections", "quit");
    runCli();

    assertThat(getOutput()).contains("Created personal collection 'My Recipes'.");
    assertThat(getOutput()).contains("My Recipes");
  }

  @Test
  void collectionCreate_withNoArgs_displaysUsage() throws Exception {
    sendCommands("collection create", "quit");
    runCli();

    assertThat(getOutput()).contains("Usage: collection create <name>");
  }

  @Test
  void collectionCreate_withBlankName_displaysUsageError() throws Exception {
    sendCommands("collection create \"\"", "quit");
    runCli();

    assertThat(getOutput()).contains("Please provide a collection name.");
  }

  @Test
  void collectionCreate_withWhitespaceOnly_displaysError() throws Exception {
    sendCommands("collection create \"   \"", "quit");
    runCli();

    assertThat(getOutput()).contains("Please provide a collection name.");
  }

  @Test
  void recipes_listsRecipesInCollection_withServingInfo() throws Exception {
    setupRecipeInCollection(RecipeFixtures.pancakes(), "Holiday Favorites");

    sendCommands("recipes \"Holiday Favorites\"", "quit");
    runCli();

    assertThat(getOutput()).contains("Pancakes");
    assertThat(getOutput()).contains("Serves");
  }

  @Test
  void recipes_emptyCollection_displaysZeroRecipes() throws Exception {
    setupCollection("Empty Collection");

    sendCommands("recipes \"Empty Collection\"", "quit");
    runCli();

    assertThat(getOutput()).contains("Empty Collection");
    assertThat(getOutput()).contains("0 recipes");
  }

  @Test
  void recipes_recipeWithoutServings_displaysNoServings() throws Exception {
    setupRecipeInCollection(RecipeFixtures.recipeWithoutServings("Bare Recipe"), "Breakfast");

    sendCommands("recipes Breakfast", "quit");
    runCli();

    assertThat(getOutput()).contains("Bare Recipe");
    assertThat(getOutput()).contains("No servings");
  }

  @Test
  void recipes_collectionNotFound_displaysNotFoundError() throws Exception {
    sendCommands("recipes \"Unknown Collection\"", "quit");
    runCli();

    assertThat(getOutput()).contains("Collection not found");
    assertThat(getOutput()).contains("Unknown Collection");
  }

  @Test
  void conversions_whenEmpty_displaysHelpfulMessage() throws Exception {
    sendCommands("conversions", "quit");
    runCli();

    assertThat(getOutput()).contains("No house conversions defined");
    assertThat(getOutput()).contains("conversion add");
  }

  @Test
  void conversions_listsHouseConversionRules() throws Exception {
    sendCommands("conversion add", "1", "cup", "flour", "120", "g", "conversions", "quit");
    runCli();

    assertThat(getOutput()).contains("House Conversions");
    assertThat(getOutput()).contains("cup");
    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("120");
  }

  @Test
  void conversionAdd_interactivelyAddsRule_displaysConfirmation() throws Exception {
    sendCommands("conversion add", "1", "cup", "flour", "120", "g", "quit");
    runCli();

    assertThat(getOutput()).contains("Added:");
    assertThat(getOutput()).contains("cup");
    assertThat(getOutput()).contains("flour");
    assertThat(getOutput()).contains("120");
  }

  @Test
  void conversionRemove_removesRule_displaysConfirmation() throws Exception {
    sendCommands(
        "conversion add",
        "1",
        "tbsp",
        "any",
        "15",
        "ml",
        "conversion remove \"tbsp any\"",
        "conversions",
        "quit");
    runCli();

    assertThat(getOutput()).contains("Removed conversion:");
    assertThat(getOutput()).contains("No house conversions defined");
  }

  @Test
  void conversionRemove_notFound_displaysNotFoundError() throws Exception {
    sendCommands("conversion remove \"stick butter\"", "quit");
    runCli();

    assertThat(getOutput()).contains("No conversion found");
    assertThat(getOutput()).contains("stick butter");
  }

  @Test
  void conversionAdd_invalidAmount_displaysError() throws Exception {
    sendCommands("conversion add", "abc", "cup", "flour", "120", "g", "quit");
    runCli();

    assertThat(getOutput()).contains("Invalid amount");
    assertThat(getOutput()).contains("Please enter a number");
  }

  @Test
  void conversionAdd_duplicateRule_displaysError() throws Exception {
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
        "flour",
        "125",
        "g",
        "quit");
    runCli();

    assertThat(getOutput()).contains("already exists");
    assertThat(getOutput()).contains("cup flour");
  }

  @Test
  void conversionAdd_invalidToAmount_displaysError() throws Exception {
    sendCommands("conversion add", "1", "cup", "flour", "xyz", "g", "quit");
    runCli();

    assertThat(getOutput()).contains("Invalid amount");
    assertThat(getOutput()).contains("Please enter a number");
  }

  @Test
  void conversionAdd_withAnyIngredient_addsUniversalConversion() throws Exception {
    sendCommands("conversion add", "1", "tbsp", "any", "15", "ml", "conversions", "quit");
    runCli();

    assertThat(getOutput()).contains("Added:");
    assertThat(getOutput()).contains("tbsp");
    assertThat(getOutput()).contains("any");
    assertThat(getOutput()).contains("House Conversions");
  }
}
