package app.cookyourbooks.cli.format;

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.IngredientRef;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;

/** Formats recipes for CLI display. */
public final class RecipeFormatter {

  private static final String NO_INGREDIENTS = "(no ingredients used in this step)";
  private static final String USES_PREFIX = "  Uses: ";

  /** Formats a recipe with decorative box for show/cook commands. */
  public String formatFull(@NonNull Recipe recipe) {
    StringBuilder sb = new StringBuilder();
    String title = recipe.getTitle();
    int width = Math.max(title.length() + 4, 35);
    String line = "═".repeat(width);
    sb.append(line).append("\n");
    sb.append("  ").append(title).append("\n");
    if (recipe.getServings() != null) {
      sb.append("  Serves ").append(recipe.getServings()).append("\n");
    }
    sb.append(line).append("\n\n");
    sb.append("Ingredients:\n");
    for (var ing : recipe.getIngredients()) {
      sb.append("  • ").append(ing.toString()).append("\n");
    }
    sb.append("\nInstructions:\n");
    for (var inst : recipe.getInstructions()) {
      sb.append("  ").append(inst.getStepNumber()).append(". ").append(inst.getText()).append("\n");
    }
    return sb.toString();
  }

  /** Formats a recipe for cook mode header (with "COOKING:" prefix). */
  public String formatCookHeader(@NonNull Recipe recipe) {
    StringBuilder sb = new StringBuilder();
    String title = recipe.getTitle();
    int width = Math.max(("COOKING: " + title).length() + 4, 40);
    String line = "═".repeat(width);
    sb.append(line).append("\n");
    sb.append("  COOKING: ").append(title).append("\n");
    if (recipe.getServings() != null) {
      sb.append("  Serves ").append(recipe.getServings()).append("\n");
    }
    sb.append(line).append("\n");
    return sb.toString();
  }

  /** Formats ingredients in two columns for compact display. */
  public String formatIngredientsCompact(@NonNull Recipe recipe) {
    var ingredients = recipe.getIngredients();
    int half = (ingredients.size() + 1) / 2;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < half; i++) {
      sb.append("  • ").append(ingredients.get(i).toString());
      if (i + half < ingredients.size()) {
        sb.append("              • ").append(ingredients.get(i + half).toString());
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * Formats the consumed ingredients for a cooking step.
   *
   * <p>Uses the instruction's ingredientRefs. If empty, returns the "(no ingredients used in this
   * step)" message. Otherwise returns "Uses: " followed by comma-separated ingredient refs.
   *
   * @param instruction the instruction for the current step
   * @return formatted string for display
   */
  public String formatConsumedIngredients(@NonNull Instruction instruction) {
    List<IngredientRef> refs = instruction.getIngredientRefs();
    if (refs.isEmpty()) {
      return "  " + NO_INGREDIENTS;
    }
    String parts = refs.stream().map(this::formatIngredientRef).collect(Collectors.joining(", "));
    return USES_PREFIX + parts;
  }

  private String formatIngredientRef(IngredientRef ref) {
    if (ref.quantity() != null) {
      String prep = ref.ingredient().getPreparation();
      String suffix = (prep != null && !prep.isEmpty()) ? ", " + prep : "";
      return ref.quantity().toString() + " " + ref.ingredient().getName() + suffix;
    }
    return ref.ingredient().toString();
  }
}
