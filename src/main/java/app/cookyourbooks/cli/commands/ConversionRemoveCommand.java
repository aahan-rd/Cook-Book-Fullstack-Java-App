package app.cookyourbooks.cli.commands;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.conversion.ConversionRule;

/** Removes a house conversion rule. */
public final class ConversionRemoveCommand extends AbstractCommand {

  public ConversionRemoveCommand() {
    super(
        "conversion remove",
        "Remove a house conversion rule",
        "conversion remove <rule> - Remove a house conversion by identifier (e.g. 'stick butter')",
        "Library");
  }

  @Override
  public void execute(@NonNull List<String> args, @NonNull CliContext context) {
    if (args.isEmpty() || args.get(0).isBlank()) {
      context.println("Usage: conversion remove <rule>");
      return;
    }
    String identifier = String.join(" ", args).trim();
    var rules = context.librarianService().listHouseConversions();
    ConversionRule toRemove = null;
    String normalized = identifier.toLowerCase(Locale.ROOT).trim();
    for (ConversionRule r : rules) {
      String ruleId =
          r.fromUnit().getAbbreviation().toLowerCase(Locale.ROOT)
              + " "
              + (r.ingredientName() != null ? r.ingredientName().toLowerCase(Locale.ROOT) : "any");
      if (ruleId.equals(normalized)) {
        toRemove = r;
        break;
      }
    }
    if (toRemove == null) {
      context.println(
          "No conversion found for '" + identifier + "'. Use 'conversions' to see existing rules.");
      return;
    }
    boolean removed = context.librarianService().removeHouseConversion(identifier);
    if (removed) {
      context.println(
          "Removed conversion: 1 "
              + toRemove.fromUnit().getAbbreviation()
              + (toRemove.ingredientName() != null ? " " + toRemove.ingredientName() : " any")
              + " = "
              + toRemove.factor()
              + " "
              + toRemove.toUnit().getAbbreviation());
    }
  }
}
