package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.CollectionFormatter;

/** Lists house conversion rules. */
public final class ConversionsCommand extends AbstractCommand {

  private final CollectionFormatter formatter = new CollectionFormatter();

  public ConversionsCommand() {
    super(
        "conversions",
        "List house conversion rules",
        "conversions - List all house conversion rules",
        "Library");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    var rules = context.librarianService().listHouseConversions();
    context.println(formatter.formatConversions(rules));
  }
}
