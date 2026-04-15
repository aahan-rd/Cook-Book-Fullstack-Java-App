package app.cookyourbooks.services.parsing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import app.cookyourbooks.model.ExactQuantity;
import app.cookyourbooks.model.FractionalQuantity;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Quantity;
import app.cookyourbooks.model.RangeQuantity;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.ParseException;

/**
 * Parses ingredient strings into {@link Ingredient} domain objects.
 *
 * <p>Handles measured ingredients (with quantity and unit) and vague ingredients (e.g., "salt to
 * taste"). Unit recognition is case-insensitive.
 */
public final class IngredientParser {

  // Quantity patterns ordered by specificity (most specific first)
  private static final Pattern MIXED_NUMBER = Pattern.compile("^(\\d+)\\s+(\\d+)/(\\d+)\\s+");
  private static final Pattern FRACTION_ONLY = Pattern.compile("^(\\d+)/(\\d+)\\s+");
  private static final Pattern RANGE =
      Pattern.compile("^(\\d+(?:\\.\\d+)?)-(\\d+(?:\\.\\d+)?)\\s+");
  private static final Pattern EXACT_OR_DECIMAL = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s+");

  private final Map<String, Unit> unitAliases;

  /** Creates a new IngredientParser with default unit aliases. */
  public IngredientParser() {
    this.unitAliases = buildUnitAliases();
  }

  // ──────────────────────────────────────────────────────────────────────────────
  // Unit Alias Configuration
  // ──────────────────────────────────────────────────────────────────────────────

  private static Map<String, Unit> buildUnitAliases() {
    Map<String, Unit> map = new LinkedHashMap<>();

    // Add standard abbreviations from Unit enum
    Arrays.stream(Unit.values())
        .forEach(
            unit -> {
              String abbr = normalize(unit.getAbbreviation());
              String plural = normalize(unit.getPluralAbbreviation());
              if (!abbr.equals(plural)) {
                map.put(plural, unit);
              }
              map.put(abbr, unit);
            });

    // Additional common aliases grouped by unit type
    addAliases(
        map,
        alias(Unit.PINCH, "pinch", "pinches"),
        alias(Unit.DASH, "dash", "dashes"),
        alias(Unit.HANDFUL, "handful", "handfuls"),
        alias(Unit.TABLESPOON, "tablespoon", "tablespoons"),
        alias(Unit.TEASPOON, "teaspoon", "teaspoons"),
        alias(Unit.CUP, "cup", "cups", "c"),
        alias(Unit.GRAM, "gram", "grams", "g"),
        alias(Unit.POUND, "pound", "pounds", "lb", "lbs"),
        alias(Unit.OUNCE, "ounce", "ounces", "oz"),
        alias(Unit.FLUID_OUNCE, "fluid ounce", "fluid ounces", "fl oz"),
        alias(Unit.MILLILITER, "milliliter", "milliliters", "ml"),
        alias(Unit.LITER, "liter", "liters", "l"),
        alias(Unit.KILOGRAM, "kilogram", "kilograms", "kg"),
        alias(Unit.WHOLE, "whole", "wholes"));

    return map;
  }

  private record UnitAlias(Unit unit, String[] names) {}

  private static UnitAlias alias(Unit unit, String... names) {
    return new UnitAlias(unit, names);
  }

  private static void addAliases(Map<String, Unit> map, UnitAlias... aliases) {
    Arrays.stream(aliases)
        .forEach(
            alias ->
                Arrays.stream(alias.names())
                    .forEach(name -> map.put(normalize(name), alias.unit())));
  }

  private static String normalize(String s) {
    return s.toLowerCase(Locale.ROOT);
  }

  // ──────────────────────────────────────────────────────────────────────────────
  // Main Parse Entry Point
  // ──────────────────────────────────────────────────────────────────────────────

  /**
   * Parses an ingredient string into an Ingredient.
   *
   * @param line the ingredient line (e.g., "2 cups flour, sifted")
   * @return the parsed Ingredient
   * @throws ParseException if the line cannot be parsed
   */
  public @NonNull Ingredient parse(@NonNull String line) throws ParseException {
    String trimmed = line.trim();
    if (trimmed.isEmpty()) {
      throw new ParseException("Empty ingredient line");
    }

    Optional<Ingredient> vague = tryParseVague(trimmed);
    return vague.isPresent() ? vague.get() : parseMeasured(trimmed, line);
  }

  // ──────────────────────────────────────────────────────────────────────────────
  // Vague Ingredient Parsing
  // ──────────────────────────────────────────────────────────────────────────────

