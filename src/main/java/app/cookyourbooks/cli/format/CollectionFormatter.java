package app.cookyourbooks.cli.format;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;

/** Formats collections and recipe listings for CLI display. */
public final class CollectionFormatter {

  /** Formats the collection type for display. */
  public String formatSourceType(SourceType type) {
    return switch (type) {
      case PERSONAL -> "Personal";
      case PUBLISHED_BOOK -> "Cookbook";
      case WEBSITE -> "Web";
    };
  }

  /** Formats the collections list. */
  public String formatCollections(@NonNull List<RecipeCollection> collections) {
    if (collections.isEmpty()) {
      return "No collections. Use 'collection create <name>' to create one.";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Collections:\n");
    int i = 1;
    for (RecipeCollection c : collections) {
      sb.append("  ")
          .append(i++)
          .append(". ")
          .append(pad(c.getTitle(), 24))
          .append("[")
          .append(formatSourceType(c.getSourceType()))
          .append("]   ")
          .append(c.getRecipes().size())
          .append(" recipes\n");
    }
    return sb.toString();
  }

  /** Formats recipes in a collection. */
  public String formatRecipesInCollection(
      @NonNull String collectionTitle, @NonNull List<Recipe> recipes) {
    StringBuilder sb = new StringBuilder();
    sb.append(collectionTitle).append(" (").append(recipes.size()).append(" recipes):\n");
    int i = 1;
    for (Recipe r : recipes) {
      String servings = r.getServings() != null ? "Serves " + r.getServings() : "No servings";
      sb.append("  ")
          .append(i++)
          .append(". ")
          .append(pad(r.getTitle(), 32))
          .append(servings)
          .append("\n");
    }
    return sb.toString();
  }

  /** Formats house conversions list. */
  public String formatConversions(
      @NonNull List<app.cookyourbooks.conversion.ConversionRule> rules) {
    if (rules.isEmpty()) {
      return "No house conversions defined. Use 'conversion add' to add one.";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("House Conversions (").append(rules.size()).append(" rules):\n");
    int i = 1;
    for (var r : rules) {
      String ing = r.ingredientName() != null ? " " + r.ingredientName() : " any";
      sb.append("  ")
          .append(i++)
          .append(". 1 ")
          .append(r.fromUnit().getAbbreviation())
          .append(ing)
          .append(" = ")
          .append(r.factor())
          .append(" ")
          .append(r.toUnit().getAbbreviation())
          .append("\n");
    }
    return sb.toString();
  }

  private String pad(String s, int width) {
    if (s == null) {
      s = "";
    }
    if (s.length() >= width) {
      return s;
    }
    return s + " ".repeat(width - s.length());
  }
}
