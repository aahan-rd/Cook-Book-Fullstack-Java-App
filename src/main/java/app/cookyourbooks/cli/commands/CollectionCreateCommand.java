package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;

/** Creates a new personal collection. */
public final class CollectionCreateCommand extends AbstractCommand {

  public CollectionCreateCommand() {
    super(
        "collection create",
        "Create a personal collection",
        "collection create <name> - Create a new personal collection with the given name",
        "Library");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty()) {
      context.println("Usage: collection create <name>");
      return;
    }
    String name = String.join(" ", args).trim();
    if (name.isBlank()) {
      context.println("Please provide a collection name.");
      return;
    }
    try {
      context.librarianService().createCollection(name);
      context.println("Created personal collection '" + name + "'.");
    } catch (IllegalArgumentException e) {
      context.println(java.util.Objects.requireNonNullElse(e.getMessage(), "Error"));
    }
  }
}
