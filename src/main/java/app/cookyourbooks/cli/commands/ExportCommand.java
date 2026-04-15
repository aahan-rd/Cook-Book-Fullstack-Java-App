package app.cookyourbooks.cli.commands;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.AmbiguousMatchFormatter;
import app.cookyourbooks.model.Recipe;

/** Exports a recipe to Markdown. */
public final class ExportCommand extends AbstractCommand {

  public ExportCommand() {
    super(
        "export",
        "Export recipe to Markdown",
        "export <recipe> <file> - Export recipe to Markdown file",
        "Tools");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.size() < 2) {
      context.println("Usage: export <recipe> <file>");
      return;
    }
    String query = args.get(0);
    Path file = Path.of(args.get(1));
    var matches = context.librarianService().resolveRecipes(query);
    if (matches.isEmpty()) {
      context.println("Recipe not found: '" + query + "'. Use 'search' to find recipes.");
      return;
    }
    if (matches.size() > 1) {
      AmbiguousMatchFormatter.formatAmbiguousRecipes(
          context.out(), query, matches, r -> findCollectionForRecipe(context, r));
      return;
    }
    try {
      context.plannerService().exportToMarkdown(matches.get(0), file);
      context.println("Exported '" + matches.get(0).getTitle() + "' to " + file.toAbsolutePath());
    } catch (Exception e) {
      context.println(java.util.Objects.requireNonNullElse(e.getMessage(), "Error"));
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
