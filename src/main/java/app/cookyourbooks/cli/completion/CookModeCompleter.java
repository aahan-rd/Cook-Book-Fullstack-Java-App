package app.cookyourbooks.cli.completion;

import java.util.List;
import java.util.Locale;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/** Completes cook mode sub-commands: next, prev, ingredients, quit. */
public final class CookModeCompleter implements Completer {

  private static final String[] COMMANDS = {"next", "prev", "ingredients", "quit"};

  private final CookModeHolder cookModeHolder;

  public CookModeCompleter(CookModeHolder cookModeHolder) {
    this.cookModeHolder = cookModeHolder;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (!cookModeHolder.isInCookMode()) {
      return;
    }
    String prefix = line.word().toLowerCase(Locale.ROOT);
    int wordIndex = line.wordIndex();
    if (wordIndex != 0) {
      return;
    }
    for (String cmd : COMMANDS) {
      if (cmd.startsWith(prefix)) {
        candidates.add(new Candidate(cmd, cmd, null, null, null, null, true));
      }
    }
  }
}
