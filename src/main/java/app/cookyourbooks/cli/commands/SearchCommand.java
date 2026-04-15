package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.model.Recipe;

/**
 * Searches recipes by ingredient.
 *
 * <p>This command finds all recipes that contain a specified ingredient and displays them grouped
 * by their parent collection.
 *
 * <p><b>Usage:</b> {@code search <ingredient>}
 *
 * <p><b>Example:</b>
 *
 * <pre>
 *   > search flour
 *   Recipes containing 'flour':
 *     1. Basic Waffles         (My Favorites)
 *     2. Chocolate Cake        (Desserts)
 *   Found 2 recipes.
 * </pre>
 *
 * <p>If no recipes match the ingredient, displays "No recipes found containing '...'."
 */
public final class SearchCommand extends AbstractCommand {

  /** Constructs a SearchCommand with standard CLI metadata. */
  public SearchCommand() {
    super(
        "search",
        "Find recipes by ingredient",
        "search <ingredient> - Find recipes containing the specified ingredient",
        "Recipe");
  }

  /**
   * Executes the search command.
   *
   * <p>Validates the ingredient argument, searches the recipe library, and displays results with
   * their parent collection names.
   *
   * @param args the command arguments (first element is the ingredient name)
   * @param context the CLI context providing access to LibrarianService
   * @throws IllegalArgumentException if args is empty or ingredient is blank
   */
  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty() || args.get(0).isBlank()) {
      context.println("Usage: search <ingredient>");
      return;
    }
    String ingredient = String.join(" ", args).trim();
    var matches = context.librarianService().searchByIngredient(ingredient);
    if (matches.isEmpty()) {
      context.println("No recipes found containing '" + ingredient + "'.");
      return;
    }
    context.println("Recipes containing '" + ingredient + "':");
    for (int i = 0; i < matches.size(); i++) {
      Recipe r = matches.get(i);
      String collName = findCollectionForRecipe(context, r);
      context.println("  " + (i + 1) + ". " + r.getTitle() + "         (" + collName + ")");
    }
    context.println("\nFound " + matches.size() + " recipes.");
  }

  /**
   * Finds the parent collection for a recipe.
   *
   * <p>Iterates through all collections to find which one contains the specified recipe.
   *
   * @param context the CLI context with access to collections
   * @param r the recipe to search for
   * @return the collection title, or "Unknown" if not found
   */
  private String findCollectionForRecipe(CliContext context, Recipe r) {
    for (var c : context.librarianService().listCollections()) {
      if (c.containsRecipe(r.getId())) {
        return c.getTitle();
      }
    }
    return "Unknown";
  }
}
