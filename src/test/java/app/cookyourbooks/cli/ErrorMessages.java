package app.cookyourbooks.cli;

/** Exact error messages from cyb5.md spec. */
public final class ErrorMessages {

  private ErrorMessages() {}

  public static String recipeNotFound(String title) {
    return "Recipe not found: '" + title + "'. Use 'search' to find recipes by ingredient.";
  }

  public static String collectionNotFound(String title) {
    return "Collection not found: '" + title + "'. Use 'collections' to see available collections.";
  }
}