  private Optional<Ingredient> tryParseVague(String trimmed) {
    return tryParseToTaste(trimmed)
        .or(() -> tryParseAPinchOf(trimmed))
        .or(() -> tryParseDescriptive(trimmed))
        .or(() -> tryParseArticleAsOne(trimmed))
        .or(() -> tryParseNonNumeric(trimmed));
  }

  /** Parses "salt to taste" style ingredients. */
  private Optional<Ingredient> tryParseToTaste(String trimmed) {
    String lower = normalize(trimmed);
    if (!lower.endsWith(" to taste")) {
      return Optional.empty();
    }
    String name = trimmed.substring(0, lower.indexOf(" to taste")).trim();
    return Optional.of(new VagueIngredient(name, "to taste", null, null));
  }

  /** Parses "a pinch of nutmeg" as MeasuredIngredient(1, PINCH) when the word is a unit. */
  private Optional<Ingredient> tryParseAPinchOf(String trimmed) {
    String lower = normalize(trimmed);
    if (!lower.startsWith("a ") || !lower.contains(" of ")) {
      return Optional.empty();
    }
    int ofIdx = lower.indexOf(" of ");
    String beforeOf = trimmed.substring(0, ofIdx).trim();
    String name = trimmed.substring(ofIdx + 4).trim();
    if (name.isEmpty()) {
      return Optional.empty();
    }
    // "a pinch" -> extract "pinch" and check if it's a unit
    String afterA = beforeOf.length() > 2 ? beforeOf.substring(2).trim() : "";
    Unit unit = unitAliases.get(normalize(afterA));
    if (unit != null) {
      return Optional.of(new MeasuredIngredient(name, new ExactQuantity(1, unit), null, null));
    }
    return Optional.empty();
  }

  /** Parses "a pinch of salt" style ingredients (non-unit case) as VagueIngredient. */
  private Optional<Ingredient> tryParseDescriptive(String trimmed) {
    String lower = normalize(trimmed);
    if (!lower.startsWith("a ") || !lower.contains(" of ")) {
      return Optional.empty();
    }
    int ofIdx = lower.indexOf(" of ");
    String desc = trimmed.substring(0, ofIdx).trim();
    String name = trimmed.substring(ofIdx + 4).trim();
    return Optional.of(new VagueIngredient(name, desc, null, null));
  }

  /** Parses "a large egg" or "an onion" as MeasuredIngredient(1, WHOLE). */
  private Optional<Ingredient> tryParseArticleAsOne(String trimmed) {
    String lower = normalize(trimmed);
    String rest;
    if (lower.startsWith("a ")) {
      rest = trimmed.substring(2).trim();
    } else if (lower.startsWith("an ")) {
      rest = trimmed.substring(3).trim();
    } else {
      return Optional.empty();
    }
    if (rest.isEmpty()) {
      return Optional.empty();
    }
    // Don't parse as measured if rest starts with a unit (e.g., "a pinch of X" handled above)
    if (rest.contains(" of ")) {
      return Optional.empty();
    }
    return Optional.of(new MeasuredIngredient(rest, new ExactQuantity(1, Unit.WHOLE), null, null));
  }

  /** Parses ingredients that don't start with a number. */
  private Optional<Ingredient> tryParseNonNumeric(String trimmed) {
    if (trimmed.isEmpty() || Character.isDigit(trimmed.charAt(0))) {
      return Optional.empty();
    }
    return Optional.of(new VagueIngredient(trimmed, null, null, null));
  }

  // ──────────────────────────────────────────────────────────────────────────────
  // Measured Ingredient Parsing
  // ──────────────────────────────────────────────────────────────────────────────

  private Ingredient parseMeasured(String trimmed, String originalLine) throws ParseException {
    PrepSplit split = splitPreparation(trimmed);

    ParseResult result =
        parseQuantityAndUnit(split.main())
            .orElseThrow(() -> new ParseException("Cannot parse ingredient: " + originalLine));

    String name = result.rest().trim();
    // Strip leading "of " per spec: "1 cup of flour" -> name "flour"
    if (name.toLowerCase(Locale.ROOT).startsWith("of ")) {
      name = name.substring(3).trim();
    }
    if (name.isEmpty()) {
      throw new ParseException("Missing ingredient name: " + originalLine);
    }
    return new MeasuredIngredient(name, result.quantity(), split.preparation(), null);
  }

  private record PrepSplit(String main, @Nullable String preparation) {}

  private PrepSplit splitPreparation(String s) {
    int commaIdx = s.indexOf(',');
    if (commaIdx <= 0) {
      return new PrepSplit(s, null);
    }
    return new PrepSplit(s.substring(0, commaIdx).trim(), s.substring(commaIdx + 1).trim());
  }

