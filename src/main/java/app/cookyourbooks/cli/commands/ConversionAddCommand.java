package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.model.Unit;

/** Interactively adds a house conversion rule. */
public final class ConversionAddCommand extends AbstractCommand {

  public ConversionAddCommand() {
    super(
        "conversion add",
        "Add a house conversion rule",
        "conversion add - Interactively add a house conversion rule",
        "Library");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    context.println("Add House Conversion");
    String fromAmountStr = context.readLine("From amount: ");
    String fromUnitStr = context.readLine("From unit: ");
    String ingredientStr = context.readLine("Ingredient (or 'any'): ");
    String toAmountStr = context.readLine("To amount: ");
    String toUnitStr = context.readLine("To unit: ");

    double fromAmount;
    double toAmount;
    try {
      fromAmount = Double.parseDouble(fromAmountStr.trim());
      toAmount = Double.parseDouble(toAmountStr.trim());
    } catch (NumberFormatException e) {
      context.println("Invalid amount. Please enter a number.");
      return;
    }

    Unit fromUnit;
    Unit toUnit;
    try {
      fromUnit = Unit.parse(fromUnitStr.trim());
      toUnit = Unit.parse(toUnitStr.trim());
    } catch (IllegalArgumentException e) {
      context.println(java.util.Objects.requireNonNullElse(e.getMessage(), "Error"));
      return;
    }

    try {
      context
          .librarianService()
          .addHouseConversion(fromAmount, fromUnit, ingredientStr.trim(), toAmount, toUnit);
      String ing = ingredientStr.trim().equalsIgnoreCase("any") ? "any" : ingredientStr.trim();
      context.println(
          "\nAdded: "
              + fromAmount
              + " "
              + fromUnit.getAbbreviation()
              + " "
              + ing
              + " = "
              + toAmount
              + " "
              + toUnit.getAbbreviation());
    } catch (IllegalArgumentException e) {
      context.println(java.util.Objects.requireNonNullElse(e.getMessage(), "Error"));
    }
  }
}
