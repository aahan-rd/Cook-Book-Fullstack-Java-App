package app.cookyourbooks.cli.mode;

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.format.RecipeFormatter;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.CookingSession;

/** Controls the interactive cooking mode: step-by-step navigation. */
public final class CookModeController {

  private static final String PROMPT = "cook> ";
  private static final String HINTS = "[next] [prev] [ingredients] [quit]";

  private final CliContext context;
  private final RecipeFormatter formatter = new RecipeFormatter();

  public CookModeController(CliContext context) {
    this.context = context;
  }

  /**
   * Runs the cooking session until the user quits or finishes.
   *
   * @param recipe the recipe to cook
   */
  public void run(@NonNull Recipe recipe) {
    context.cookModeHolder().setInCookMode(true);
    try {
      CookingSession session = context.cookingService().startSession(recipe);
      context.println(formatter.formatCookHeader(recipe));
      context.println("Ingredients:");
      context.println(formatter.formatIngredientsCompact(recipe));
      displayStep(session);

      while (true) {
        String line = context.readLine(PROMPT);
        if (line == null) {
          break;
        }
        String cmd = line.trim().toLowerCase(Locale.ROOT);
        if (cmd.isEmpty()) {
          continue;
        }

        if (cmd.equals("next") || cmd.equals("n")) {
          if (session.hasNext()) {
            session.next();
            if (session.isFinished()) {
              context.println("  Finished cooking " + recipe.getTitle() + "! Enjoy!");
              return;
            }
            displayStep(session);
          } else {
            session.next();
            if (session.isFinished()) {
              context.println("  Finished cooking " + recipe.getTitle() + "! Enjoy!");
              return;
            }
          }
        } else if (cmd.equals("prev") || cmd.equals("p")) {
          if (session.hasPrevious()) {
            session.previous();
            displayStep(session);
          } else {
            context.println("Already at the beginning.");
          }
        } else if (cmd.equals("ingredients") || cmd.equals("i")) {
          context.println("\nIngredients:");
          for (var ing : recipe.getIngredients()) {
            context.println("  • " + ing.toString());
          }
          context.println("");
        } else if (cmd.equals("quit") || cmd.equals("q")) {
          return;
        }
      }
    } finally {
      context.cookModeHolder().setInCookMode(true);
    }
  }

  private void displayStep(CookingSession session) {
    var instruction = session.getCurrentInstruction();
    context.println("──────────────────────────────────────────");
    context.println("  Step " + session.getCurrentStep() + " of " + session.getTotalSteps());
    context.println("──────────────────────────────────────────");
    context.println("  " + instruction.getText());
    context.println("");
    context.println(formatter.formatConsumedIngredients(instruction));
    context.println("");
    context.println(HINTS);
    context.println("");
  }
}
