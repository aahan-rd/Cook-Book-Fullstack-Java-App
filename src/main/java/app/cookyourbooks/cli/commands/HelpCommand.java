package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;

/** Displays help for commands. */
public final class HelpCommand extends AbstractCommand {

  private final CommandRegistry registry;

  public HelpCommand(CommandRegistry registry) {
    super(
        "help",
        "Show help (or help for a specific command)",
        "help [command] - Show all commands or detailed help for a specific command",
        "General");
    this.registry = registry;
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty()) {
      listAllCommands(context);
    } else {
      showCommandHelp(args.get(0), context);
    }
  }

  private void listAllCommands(CliContext context) {
    context.println("\nCookYourBooks Commands:");
    Map<String, List<Command>> byCategory =
        registry.getAllCommands().stream().collect(Collectors.groupingBy(Command::getCategory));
    for (String category : List.of("Library", "Recipe", "Tools", "General")) {
      List<Command> cmds = byCategory.get(category);
      if (cmds != null && !cmds.isEmpty()) {
        context.println("  " + category + ":");
        for (Command c : cmds) {
          context.println("    " + pad(c.getName(), 32) + c.getDescription());
        }
        context.println("");
      }
    }
  }

  private void showCommandHelp(String name, CliContext context) {
    var cmd = registry.find(name);
    if (cmd.isEmpty()) {
      context.println("Unknown command: '" + name + "'. Type 'help' for a list of commands.");
      return;
    }
    context.println("\n" + cmd.get().getDetailedHelp());
  }

  private String pad(String s, int w) {
    return s.length() >= w ? s : s + " ".repeat(w - s.length());
  }
}
