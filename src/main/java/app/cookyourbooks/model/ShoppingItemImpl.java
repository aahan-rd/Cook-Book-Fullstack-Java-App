package app.cookyourbooks.model;

import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Simple implementation of {@link ShoppingItem}.
 *
 * <p>Immutable data container for a single shopping list entry.
 */
public final class ShoppingItemImpl implements ShoppingItem {

  private final String name;
  private final Quantity quantity;

  /**
   * Constructs a shopping item with the given name and quantity.
   *
   * @param name the ingredient name (must not be null or blank)
   * @param quantity the total quantity needed (must not be null)
   * @throws IllegalArgumentException if name is blank or quantity is null
   */
  public ShoppingItemImpl(@NonNull String name, @NonNull Quantity quantity) {
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
  }

  @Override
  public @NonNull String getName() {
    return name;
  }

  @Override
  public @NonNull Quantity getQuantity() {
    return quantity;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ShoppingItemImpl that)) {
      return false;
    }
    return name.equals(that.name) && quantity.equals(that.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, quantity);
  }
}
