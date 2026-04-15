package app.cookyourbooks.services;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;

/** Implementation of {@link CookingService} and {@link CookingSession}. */
public final class CookingServiceImpl implements CookingService {

  @Override
  public @NonNull CookingSession startSession(@NonNull Recipe recipe) {
    return new CookingSessionImpl(recipe);
  }

  private static final class CookingSessionImpl implements CookingSession {

    private final Recipe recipe;
    private final List<Instruction> instructions;
    private int currentIndex;
    private boolean finished;

    CookingSessionImpl(Recipe recipe) {
      this.recipe = recipe;
      this.instructions = recipe.getInstructions();
      this.currentIndex = 0;
      this.finished = false;
      if (instructions.isEmpty()) {
        throw new IllegalArgumentException("Recipe has no instructions");
      }
    }

    @Override
    public @NonNull Recipe getRecipe() {
      return recipe;
    }

    @Override
    public int getCurrentStep() {
      return currentIndex + 1;
    }

    @Override
    public int getTotalSteps() {
      return instructions.size();
    }

    @Override
    public @NonNull Instruction getCurrentInstruction() {
      return instructions.get(currentIndex);
    }

    @Override
    public boolean hasNext() {
      return currentIndex < instructions.size() - 1;
    }

    @Override
    public boolean hasPrevious() {
      return currentIndex > 0;
    }

    @Override
    public void next() {
      if (hasNext()) {
        currentIndex++;
      } else {
        finished = true;
      }
    }

    @Override
    public void previous() {
      if (hasPrevious()) {
        currentIndex--;
      }
      finished = false;
    }

    @Override
    public boolean isFinished() {
      return finished;
    }
  }
}