  // ──────────────────────────────────────────────────────────────────────────────
  // Quantity Parsing
  // ──────────────────────────────────────────────────────────────────────────────

  private record ParseResult(Quantity quantity, String rest) {}

  /** Strategy for parsing a specific quantity format. */
  @FunctionalInterface
  private interface QuantityParser {
    Optional<ParseResult> parse(String input, Function<String, Optional<UnitMatch>> unitFinder);
  }

  /** All quantity parsers in order of specificity. */
  private final List<QuantityParser> quantityParsers =
      List.of(
          this::tryParseMixedNumber,
          this::tryParseFraction,
          this::tryParseRange,
          this::tryParseExact);

  private Optional<ParseResult> parseQuantityAndUnit(String s) {
    String input = s.trim();
    return quantityParsers.stream()
        .map(parser -> parser.parse(input, this::findUnit))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  /** Parses mixed numbers like "2 1/2 cups". */
  private Optional<ParseResult> tryParseMixedNumber(
      String s, Function<String, Optional<UnitMatch>> unitFinder) {
    return matchPattern(MIXED_NUMBER, s)
        .flatMap(
            m -> {
              int whole = Integer.parseInt(m.group(1));
              int num = Integer.parseInt(m.group(2));
              int denom = Integer.parseInt(m.group(3));
              String rest = s.substring(m.end());
              return unitFinder
                  .apply(rest)
                  .map(
                      um ->
                          new ParseResult(
                              new FractionalQuantity(whole, num, denom, um.unit()),
                              rest.substring(um.length()).trim()));
            });
  }

  /** Parses simple fractions like "1/2 cup". */
  private Optional<ParseResult> tryParseFraction(
      String s, Function<String, Optional<UnitMatch>> unitFinder) {
    return matchPattern(FRACTION_ONLY, s)
        .flatMap(
            m -> {
              int num = Integer.parseInt(m.group(1));
              int denom = Integer.parseInt(m.group(2));
              String rest = s.substring(m.end());
              return unitFinder
                  .apply(rest)
                  .map(
                      um ->
                          new ParseResult(
                              new FractionalQuantity(0, num, denom, um.unit()),
                              rest.substring(um.length()).trim()));
            });
  }

  /** Parses ranges like "2-3 cups" or "1.5-2 lbs". */
  private Optional<ParseResult> tryParseRange(
      String s, Function<String, Optional<UnitMatch>> unitFinder) {
    return matchPattern(RANGE, s)
        .map(
            m -> {
              double min = Double.parseDouble(m.group(1));
              double max = Double.parseDouble(m.group(2));
              String rest = s.substring(m.end());
              return unitFinder
                  .apply(rest)
                  .map(
                      um ->
                          new ParseResult(
                              new RangeQuantity(min, max, um.unit()),
                              rest.substring(um.length()).trim()))
                  .orElseGet(
                      () -> new ParseResult(new RangeQuantity(min, max, Unit.WHOLE), rest.trim()));
            });
  }

  /** Parses exact amounts like "2 cups" or "1.5 lbs". */
  private Optional<ParseResult> tryParseExact(
      String s, Function<String, Optional<UnitMatch>> unitFinder) {
    return matchPattern(EXACT_OR_DECIMAL, s)
        .map(
            m -> {
              double amount = Double.parseDouble(m.group(1));
              String rest = s.substring(m.end());
              return unitFinder
                  .apply(rest)
                  .map(
                      um ->
                          new ParseResult(
                              new ExactQuantity(amount, um.unit()),
                              rest.substring(um.length()).trim()))
                  .orElseGet(
                      () -> new ParseResult(new ExactQuantity(amount, Unit.WHOLE), rest.trim()));
            });
  }

  private Optional<Matcher> matchPattern(Pattern pattern, String input) {
    Matcher m = pattern.matcher(input);
    return m.find() ? Optional.of(m) : Optional.empty();
  }

  // ──────────────────────────────────────────────────────────────────────────────
  // Unit Matching
  // ──────────────────────────────────────────────────────────────────────────────

  private record UnitMatch(Unit unit, int length) {}

  private Optional<UnitMatch> findUnit(String s) {
    if (s == null || s.isBlank()) {
      return Optional.empty();
    }
    String padded = " " + s + " ";
    String lower = normalize(padded);

    return unitAliases.entrySet().stream()
        .filter(e -> lower.indexOf(" " + e.getKey() + " ") == 0)
        .max(Comparator.comparingInt(e -> e.getKey().length()))
        .map(e -> new UnitMatch(e.getValue(), e.getKey().length()));
  }
}
