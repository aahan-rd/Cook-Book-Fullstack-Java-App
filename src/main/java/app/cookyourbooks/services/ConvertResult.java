package app.cookyourbooks.services;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Recipe;

/**
 * Result of converting a recipe to a target unit.
 *
 * <p>Contains both the original and converted recipes. Does not persist the converted recipe — the
 * CLI decides whether to save.
 */
public record ConvertResult(@NonNull Recipe original, @NonNull Recipe converted) {}
