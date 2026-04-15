package app.cookyourbooks.cli.commands;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.AmbiguousMatchFormatter;
import app.cookyourbooks.cli.format.ShoppingListFormatter;
import app.cookyourbooks.model.Recipe;

/** Generates a shopping list from multiple recipes. */
public final class ShoppingListCommand extends AbstractCommand {

  private final ShoppingListFormatter formatter = new ShoppingListFormatter();

  public ShoppingListCommand() {
    super(
        "shopping-list",
        "Generate aggregated shopping list",
        "shopping-list <r1> [r2] ... - Generate shopping list from recipes",
        "Tools");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty()) {
      context.println("Usage: shopping-list <recipe1> [recipe2] ...");
      return;
    }
    List<Recipe> recipes = new ArrayList<>();
    for (String query : args) {
      if (query.isBlank()) {
        continue;
      }
      String q = query.trim();
      var matches = context.librarianService().resolveRecipes(q);
      if (matches.isEmpty()) {
        context.println("Recipe not found: '" + q + "'.");
        return;
      }
      if (matches.size() > 1) {
        AmbiguousMatchFormatter.formatAmbiguousRecipes(
            context.out(), q, matches, r -> findCollectionForRecipe(context, r));
        return;
      }
      recipes.add(matches.get(0));
    }
    var list = context.plannerService().generateShoppingList(recipes);
    context.println(formatter.format(list, recipes.size()));
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
