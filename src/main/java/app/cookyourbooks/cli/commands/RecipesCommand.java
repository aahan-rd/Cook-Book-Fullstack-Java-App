package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.CollectionFormatter;

/** Lists recipes in a collection. */
public final class RecipesCommand extends AbstractCommand {

  private final CollectionFormatter formatter = new CollectionFormatter();

  public RecipesCommand() {
    super(
        "recipes",
        "List recipes in a collection",
        "recipes <collection> - List all recipes in the specified collection",
        "Library");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty() || args.get(0).isBlank()) {
      context.println(
          "Please specify a collection name. Use 'collections' to see available collections.");
      return;
    }
    String name = args.get(0);
    try {
      var recipes = context.librarianService().listRecipes(name);
      context.println(formatter.formatRecipesInCollection(name, recipes));
    } catch (Exception e) {
      context.println("Collection not found: " + e.getMessage());
    }
  }
}
