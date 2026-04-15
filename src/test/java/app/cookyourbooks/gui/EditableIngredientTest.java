package app.cookyourbooks.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.shared.EditableIngredient;
import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;

/**
 * Tests for EditableIngredient — the shared mutable wrapper.
 *
 * <p>These tests are owned by whoever creates EditableIngredient (coordinate with your team). All
 * four teammates depend on this class working correctly.
 */
class EditableIngredientTest {

  // ── Construction from existing domain objects ──

  @Test
  void fromMeasuredIngredient_preservesNameAndQuantity() {
    var ingredient = new MeasuredIngredient("flour", new ExactQuantity(2, Unit.CUP), null, null);
    var editable = new EditableIngredient(ingredient);

    assertEquals("flour", editable.getName());
    assertNotNull(editable.getOriginalQuantity());
  }

  @Test
  void fromVagueIngredient_preservesNameAndNullQuantity() {
    var ingredient = new VagueIngredient("salt", "to taste", null, null);
    var editable = new EditableIngredient(ingredient);

    assertEquals("salt", editable.getName());
    assertNull(editable.getOriginalQuantity());
  }

  @Test
  void blankConstructor_startsWithEmptyName() {
    var editable = new EditableIngredient();
    assertEquals("", editable.getName());
    assertNull(editable.getOriginalQuantity());
    assertTrue(editable.isBlank());
  }

  // ── Name mutation via property ──

  @Test
  void setName_updatesNameProperty() {
    var editable = new EditableIngredient();
    editable.setName("butter");
    assertEquals("butter", editable.getName());
    assertFalse(editable.isBlank());
  }

  @Test
  void nameProperty_canBeSetDirectly() {
    var editable = new EditableIngredient();
    editable.nameProperty().set("sugar");
    assertEquals("sugar", editable.getName());
  }

  // ── toIngredient() conversion ──

  @Test
  void toIngredient_fromMeasured_returnsMeasuredIngredientWithOriginalQuantity() {
    var quantity = new ExactQuantity(2, Unit.CUP);
    var original = new MeasuredIngredient("flour", quantity, null, null);
    var editable = new EditableIngredient(original);
    editable.setName("bread flour"); // rename it

    Ingredient result = editable.toIngredient();

    assertInstanceOf(MeasuredIngredient.class, result);
    assertEquals("bread flour", result.getName());
    assertEquals(quantity, ((MeasuredIngredient) result).getQuantity());
  }

  @Test
  void toIngredient_fromVague_returnsVagueIngredient() {
    var original = new VagueIngredient("salt", "to taste", null, null);
    var editable = new EditableIngredient(original);

    Ingredient result = editable.toIngredient();

    assertInstanceOf(VagueIngredient.class, result);
    assertEquals("salt", result.getName());
  }

  @Test
  void toIngredient_fromBlankConstructor_returnsVagueIngredient() {
    var editable = new EditableIngredient();
    editable.setName("pepper");

    Ingredient result = editable.toIngredient();

    assertInstanceOf(VagueIngredient.class, result);
    assertEquals("pepper", result.getName());
  }

  @Test
  void toIngredient_stripsWhitespaceFromName() {
    var editable = new EditableIngredient();
    editable.setName("  sugar  ");

    Ingredient result = editable.toIngredient();
    assertEquals("sugar", result.getName());
  }

  // ── isBlank() ──

  @Test
  void isBlank_trueForWhitespaceOnly() {
    var editable = new EditableIngredient();
    editable.setName("   ");
    assertTrue(editable.isBlank());
  }

  @Test
  void isBlank_falseForNonBlankName() {
    var editable = new EditableIngredient();
    editable.setName("eggs");
    assertFalse(editable.isBlank());
  }
}
