package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.AmbiguousMatchFormatter;
import app.cookyourbooks.model.Recipe;

/** Deletes a recipe with confirmation. */
public final class DeleteCommand extends AbstractCommand {

  public DeleteCommand() {
    super(
        "delete",
        "Delete a recipe",
        "delete <recipe> - Delete a recipe (with confirmation)",
        "Recipe");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty() || args.get(0).isBlank()) {
      context.println("Usage: delete <recipe>");
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
    Recipe recipe = matches.get(0);
    String response = context.readLine("Delete recipe '" + recipe.getTitle() + "'? (y/n): ");
    if (response != null && response.trim().equalsIgnoreCase("y")) {
      context.librarianService().deleteRecipe(recipe.getId());
      context.println("Deleted recipe '" + recipe.getTitle() + "'.");
    }
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
