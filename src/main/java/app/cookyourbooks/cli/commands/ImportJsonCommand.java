package app.cookyourbooks.cli.commands;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.services.ImportException;

/** Imports a recipe from a JSON file. */
public final class ImportJsonCommand extends AbstractCommand {

  public ImportJsonCommand() {
    super(
        "import json",
        "Import recipe from JSON file",
        "import json <file> <collection> - Import recipe from JSON file into collection",
        "Recipe");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.size() < 2) {
      context.println("Usage: import json <file> <collection>");
      return;
    }
    Path file = Path.of(args.get(0));
    String collectionName = String.join(" ", args.subList(1, args.size())).trim();
    try {
      var recipe = context.librarianService().importFromJson(file, collectionName);
      context.println("Imported '" + recipe.getTitle() + "' into '" + collectionName + "'.");
    } catch (ImportException e) {
      context.println(java.util.Objects.requireNonNullElse(e.getMessage(), "Error"));
    } catch (Exception e) {
      context.println(
          "Collection not found: '"
              + collectionName
              + "'. Use 'collections' to see available collections.");
    }
  }
}
