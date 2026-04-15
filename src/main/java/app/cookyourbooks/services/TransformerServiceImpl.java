package app.cookyourbooks.services;

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.conversion.ConversionRegistry;
import app.cookyourbooks.exception.UnsupportedConversionException;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.Servings;
import app.cookyourbooks.model.Unit;

/**
 * Implementation of {@link TransformerService} using the domain model's scale and convert methods.
 */
public final class TransformerServiceImpl implements TransformerService {

  private final Supplier<ConversionRegistry> registrySupplier;

  /**
   * Constructs a new TransformerServiceImpl.
   *
   * @param registrySupplier supplier for the conversion registry (fetched on each convert call to
   *     pick up house conversion changes)
   */
  public TransformerServiceImpl(@NonNull Supplier<ConversionRegistry> registrySupplier) {
    this.registrySupplier = registrySupplier;
  }

  @Override
  public @NonNull ScaleResult scale(@NonNull Recipe recipe, int targetServings) {
    if (targetServings <= 0) {
      throw new IllegalArgumentException("Invalid servings. Please provide a positive number.");
    }
    Servings s = recipe.getServings();
    if (s == null) {
      throw new IllegalArgumentException(
          "Cannot scale '" + recipe.getTitle() + "': no serving information available.");
    }
    double factor = (double) targetServings / s.getAmount();
    Recipe scaled = recipe.scale(factor);
    return new ScaleResult(recipe, scaled, factor);
  }

  @Override
  public @NonNull ConvertResult convert(@NonNull Recipe recipe, @NonNull Unit targetUnit)
      throws UnsupportedConversionException {
    Recipe converted = recipe.convert(targetUnit, registrySupplier.get());
    return new ConvertResult(recipe, converted);
  }
}
