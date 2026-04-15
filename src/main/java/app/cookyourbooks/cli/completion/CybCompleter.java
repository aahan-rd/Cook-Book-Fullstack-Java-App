package app.cookyourbooks.cli.completion;

import java.util.List;
import java.util.Locale;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/** Main completer that delegates to context-aware completers based on the current command. */
public final class CybCompleter implements Completer {

  private final CommandCompleter commandCompleter;
  private final RecipeTitleCompleter recipeCompleter;
  private final CollectionNameCompleter collectionCompleter;
  private final UnitCompleter unitCompleter;
  private final CookModeCompleter cookModeCompleter;

  public CybCompleter(
      CommandCompleter commandCompleter,
      RecipeTitleCompleter recipeCompleter,
      CollectionNameCompleter collectionCompleter,
      UnitCompleter unitCompleter,
      CookModeCompleter cookModeCompleter) {
    this.commandCompleter = commandCompleter;
    this.recipeCompleter = recipeCompleter;
    this.collectionCompleter = collectionCompleter;
    this.unitCompleter = unitCompleter;
    this.cookModeCompleter = cookModeCompleter;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    cookModeCompleter.complete(reader, line, candidates);
    if (!candidates.isEmpty()) {
      return;
    }
    List<String> words = line.words();
    if (words.isEmpty()) {
      commandCompleter.complete(reader, line, candidates);
      return;
    }
    String command = words.get(0).toLowerCase(Locale.ROOT);
    int wordIndex = line.wordIndex();
    if (wordIndex == 0) {
      commandCompleter.complete(reader, line, candidates);
      return;
    }
    switch (command) {
      case "recipes" -> {
        if (wordIndex == 1) {
          collectionCompleter.complete(reader, line, candidates);
        }
      }
      case "show", "delete", "scale", "cook", "export" -> {
        if (wordIndex == 1) {
          recipeCompleter.complete(reader, line, candidates);
        }
      }
      case "convert" -> {
        if (wordIndex == 1) {
          recipeCompleter.complete(reader, line, candidates);
        } else if (wordIndex == 2) {
          unitCompleter.complete(reader, line, candidates);
        }
      }
      case "shopping-list" -> recipeCompleter.complete(reader, line, candidates);
      case "import" -> {
        if (wordIndex == 3) {
          collectionCompleter.complete(reader, line, candidates);
        }
      }
      default -> {
        // No completion for unknown commands
      }
    }
  }
}
