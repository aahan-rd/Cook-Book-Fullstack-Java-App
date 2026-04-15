package app.cookyourbooks.cli.format;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.ShoppingItem;
import app.cookyourbooks.model.ShoppingList;

/** Formats shopping lists for CLI display. */
public final class ShoppingListFormatter {

  /** Formats a shopping list with measured and vague items. */
  public String format(@NonNull ShoppingList list, int recipeCount) {
    StringBuilder sb = new StringBuilder();
    sb.append("Shopping List (").append(recipeCount).append(" recipes):\n");
    sb.append("═══════════════════════════\n");
    sb.append("  Measured Items:\n");
    for (ShoppingItem item : list.getItems()) {
      sb.append("    • ")
          .append(item.getQuantity())
          .append(" ")
          .append(item.getName())
          .append("\n");
    }
    sb.append("\n  Also needed:\n");
    for (String name : list.getUncountableItems()) {
      sb.append("    • ").append(name).append("\n");
    }
    sb.append("\nTotal: ")
        .append(list.getItems().size())
        .append(" measured items, ")
        .append(list.getUncountableItems().size())
        .append(" vague items\n");
    return sb.toString();
  }
}
