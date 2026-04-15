package app.cookyourbooks.gui.shared;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.Nullable;

import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Quantity;
import app.cookyourbooks.model.VagueIngredient;

/**
 * A mutable, JavaFX-friendly wrapper around an {@link Ingredient}.
 *
 * <p>Domain {@link Ingredient} objects are immutable and enforce non-blank names, which makes them
 * unsuitable for direct binding to form fields — a user typing a new ingredient starts with a blank
 * name, which the domain object would reject. This class holds the editable state and converts back
 * to an immutable {@link Ingredient} when the user is done editing.
 *
 * <p>Used by both Recipe Editor and Import Interface — lives in {@code gui/shared} so both features
 * can depend on the same class.
 */
public class EditableIngredient {

  private final StringProperty name = new SimpleStringProperty("");

  /**
   * The original quantity from the domain object, preserved so that editing just the name of a
   * MeasuredIngredient does not lose its quantity. Null if this ingredient was originally vague or
   * was created blank for a new entry.
   */
  @Nullable private final Quantity originalQuantity;

  /**
   * Creates an EditableIngredient from an existing domain Ingredient. Preserves the quantity if the
   * ingredient is a MeasuredIngredient.
   *
   * @param ingredient the domain ingredient to wrap; must not be null
   */
  public EditableIngredient(Ingredient ingredient) {
    if (ingredient instanceof MeasuredIngredient mi) {
      this.name.set(mi.getName());
      this.originalQuantity = mi.getQuantity();
    } else {
      this.name.set(ingredient.getName());
      this.originalQuantity = null;
    }
  }

  /**
   * Creates a blank EditableIngredient for a new entry. Converts to a VagueIngredient when saved.
   */
  public EditableIngredient() {
    this.originalQuantity = null;
  }

  /**
   * Returns the observable name property for JavaFX binding. Bind this bidirectionally to a
   * TextField in the View.
   *
   * @return the name property; never null
   */
  public StringProperty nameProperty() {
    return name;
  }

  /**
   * Returns the current name value.
   *
   * @return the ingredient name; may be blank if the user hasn't typed yet
   */
  public String getName() {
    return name.get();
  }

  /**
   * Sets the name directly (useful in tests without JavaFX binding).
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name.set(name);
  }

  /**
   * Returns the original quantity this ingredient was created with, or null if this was a vague
   * ingredient or a new blank entry.
   *
   * @return the original quantity, or null
   */
  @Nullable
  public Quantity getOriginalQuantity() {
    return originalQuantity;
  }

  /**
   * Converts this editable ingredient back to an immutable domain Ingredient.
   *
   * <p>If this wrapper was created from a MeasuredIngredient and the name is still non-blank,
   * returns a MeasuredIngredient preserving the original quantity. Otherwise returns a
   * VagueIngredient with just the name.
   *
   * @return an immutable Ingredient representing the current state
   * @throws IllegalArgumentException if the name is blank (caller should filter out blank entries
   *     before calling this)
   */
  public Ingredient toIngredient() {
    String trimmed = name.get().strip();
    if (originalQuantity != null) {
      return new MeasuredIngredient(trimmed, originalQuantity, null, null);
    }
    return new VagueIngredient(trimmed, null, null, null);
  }

  /**
   * Returns true if the current name is blank (empty or whitespace only). Callers should filter
   * these out before converting to domain objects.
   *
   * @return true if the name is blank
   */
  public boolean isBlank() {
    return name.get().isBlank();
  }

  /**
   * Returns display string
   *
   * @return string
   */
  public String getDisplayString() {
    if (originalQuantity != null && !name.get().isBlank()) {
      return originalQuantity.toString() + " " + name.get().strip();
    }
    return name.get().strip();
  }
}
