package app.cookyourbooks.model;

import java.util.Locale;

/**
 * An enumeration representing units of measurement for ingredients.
 *
 * <p>Each unit belongs to a specific {@link UnitSystem}, has a {@link UnitDimension} (weight,
 * volume, count, or other), and has both singular and plural abbreviations for proper display
 * formatting.
 */
public enum Unit {
  // Imperial units
  /** Cup (imperial) - "cup"/"cups" */
  CUP(UnitSystem.IMPERIAL, UnitDimension.VOLUME, "cup", "cups"),
  /** Tablespoon (imperial) - "tbsp"/"tbsp" */
  TABLESPOON(UnitSystem.IMPERIAL, UnitDimension.VOLUME, "tbsp", "tbsp"),
  /** Teaspoon (imperial) - "tsp"/"tsp" */
  TEASPOON(UnitSystem.IMPERIAL, UnitDimension.VOLUME, "tsp", "tsp"),
  /** Fluid ounce (imperial) - "fl oz"/"fl oz" */
  FLUID_OUNCE(UnitSystem.IMPERIAL, UnitDimension.VOLUME, "fl oz", "fl oz"),
  /** Ounce (imperial) - "oz"/"oz" */
  OUNCE(UnitSystem.IMPERIAL, UnitDimension.WEIGHT, "oz", "oz"),
  /** Pound (imperial) - "lb"/"lb" */
  POUND(UnitSystem.IMPERIAL, UnitDimension.WEIGHT, "lb", "lb"),

  // Metric units
  /** Milliliter (metric) - "ml"/"ml" */
  MILLILITER(UnitSystem.METRIC, UnitDimension.VOLUME, "ml", "ml"),
  /** Liter (metric) - "L"/"L" */
  LITER(UnitSystem.METRIC, UnitDimension.VOLUME, "L", "L"),
  /** Gram (metric) - "g"/"g" */
  GRAM(UnitSystem.METRIC, UnitDimension.WEIGHT, "g", "g"),
  /** Kilogram (metric) - "kg"/"kg" */
  KILOGRAM(UnitSystem.METRIC, UnitDimension.WEIGHT, "kg", "kg"),

  // House count units
  /** Whole (for counting items like eggs) - "whole"/"whole" */
  WHOLE(UnitSystem.HOUSE, UnitDimension.COUNT, "whole", "whole"),

  // House units
  /** Pinch (house) - "pinch"/"pinches" */
  PINCH(UnitSystem.HOUSE, UnitDimension.OTHER, "pinch", "pinches"),
  /** Dash (house) - "dash"/"dashes" */
  DASH(UnitSystem.HOUSE, UnitDimension.OTHER, "dash", "dashes"),
  /** Handful (house) - "handful"/"handfuls" */
  HANDFUL(UnitSystem.HOUSE, UnitDimension.OTHER, "handful", "handfuls"),
  /** To taste (house) - "to taste"/"to taste" */
  TO_TASTE(UnitSystem.HOUSE, UnitDimension.OTHER, "to taste", "to taste");

  private final UnitSystem system;
  private final UnitDimension dimension;
  private final String abbreviation;
  private final String pluralAbbreviation;

  /**
   * Constructs a Unit enum constant with its system, dimension, and abbreviations.
   *
   * @param system the unit system this unit belongs to
   * @param dimension the physical dimension of this unit
   * @param abbreviation the singular abbreviation
   * @param pluralAbbreviation the plural abbreviation
   */
  Unit(UnitSystem system, UnitDimension dimension, String abbreviation, String pluralAbbreviation) {
    this.system = system;
    this.dimension = dimension;
    this.abbreviation = abbreviation;
    this.pluralAbbreviation = pluralAbbreviation;
  }

  /**
   * Returns the unit system this unit belongs to.
   *
   * @return the unit system (never null)
   */
  public UnitSystem getSystem() {
    return system;
  }

  /**
   * Returns the physical dimension of this unit.
   *
   * @return the dimension (never null)
   */
  public UnitDimension getDimension() {
    return dimension;
  }

  /**
   * Returns the singular form abbreviation for display.
   *
   * @return the singular abbreviation (never null)
   */
  public String getAbbreviation() {
    return abbreviation;
  }

  /**
   * Returns the plural form abbreviation for display.
   *
   * @return the plural abbreviation (never null)
   */
  public String getPluralAbbreviation() {
    return pluralAbbreviation;
  }

  /**
   * Parses a unit name string into a Unit enum constant.
   *
   * <p>Matching is case-insensitive. Accepts enum names (e.g., "GRAM", "gram"), abbreviations
   * (e.g., "g", "cup", "tsp", "tbsp"), and plural forms (e.g., "cups", "grams").
   *
   * @param s the string to parse (must not be null or blank)
   * @return the matching Unit
   * @throws IllegalArgumentException if the string does not match any known unit
   */
  public static Unit parse(String s) {
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException("Unit string cannot be null or blank");
    }
    String normalized = s.trim().toLowerCase(Locale.ROOT);
    for (Unit u : values()) {
      if (u.name().toLowerCase(Locale.ROOT).equals(normalized)
          || u.getAbbreviation().toLowerCase(Locale.ROOT).equals(normalized)
          || u.getPluralAbbreviation().toLowerCase(Locale.ROOT).equals(normalized)) {
        return u;
      }
    }
    throw new IllegalArgumentException("Unknown unit: '" + s + "'");
  }

  /**
   * Returns all valid unit names for tab completion and error messages.
   *
   * @return array of unit abbreviation strings (never null)
   */
  public static String[] getParseableNames() {
    return java.util.Arrays.stream(values())
        .map(Unit::getAbbreviation)
        .distinct()
        .toArray(String[]::new);
  }
}
