package app.cookyourbooks.cli.completion;

import java.util.List;
import java.util.Locale;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.repository.RecipeRepository;

/** Completes recipe titles and short IDs (first 8 characters of recipe ID). */
public final class RecipeTitleCompleter implements Completer {

  private static final int SHORT_ID_LENGTH = 8;

  private final RecipeRepository recipeRepository;

  public RecipeTitleCompleter(RecipeRepository recipeRepository) {
    this.recipeRepository = recipeRepository;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String prefix = line.word();
    String lower = prefix.toLowerCase(Locale.ROOT);
    for (Recipe r : recipeRepository.findAll()) {
      String title = r.getTitle();
      String shortId = shortId(r.getId());
      boolean titleMatches = title.toLowerCase(Locale.ROOT).startsWith(lower);
      boolean idMatches = shortId.toLowerCase(Locale.ROOT).startsWith(lower);
      if (titleMatches) {
        String value = title.contains(" ") ? "\"" + title + "\"" : title;
        candidates.add(new Candidate(value, title, null, null, null, null, true));
      }
      if (idMatches) {
        candidates.add(
            new Candidate(shortId, title + " [" + shortId + "]", null, null, null, null, true));
      }
    }
  }

  private static String shortId(String id) {
    if (id == null || id.length() < SHORT_ID_LENGTH) {
      return id != null ? id : "";
    }
    return id.substring(0, SHORT_ID_LENGTH);
  }
}
