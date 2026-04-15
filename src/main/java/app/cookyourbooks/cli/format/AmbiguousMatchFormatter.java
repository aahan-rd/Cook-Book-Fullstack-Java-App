package app.cookyourbooks.cli.format;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Recipe;

/** Formats ambiguous recipe matches per the cyb5 spec. */
public final class AmbiguousMatchFormatter {

  private AmbiguousMatchFormatter() {}

  /**
   * Formats multiple recipe matches with short IDs and context-aware hint.
   *
   * @param out output target
   * @param query the user's search string
   * @param matches the matching recipes
   * @param collectionNameFn returns the collection name for a recipe
   */
  public static void formatAmbiguousRecipes(
      @NonNull PrintWriter out,
      @NonNull String query,
      @NonNull List<Recipe> matches,
      @NonNull Function<Recipe, String> collectionNameFn) {
    out.println("Multiple recipes match '" + query + "':");
    for (int i = 0; i < matches.size(); i++) {
      Recipe r = matches.get(i);
      String shortId = shortId(r);
      out.println(
          "  "
              + (i + 1)
              + ". "
              + r.getTitle()
              + "  ["
              + shortId
              + "]  ("
              + collectionNameFn.apply(r)
              + ")");
    }
    boolean allSameTitle = matches.stream().map(Recipe::getTitle).distinct().count() == 1;
    String exampleShortId = shortId(matches.get(0));
    out.println(
        allSameTitle
            ? "Please specify a short ID (e.g. 'show " + exampleShortId + "')."
            : "Please specify the full recipe name, or use a short ID (e.g. 'show "
                + exampleShortId
                + "').");
  }

  private static String shortId(Recipe r) {
    String id = r.getId();
    return id.substring(0, Math.min(8, id.length()));
  }
}
