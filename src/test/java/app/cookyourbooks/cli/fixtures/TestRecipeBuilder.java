package app.cookyourbooks.cli.fixtures;

import java.util.ArrayList;
import java.util.List;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.IngredientRef;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.RangeQuantity;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;

/** Builder for test recipes. */
public final class TestRecipeBuilder {

  private final String title;
  private Servings servings = new Servings(4);
  private final List<app.cookyourbooks.model.Ingredient> ingredients = new ArrayList<>();
  private final List<Instruction> instructions = new ArrayList<>();

  private TestRecipeBuilder(String title) {
    this.title = title;
  }

  public static TestRecipeBuilder recipe(String title) {
    return new TestRecipeBuilder(title);
  }

  public TestRecipeBuilder serves(int amount) {
    this.servings = new Servings(amount);
    return this;
  }

  public TestRecipeBuilder withIngredient(String name, double amount, Unit unit) {
    ingredients.add(new MeasuredIngredient(name, new ExactQuantity(amount, unit), null, null));
    return this;
  }

  public TestRecipeBuilder withIngredient(
      String name, double amount, Unit unit, String preparation) {
    ingredients.add(
        new MeasuredIngredient(name, new ExactQuantity(amount, unit), preparation, null));
    return this;
  }

  public TestRecipeBuilder withRangeQuantityIngredient(
      String name, double min, double max, Unit unit) {
    ingredients.add(new MeasuredIngredient(name, new RangeQuantity(min, max, unit), null, null));
    return this;
  }

  public TestRecipeBuilder withVagueIngredient(String name, String description) {
    ingredients.add(new VagueIngredient(name, description, null, null));
    return this;
  }

  public TestRecipeBuilder withStep(String text) {
    instructions.add(new Instruction(instructions.size() + 1, text, List.of()));
    return this;
  }

  public TestRecipeBuilder withStepWithRefs(String text, List<IngredientRef> refs) {
    instructions.add(new Instruction(instructions.size() + 1, text, refs));
    return this;
  }

  public Recipe build() {
    return new Recipe(null, title, servings, List.copyOf(ingredients), instructions, List.of());
  }
}
