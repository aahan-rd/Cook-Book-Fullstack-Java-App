package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.AmbiguousMatchFormatter;
import app.cookyourbooks.cli.format.ComparisonFormatter;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;

/** Scales a recipe to target servings with preview and save prompt. */
public final class ScaleCommand extends AbstractCommand {

  private final ComparisonFormatter formatter = new ComparisonFormatter();

  public ScaleCommand() {
    super(
        "scale",
        "Scale a recipe",
        "scale <recipe> <servings> - Scale recipe to target servings",
        "Tools");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.size() < 2) {
      context.println("Usage: scale <recipe> <servings>");
      return;
    }
    String query = args.get(0);
    int targetServings;
    try {
      targetServings = Integer.parseInt(args.get(1));
    } catch (NumberFormatException e) {
      context.println("Invalid servings. Please provide a positive number.");
      return;
    }
    var matches = context.librarianService().resolveRecipes(query);
    if (matches.isEmpty()) {
      context.println("Recipe not found: '" + query + "'. Use 'search' to find recipes.");
      return;
    }
    if (matches.size() > 1) {
      AmbiguousMatchFormatter.formatAmbiguousRecipes(
          context.out(), query, matches, r -> findCollectionForRecipe(context, r).getTitle());
      return;
    }
    Recipe recipe = matches.get(0);
    try {
      var result = context.transformerService().scale(recipe, targetServings);
      context.println(
          formatter.formatScale(
              result.original(), result.scaled(), targetServings, result.factor()));
      String response = context.readLine("Save scaled recipe? (y/n): ");
      if (response != null && response.trim().equalsIgnoreCase("y")) {
        RecipeCollection coll = findCollectionForRecipe(context, recipe);
        String newTitle = result.scaled().getTitle() + " (scaled to " + targetServings + ")";
        Recipe toSave =
            new app.cookyourbooks.model.Recipe(
                null,
                newTitle,
                result.scaled().getServings(),
                result.scaled().getIngredients(),
                result.scaled().getInstructions(),
                result.scaled().getConversionRules());
        context.librarianService().saveRecipe(toSave, coll.getId());
        context.println("Saved scaled recipe '" + newTitle + "'.");
      } else {
        context.println("Scaling discarded.");
      }
    } catch (IllegalArgumentException e) {
      context.println(java.util.Objects.requireNonNullElse(e.getMessage(), "Error"));
    }
  }

  private RecipeCollection findCollectionForRecipe(CliContext context, Recipe r) {
    for (var c : context.librarianService().listCollections()) {
      if (c.containsRecipe(r.getId())) {
        return c;
      }
    }
    throw new IllegalStateException("Recipe not in any collection");
  }
}
