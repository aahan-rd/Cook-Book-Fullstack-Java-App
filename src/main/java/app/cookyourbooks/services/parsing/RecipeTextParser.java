package app.cookyourbooks.services.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.services.ParseException;

/**
 * Parses plain text recipe format into a {@link Recipe}.
 *
 * <p>Handles typical formats with title, servings, ingredients section, and instructions section.
 */
public final class RecipeTextParser {

  private static final Pattern SERVINGS_PATTERN =
      Pattern.compile("(?i)(?:makes|serves)\\s*:?\\s*(\\d+)\\s*(.*)");
  private static final Pattern INGREDIENTS_HEADER =
      Pattern.compile("(?i)^\\s*ingredients?\\s*:?\\s*$");
  private static final Pattern INSTRUCTIONS_HEADER =
      Pattern.compile("(?i)^\\s*(?:instructions?|directions?|steps?)\\s*:?\\s*$");
  // Requires "." or ")" after digit to distinguish numbered lists from quantities like "2 cups"
  private static final Pattern NUMBERED_LINE = Pattern.compile("^\\s*\\d+[.)]\\s+(.*)$");

  private final IngredientParser ingredientParser;

  /** Creates a RecipeTextParser with default IngredientParser. */
  public RecipeTextParser() {
    this.ingredientParser = new IngredientParser();
  }

  /**
   * Creates a RecipeTextParser with the given IngredientParser.
   *
   * @param ingredientParser the parser for ingredient lines
   */
  public RecipeTextParser(IngredientParser ingredientParser) {
    this.ingredientParser = ingredientParser;
  }

  /**
   * Parses plain text into a Recipe.
   *
   * @param recipeText the plain text recipe
   * @return the parsed Recipe
   * @throws ParseException if the text cannot be parsed
   */
  public @NonNull Recipe parse(@NonNull String recipeText) throws ParseException {
    List<String> nonBlank = recipeText.lines().filter(line -> !line.isBlank()).toList();
    if (nonBlank.isEmpty()) {
      throw new ParseException("Recipe text is empty");
    }

    String title = nonBlank.get(0).trim();
    Servings servings = parseServings(nonBlank);
    int ingredientsStart = findSectionStart(nonBlank, INGREDIENTS_HEADER, 1);
    int instructionsStart = findSectionStart(nonBlank, INSTRUCTIONS_HEADER, ingredientsStart);

    List<Ingredient> ingredients;
    List<String> instructionLines;

    if (ingredientsStart >= 0 && instructionsStart >= 0) {
      ingredients = parseIngredients(nonBlank, ingredientsStart, instructionsStart);
      instructionLines = nonBlank.subList(instructionsStart + 1, nonBlank.size());
    } else if (ingredientsStart >= 0) {
      ingredients = parseIngredients(nonBlank, ingredientsStart, nonBlank.size());
      instructionLines = List.of();
    } else if (instructionsStart >= 0) {
      ingredients = parseIngredients(nonBlank, 1, instructionsStart);
      instructionLines = nonBlank.subList(instructionsStart + 1, nonBlank.size());
    } else {
      ingredients = parseIngredientsFromLines(nonBlank, 1, nonBlank.size());
      instructionLines = List.of();
    }

    List<Instruction> instructions = new ArrayList<>();
    for (int step = 0; step < instructionLines.size(); step++) {
      String text = stripNumbering(instructionLines.get(step));
      if (!text.isBlank()) {
        instructions.add(new Instruction(step + 1, text, List.of()));
      }
    }

    return new Recipe(title, servings, ingredients, instructions, List.of());
  }

  private static @Nullable Servings parseServings(List<String> lines) {
    for (int i = 1; i < Math.min(lines.size(), 5); i++) {
      Matcher m = SERVINGS_PATTERN.matcher(lines.get(i));
      if (m.find()) {
        int n = Integer.parseInt(m.group(1));
        if (n > 0) {
          String description = m.groupCount() >= 2 ? m.group(2).trim() : "";
          return description.isEmpty() ? new Servings(n) : new Servings(n, description);
        }
      }
    }
    return null;
  }

  private static int findSectionStart(List<String> lines, Pattern header, int fromIndex) {
    for (int i = fromIndex; i < lines.size(); i++) {
      if (header.matcher(lines.get(i)).matches()) {
        return i;
      }
    }
    return -1;
  }

  private List<Ingredient> parseIngredients(
      List<String> lines, int startInclusive, int endExclusive) throws ParseException {
    return parseIngredientsFromLines(lines, startInclusive + 1, endExclusive);
  }

  private List<Ingredient> parseIngredientsFromLines(
      List<String> lines, int startInclusive, int endExclusive) throws ParseException {
    List<Ingredient> result = new ArrayList<>();
    for (int i = startInclusive; i < endExclusive; i++) {
      String line = lines.get(i);
      if (INSTRUCTIONS_HEADER.matcher(line).matches()
          || INGREDIENTS_HEADER.matcher(line).matches()) {
        break;
      }
      String ingredientLine = stripNumbering(line);
      if (!ingredientLine.isBlank()) {
        result.add(ingredientParser.parse(ingredientLine));
      }
    }
    return result;
  }

  private static String stripNumbering(String line) {
    Matcher m = NUMBERED_LINE.matcher(line);
    if (m.matches()) {
      return m.group(1).trim();
    }
    return line.trim();
  }
}
