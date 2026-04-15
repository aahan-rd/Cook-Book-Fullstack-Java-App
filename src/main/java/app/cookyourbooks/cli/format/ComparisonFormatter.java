package app.cookyourbooks.cli.format;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.VagueIngredient;

/** Formats side-by-side comparison of original vs scaled/converted ingredients. */
public final class ComparisonFormatter {

  private static final int NAME_WIDTH = 24;
  private static final int COL_WIDTH = 12;

  /** Formats scale comparison. */
  public String formatScale(
      @NonNull Recipe original, @NonNull Recipe scaled, int targetServings, double factor) {
    StringBuilder sb = new StringBuilder();
    sb.append("\nScaled '")
        .append(original.getTitle())
        .append("' to ")
        .append(targetServings)
        .append(" servings (")
        .append(String.format("%.1fx", factor))
        .append("):\n");
    sb.append(formatComparisonTable(original.getIngredients(), scaled.getIngredients()));
    return sb.toString();
  }

  /** Formats convert comparison. */
  public String formatConvert(
      @NonNull Recipe original, @NonNull Recipe converted, @NonNull String unitName) {
    StringBuilder sb = new StringBuilder();
    sb.append("\nConverted '")
        .append(original.getTitle())
        .append("' to ")
        .append(unitName.toUpperCase(Locale.ROOT))
        .append(":\n");
    sb.append(formatComparisonTable(original.getIngredients(), converted.getIngredients()));
    return sb.toString();
  }

  private String formatComparisonTable(List<Ingredient> original, List<Ingredient> converted) {
    StringBuilder sb = new StringBuilder();
    sb.append("  ")
        .append(pad("Ingredient", NAME_WIDTH))
        .append(pad("Original", COL_WIDTH))
        .append("        ")
        .append(pad("Scaled", COL_WIDTH))
        .append("\n");
    sb.append("  ").append("─".repeat(NAME_WIDTH + COL_WIDTH * 2 + 10)).append("\n");
    for (int i = 0; i < original.size(); i++) {
      Ingredient o = original.get(i);
      Ingredient c = i < converted.size() ? converted.get(i) : o;
      String name = truncate(o.getName(), NAME_WIDTH - 2);
      String origStr = formatIngredientQuantity(o);
      String convStr = formatIngredientQuantity(c);
      sb.append("  ")
          .append(pad(name, NAME_WIDTH))
          .append(pad(origStr, COL_WIDTH))
          .append("  →  ")
          .append(pad(convStr, COL_WIDTH))
          .append("\n");
    }
    return sb.toString();
  }

  private String formatIngredientQuantity(Ingredient ing) {
    if (ing instanceof VagueIngredient v) {
      return v.getDescription() != null ? v.getDescription() : "to taste";
    }
    if (ing instanceof MeasuredIngredient m) {
      return m.getQuantity().toString();
    }
    return "";
  }

  private String pad(String s, int width) {
    if (s == null) {
      s = "";
    }
    if (s.length() >= width) {
      return s.substring(0, width);
    }
    return s + " ".repeat(width - s.length());
  }

  private String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, max - 2) + "..";
  }
}
