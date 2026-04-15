package app.cookyourbooks.cli.fixtures;

import java.util.List;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;

/** Factory for common test recipes. */
public final class RecipeFixtures {

  private RecipeFixtures() {}

  public static Recipe pancakes() {
    return TestRecipeBuilder.recipe("Pancakes")
        .serves(4)
        .withIngredient("flour", 2, Unit.CUP)
        .withIngredient("milk", 1, Unit.CUP)
        .withVagueIngredient("salt", "to taste")
        .withStep("Mix ingredients")
        .withStep("Cook on griddle")
        .build();
  }

  public static Recipe recipeWithSteps(String title, int stepCount) {
    var builder = TestRecipeBuilder.recipe(title).serves(4);
    builder.withIngredient("flour", 2, Unit.CUP);
    for (int i = 1; i <= stepCount; i++) {
      builder.withStep("Step " + i + " instruction");
    }
    return builder.build();
  }

  public static Recipe recipeWithIngredient(String title, String ingredientName) {
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withIngredient(ingredientName, 1, Unit.CUP)
        .withStep("Mix")
        .build();
  }

  public static Recipe recipeWithoutServings(String title) {
    List<app.cookyourbooks.model.Ingredient> ingredients =
        List.of(
            new MeasuredIngredient("flour", new ExactQuantity(2, Unit.CUP), null, null),
            new MeasuredIngredient("water", new ExactQuantity(1, Unit.CUP), null, null));
    var instructions =
        List.of(new Instruction(1, "Mix", List.of()), new Instruction(2, "Bake", List.of()));
    return new Recipe(null, title, null, ingredients, instructions, List.of());
  }

  public static Recipe recipeWithPreparedIngredient(
      String title, String ingredientName, String preparation) {
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withIngredient(ingredientName, 1, Unit.CUP, preparation)
        .withStep("Mix")
        .build();
  }

  public static Recipe recipeWithRangeQuantity(String title) {
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withRangeQuantityIngredient("flour", 2, 3, Unit.CUP)
        .withIngredient("sugar", 1, Unit.CUP)
        .withStep("Mix")
        .build();
  }

  public static Recipe recipeWithNoInstructions(String title) {
    List<app.cookyourbooks.model.Ingredient> ingredients =
        List.of(new MeasuredIngredient("flour", new ExactQuantity(2, Unit.CUP), null, null));
    return new Recipe(
        null, title, new app.cookyourbooks.model.Servings(4), ingredients, List.of(), List.of());
  }

  public static Recipe recipeWithStepIngredientRefs(String title) {
    var butter = new MeasuredIngredient("butter", new ExactQuantity(1, Unit.CUP), "melted", null);
    var ref = new app.cookyourbooks.model.IngredientRef(butter, new ExactQuantity(1, Unit.CUP));
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withIngredient("flour", 2, Unit.CUP)
        .withStepWithRefs("Use the butter", List.of(ref))
        .build();
  }

  /** Recipe with an ingredient name longer than 24 chars for ComparisonFormatter truncate tests. */
  public static Recipe recipeWithLongIngredientName(String title, String longIngredientName) {
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withIngredient(longIngredientName, 1, Unit.CUP)
        .withStep("Mix")
        .build();
  }

  /** Recipe with single ingredient for formatIngredientsCompact math tests. */
  public static Recipe recipeWithSingleIngredient(String title) {
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withIngredient("flour", 2, Unit.CUP)
        .withStep("Mix")
        .build();
  }

  /** Recipe with two ingredients for formatIngredientsCompact two-column layout. */
  public static Recipe recipeWithTwoIngredients(String title) {
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withIngredient("flour", 2, Unit.CUP)
        .withIngredient("sugar", 1, Unit.CUP)
        .withStep("Mix")
        .build();
  }

  /** Recipe with three ingredients for formatIngredientsCompact odd-count layout. */
  public static Recipe recipeWithThreeIngredients(String title) {
    return TestRecipeBuilder.recipe(title)
        .serves(4)
        .withIngredient("flour", 2, Unit.CUP)
        .withIngredient("sugar", 1, Unit.CUP)
        .withIngredient("milk", 1, Unit.CUP)
        .withStep("Mix")
        .build();
  }

  /** VagueIngredient with null description (formatIngredientQuantity fallback to "to taste"). */
  public static Recipe recipeWithVagueIngredientNullDescription(String title) {
    List<app.cookyourbooks.model.Ingredient> ingredients =
        List.of(
            new app.cookyourbooks.model.VagueIngredient("salt", null, null, null),
            new MeasuredIngredient("flour", new ExactQuantity(2, Unit.CUP), null, null));
    var instructions =
        List.of(new Instruction(1, "Mix", List.of()), new Instruction(2, "Bake", List.of()));
    return new Recipe(
        null, title, new app.cookyourbooks.model.Servings(4), ingredients, instructions, List.of());
  }
}
