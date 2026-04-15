package app.cookyourbooks.cli;

import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.completion.CookModeHolder;
import app.cookyourbooks.services.CookingService;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.PlannerService;
import app.cookyourbooks.services.TransformerService;

/** Shared context for CLI commands: services and I/O. */
public record CliContext(
    @NonNull LibrarianService librarianService,
    @NonNull CookingService cookingService,
    @NonNull PlannerService plannerService,
    @NonNull TransformerService transformerService,
    @NonNull Terminal terminal,
    @NonNull LineReader lineReader,
    @NonNull PrintWriter out,
    @NonNull CookModeHolder cookModeHolder) {

  /** Writes a line to the terminal output. */
  public void println(String s) {
    out.println(s);
    out.flush();
  }

  /** Writes text to the terminal output (no newline). */
  public void print(String s) {
    out.print(s);
    out.flush();
  }

  /** Reads a line from the user with the given prompt. */
  public String readLine(String prompt) {
    return lineReader.readLine(prompt);
  }
}
