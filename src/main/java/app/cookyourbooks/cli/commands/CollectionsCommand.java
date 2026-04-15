package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.CollectionFormatter;

/** Lists all recipe collections. */
public final class CollectionsCommand extends AbstractCommand {

  private final CollectionFormatter formatter = new CollectionFormatter();

  public CollectionsCommand() {
    super(
        "collections",
        "List all recipe collections",
        "collections - List all recipe collections with their type and recipe count",
        "Library");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    var collections = context.librarianService().listCollections();
    context.println(formatter.formatCollections(collections));
  }
}
