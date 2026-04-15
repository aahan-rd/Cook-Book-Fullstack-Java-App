package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.AmbiguousMatchFormatter;
import app.cookyourbooks.cli.mode.CookModeController;
import app.cookyourbooks.model.Recipe;

/** Enters interactive cooking mode. */
public final class CookCommand extends AbstractCommand {

  public CookCommand() {
    super(
        "cook",
        "Step-by-step cooking mode",
        "cook <recipe> - Enter step-by-step cooking mode",
        "Tools");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty() || args.get(0).isBlank()) {
      context.println("Usage: cook <recipe>");
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
    new CookModeController(context).run(matches.get(0));
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
