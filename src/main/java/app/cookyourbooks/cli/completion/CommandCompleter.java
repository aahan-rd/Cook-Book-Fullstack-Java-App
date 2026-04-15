package app.cookyourbooks.cli.completion;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import app.cookyourbooks.cli.commands.Command;
import app.cookyourbooks.cli.commands.CommandRegistry;

/** Completes command names. */
public final class CommandCompleter implements Completer {

  private final CommandRegistry registry;

  public CommandCompleter(CommandRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String prefix = line.word();
    for (String name : registry.getAllCommandNames()) {
      if (name.startsWith(prefix.toLowerCase(Locale.ENGLISH))) {
        Optional<Command> cmd = registry.find(name);
        cmd.ifPresent(
            c ->
                candidates.add(
                    new Candidate(name, name, null, c.getDescription(), null, null, true)));
      }
    }
  }
}
