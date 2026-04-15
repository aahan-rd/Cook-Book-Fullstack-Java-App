package app.cookyourbooks.gui;

import java.util.Arrays;
import java.util.List;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.PersonalCollectionImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;

/**
 * Shared test fixture helpers for all four GUI features.
 *
 * <p>Lives in src/test so it is not included in the production build. All four teammates import
 * from this class — coordinate changes as a team.
 *
 * <p>Usage example:
 *
 * <pre>
 *   Recipe r = RecipeFixtures.makeRecipe("r1", "Pancakes");
 *   RecipeCollection c = RecipeFixtures.makeCollection("c1", "Breakfast", r);
 * </pre>
 */
public final class RecipeFixtures {

  private RecipeFixtures() {}

  /**
   * Creates a minimal recipe with the given ID and title. Has no servings, no ingredients, no
   * instructions.
   *
   * @param id the recipe ID
   * @param title the recipe title
   * @return a valid minimal Recipe
   */
  public static Recipe makeRecipe(String id, String title) {
    return new Recipe(id, title, null, List.of(), List.of(), List.of());
  }

  /**
   * Creates a recipe with a specific serving count.
   *
   * @param id the recipe ID
   * @param title the recipe title
   * @param servings number of servings
   * @return a Recipe with servings set
   */
  public static Recipe makeRecipeWithServings(String id, String title, int servings) {
    return new Recipe(id, title, new Servings(servings), List.of(), List.of(), List.of());
  }

  /**
   * Creates a recipe with the given ingredients.
   *
   * @param id the recipe ID
   * @param title the recipe title
   * @param ingredients zero or more ingredients
   * @return a Recipe with those ingredients
   */
  public static Recipe makeRecipeWithIngredients(
      String id, String title, Ingredient... ingredients) {
    return new Recipe(id, title, new Servings(4), Arrays.asList(ingredients), List.of(), List.of());
  }

  /**
   * Creates a MeasuredIngredient with a cup quantity — useful for scaling and conversion tests.
   *
   * @param name ingredient name
   * @param amount amount in cups
   * @return a MeasuredIngredient
   */
  public static MeasuredIngredient measuredCups(String name, double amount) {
    return new MeasuredIngredient(name, new ExactQuantity(amount, Unit.CUP), null, null);
  }

  /**
   * Creates a VagueIngredient — useful for testing that vague ingredients are left unchanged during
   * scaling or conversion.
   *
   * @param name ingredient name
   * @param description e.g. "to taste"
   * @return a VagueIngredient
   */
  public static VagueIngredient vague(String name, String description) {
    return new VagueIngredient(name, description, null, null);
  }

  /**
   * Creates a personal collection containing the given recipes.
   *
   * @param id the collection ID
   * @param title the collection title
   * @param recipes zero or more recipes to include
   * @return a PersonalCollection
   */
  public static RecipeCollection makeCollection(String id, String title, Recipe... recipes) {
    return PersonalCollectionImpl.builder()
        .id(id)
        .title(title)
        .recipes(Arrays.asList(recipes))
        .build();
  }

  /**
   * Creates an empty personal collection with no recipes.
   *
   * @param id the collection ID
   * @param title the collection title
   * @return an empty PersonalCollection
   */
  public static RecipeCollection makeEmptyCollection(String id, String title) {
    return PersonalCollectionImpl.builder().id(id).title(title).build();
  }
}
