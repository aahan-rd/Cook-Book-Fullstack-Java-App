package app.cookyourbooks.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Quantity;
import app.cookyourbooks.model.RangeQuantity;
import app.cookyourbooks.model.ShoppingItemImpl;
import app.cookyourbooks.model.ShoppingList;
import app.cookyourbooks.model.ShoppingListImpl;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;

/**
 * Aggregates ingredients from multiple recipes into a shopping list.
 *
 * <p>Combines ingredients with the same name (case-insensitive) and unit. Ingredients with
 * different units are kept as separate items. VagueIngredients are listed as uncountable items.
 */
public final class ShoppingListAggregator {

  /**
   * Aggregates ingredients from the given list into a shopping list.
   *
   * @param ingredients the ingredients to aggregate
   * @return a ShoppingList with combined quantities for like ingredients
   */
  public @NonNull ShoppingList aggregate(@NonNull List<Ingredient> ingredients) {
    // Key: "name|unit" for measured ingredients
    Map<String, ShoppingItemImpl> aggregated = new LinkedHashMap<>();
    // Track uncountable items: use LinkedHashSet to preserve first-encounter order and deduplicate
    Set<String> seenUncountable = new LinkedHashSet<>();
    List<String> uncountableItems = new ArrayList<>();

    for (Ingredient ing : ingredients) {
      if (ing instanceof MeasuredIngredient m) {
        String name = m.getName();
        Unit unit = m.getQuantity().getUnit();
        String key = name.toLowerCase(Locale.ROOT) + "|" + unit.name();
        aggregated.merge(
            key,
            new ShoppingItemImpl(name, m.getQuantity()),
            (existing, neu) ->
                new ShoppingItemImpl(
                    existing.getName(), addQuantities(existing.getQuantity(), neu.getQuantity())));
      } else if (ing instanceof VagueIngredient v) {
        // Deduplicate by lowercase name, but preserve original name from first occurrence
        String lowerName = v.getName().toLowerCase(Locale.ROOT);
        if (seenUncountable.add(lowerName)) {
          uncountableItems.add(v.getName());
        }
      }
    }

    return new ShoppingListImpl(new ArrayList<>(aggregated.values()), uncountableItems);
  }

  private Quantity addQuantities(Quantity qa, Quantity qb) {
    Unit unit = qa.getUnit();
    if (!unit.equals(qb.getUnit())) {
      throw new IllegalArgumentException("Cannot add quantities with different units");
    }
    if (qa instanceof RangeQuantity ra && qb instanceof RangeQuantity rb) {
      return new RangeQuantity(ra.getMin() + rb.getMin(), ra.getMax() + rb.getMax(), unit);
    }
    double sum = qa.toDecimal() + qb.toDecimal();
    return new ExactQuantity(sum, unit);
  }
}
