package app.cookyourbooks.services;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Recipe;

/**
 * Service for the Cook actor: step-by-step cooking mode.
 *
 * <p>Creates cooking sessions that manage navigation through recipe instructions.
 */
public interface CookingService {

  /**
   * Starts a new cooking session for the given recipe.
   *
   * @param recipe the recipe to cook (must not be null, must have at least one instruction)
   * @return a new CookingSession
   */
  @NonNull CookingSession startSession(@NonNull Recipe recipe);
}
