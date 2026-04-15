package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.AmbiguousMatchFormatter;
import app.cookyourbooks.cli.format.RecipeFormatter;
import app.cookyourbooks.model.Recipe;

/** Displays a recipe's full details. */
public final class ShowCommand extends AbstractCommand {

  private final RecipeFormatter formatter = new RecipeFormatter();

  public ShowCommand() {
    super("show", "Display a recipe", "show <recipe> - Display full recipe details", "Recipe");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty() || args.get(0).isBlank()) {
      context.println("Usage: show <recipe>");
      return;
    }
    String query = String.join(" ", args).trim();
    var matches = context.librarianService().resolveRecipes(query);
    if (matches.isEmpty()) {
      context.println(
          "Recipe not found: '" + query + "'. Use 'search' to find recipes by ingredient.");
      return;
    }
    if (matches.size() > 1) {
      AmbiguousMatchFormatter.formatAmbiguousRecipes(
          context.out(), query, matches, r -> findCollectionForRecipe(context, r));
      return;
    }
    context.println(formatter.formatFull(matches.get(0)));
  }

  private String findCollectionForRecipe(CliContext context, Recipe r) {
    for (var c : context.librarianService().listCollections()) {
      if (c.containsRecipe(r.getId())) {
        return c.getTitle();
      }
    }
    return "Unknown";
  }
}
