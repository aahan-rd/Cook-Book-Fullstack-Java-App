package app.cookyourbooks.services;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.exception.UnsupportedConversionException;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Unit;

/**
 * Shared capability for recipe transformations: scaling and unit conversion.
 *
 * <p>Does not persist results — enables "preview before save" workflows. The Planner (and other
 * actors) use this service to transform recipes; the CLI or other driving adapters decide whether
 * to persist.
 */
public interface TransformerService {

  /**
   * Scales a recipe to the target number of servings.
   *
   * @param recipe the recipe to scale (must not be null)
   * @param targetServings the desired number of servings (must be positive)
   * @return the original and scaled recipes plus the scale factor
   * @throws IllegalArgumentException if targetServings is not positive, or if the recipe has no
   *     servings information
   */
  @NonNull ScaleResult scale(@NonNull Recipe recipe, int targetServings);

  /**
   * Converts all measured ingredients in a recipe to the target unit.
   *
   * @param recipe the recipe to convert (must not be null)
   * @param targetUnit the target unit (must not be null)
   * @return the original and converted recipes
   * @throws UnsupportedConversionException if any ingredient cannot be converted
   */
  @NonNull ConvertResult convert(@NonNull Recipe recipe, @NonNull Unit targetUnit)
      throws UnsupportedConversionException;
}
