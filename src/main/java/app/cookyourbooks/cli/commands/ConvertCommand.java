package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.AmbiguousMatchFormatter;
import app.cookyourbooks.cli.format.ComparisonFormatter;
import app.cookyourbooks.exception.UnsupportedConversionException;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.Unit;

/** Converts recipe units with preview and save prompt. */
public final class ConvertCommand extends AbstractCommand {

  private final ComparisonFormatter formatter = new ComparisonFormatter();

  public ConvertCommand() {
    super(
        "convert",
        "Convert recipe units",
        "convert <recipe> <unit> - Convert recipe to target unit",
        "Tools");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.size() < 2) {
      context.println("Usage: convert <recipe> <unit>");
      return;
    }
    String query = args.get(0);
    Unit targetUnit;
    try {
      targetUnit = Unit.parse(args.get(1));
    } catch (IllegalArgumentException e) {
      context.println(
          "Unknown unit: '"
              + args.get(1)
              + "'. Valid units include: gram, cup, tsp, tbsp, oz, lb, ml, l.");
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
      var result = context.transformerService().convert(recipe, targetUnit);
      context.println(
          formatter.formatConvert(
              result.original(), result.converted(), targetUnit.getAbbreviation()));
      String response = context.readLine("Save converted recipe? (y/n): ");
      if (response != null && response.trim().equalsIgnoreCase("y")) {
        RecipeCollection coll = findCollectionForRecipe(context, recipe);
        String newTitle =
            result.converted().getTitle() + " (converted to " + targetUnit.name() + ")";
        Recipe toSave =
            new app.cookyourbooks.model.Recipe(
                null,
                newTitle,
                result.converted().getServings(),
                result.converted().getIngredients(),
                result.converted().getInstructions(),
                result.converted().getConversionRules());
        context.librarianService().saveRecipe(toSave, coll.getId());
        context.println("Saved converted recipe '" + newTitle + "'.");
      } else {
        context.println("Conversion discarded.");
      }
    } catch (UnsupportedConversionException e) {
      context.println("Cannot convert: " + e.getMessage());
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
