package app.cookyourbooks.cli.completion;

import java.util.List;
import java.util.Locale;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import app.cookyourbooks.model.Unit;

/** Completes unit names for the convert command. */
public final class UnitCompleter implements Completer {

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String prefix = line.word();
    String lower = prefix.toLowerCase(Locale.ROOT);
    for (String name : Unit.getParseableNames()) {
      if (name.toLowerCase(Locale.ROOT).startsWith(lower)) {
        candidates.add(new Candidate(name, name, null, null, null, null, true));
      }
    }
  }
}
