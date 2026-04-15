package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;

/** Exits the application. */
public final class QuitCommand extends AbstractCommand {

  private final Runnable onQuit;

  public QuitCommand(Runnable onQuit) {
    super("quit", "Exit CookYourBooks", "quit / exit - Exit the application", "General");
    this.onQuit = onQuit;
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    context.println("Goodbye!");
    onQuit.run();
  }
}
