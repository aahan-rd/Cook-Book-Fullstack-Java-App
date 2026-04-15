package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;

/** Interface for CLI commands. */
public interface Command {

  /** Returns the primary command name (e.g., "show", "shopping-list"). */
  @NonNull String getName();

  /** Returns a brief description for the help listing. */
  @NonNull String getDescription();

  /** Returns detailed help including syntax and examples. */
  @NonNull String getDetailedHelp();

  /** Returns the category for grouping in help (Library, Recipe, Tools, General). */
  @NonNull String getCategory();

  /** Executes the command with the given arguments. */
  void execute(@NonNull List<String> args, @NonNull CliContext context);
}
