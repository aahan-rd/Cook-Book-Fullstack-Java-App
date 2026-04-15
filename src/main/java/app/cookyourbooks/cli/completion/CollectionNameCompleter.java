package app.cookyourbooks.cli.completion;

import java.util.List;
import java.util.Locale;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.repository.RecipeCollectionRepository;

/** Completes collection names. */
public final class CollectionNameCompleter implements Completer {

  private final RecipeCollectionRepository collectionRepository;

  public CollectionNameCompleter(RecipeCollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String prefix = line.word();
    String lower = prefix.toLowerCase(Locale.ROOT);
    for (RecipeCollection c : collectionRepository.findAll()) {
      if (c.getTitle().toLowerCase(Locale.ROOT).startsWith(lower)) {
        String title = c.getTitle();
        String value = title.contains(" ") ? "\"" + title + "\"" : title;
        candidates.add(new Candidate(value, title, null, null, null, null, true));
      }
    }
  }
}
