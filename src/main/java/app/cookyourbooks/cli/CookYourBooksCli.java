package app.cookyourbooks.cli;

import java.util.List;
import java.util.Optional;

import org.jline.reader.ParsedLine;

import app.cookyourbooks.cli.commands.CollectionCreateCommand;
import app.cookyourbooks.cli.commands.CollectionsCommand;
import app.cookyourbooks.cli.commands.Command;
import app.cookyourbooks.cli.commands.CommandRegistry;
import app.cookyourbooks.cli.commands.ConversionAddCommand;
import app.cookyourbooks.cli.commands.ConversionRemoveCommand;
import app.cookyourbooks.cli.commands.ConversionsCommand;
import app.cookyourbooks.cli.commands.ConvertCommand;
import app.cookyourbooks.cli.commands.CookCommand;
import app.cookyourbooks.cli.commands.DeleteCommand;
import app.cookyourbooks.cli.commands.ExportCommand;
import app.cookyourbooks.cli.commands.HelpCommand;
import app.cookyourbooks.cli.commands.ImportJsonCommand;
import app.cookyourbooks.cli.commands.QuitCommand;
import app.cookyourbooks.cli.commands.RecipesCommand;
import app.cookyourbooks.cli.commands.ScaleCommand;
import app.cookyourbooks.cli.commands.SearchCommand;
import app.cookyourbooks.cli.commands.ShoppingListCommand;
import app.cookyourbooks.cli.commands.ShowCommand;

/** Main CLI loop for CookYourBooks. */
public final class CookYourBooksCli {

  private static final String PROMPT = "cyb> ";

  private final CliContext context;
  private final CommandRegistry registry;
  private volatile boolean running = true;

  public CookYourBooksCli(CliContext context, CommandRegistry registry) {
    this.context = context;
    this.registry = registry;
  }

  /** Runs the CLI main loop. */
  public void run() {
    context.println("Welcome to CookYourBooks! Type 'help' to get started.");
    context.println("");

    while (running) {
      try {
        String line = context.readLine(PROMPT);
        if (line == null) {
          break;
        }
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }

        ParsedLine parsed = context.lineReader().getParsedLine();
        List<String> words = parsed != null ? parsed.words() : List.of(line.split("\\s+"));
        if (words.isEmpty()) {
          continue;
        }

        dispatch(words);
      } catch (Exception e) {
        context.println("Error: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
      }
    }
  }

  private void dispatch(List<String> words) {
    List<String> args;
    Optional<Command> cmd;

    if (words.size() >= 2) {
      cmd = registry.find(words.get(0) + " " + words.get(1));
      if (cmd.isPresent()) {
        args = words.size() > 2 ? words.subList(2, words.size()) : List.of();
      } else {
        cmd = registry.find(words.get(0));
        args = words.size() > 1 ? words.subList(1, words.size()) : List.of();
      }
    } else {
      cmd = registry.find(words.get(0));
      args = List.of();
    }

    if (cmd.isEmpty()) {
      context.println(
          "Unknown command: '" + words.get(0) + "'. Type 'help' for a list of commands.");
      return;
    }

    if (cmd.get() instanceof QuitCommand) {
      cmd.get().execute(args, context);
      running = false;
      return;
    }

    cmd.get().execute(args, context);
  }

  /** Stops the CLI loop (called by QuitCommand). */
  public void stop() {
    running = false;
  }

  /** Creates and configures the CLI with all commands. */
  public static CookYourBooksCli create(CliContext context) {
    CommandRegistry registry = new CommandRegistry();

    CookYourBooksCli cli = new CookYourBooksCli(context, registry);
    QuitCommand quitCommand = new QuitCommand(cli::stop);

    registry.register(new HelpCommand(registry));
    registry.register(quitCommand);
    registry.registerAlias("exit", quitCommand);
    registry.register(new CollectionsCommand());
    registry.register(new CollectionCreateCommand());
    registry.register(new RecipesCommand());
    registry.register(new ConversionsCommand());
    registry.register(new ConversionAddCommand());
    registry.register(new ConversionRemoveCommand());
    registry.register(new ShowCommand());
    registry.register(new SearchCommand());
    registry.register(new ImportJsonCommand());
    registry.register(new DeleteCommand());
    registry.register(new ScaleCommand());
    registry.register(new ConvertCommand());
    registry.register(new ShoppingListCommand());
    registry.register(new CookCommand());
    registry.register(new ExportCommand());

    return cli;
  }
}
