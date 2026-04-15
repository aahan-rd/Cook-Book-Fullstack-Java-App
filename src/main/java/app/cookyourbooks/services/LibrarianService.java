package app.cookyourbooks.services;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.conversion.ConversionRule;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;
import app.cookyourbooks.model.Unit;

/**
 * Service for the Librarian actor: collection and recipe management, import, search, house
 * conversions.
 */
public interface LibrarianService {

  /** Lists all recipe collections. */
  @NonNull List<RecipeCollection> listCollections();

  /** Lists all recipes across all collections. */
  @NonNull List<Recipe> listAllRecipes();

  /**
   * Creates a new personal collection with the given title.
   *
   * @param name the collection title (must not be blank)
   * @return the created collection
   */
  @NonNull RecipeCollection createCollection(@NonNull String name);

  /**
   * Creates a new collection with the given title and source type.
   *
   * @param name the collection title (must not be blank)
   * @param sourceType the type of collection to create
   * @return the created collection
   */
  @NonNull RecipeCollection createCollection(@NonNull String name, @NonNull SourceType sourceType);

  /**
   * Finds a collection by its unique identifier.
   *
   * @param collectionId the collection ID
   * @return the collection if found
   */
  Optional<RecipeCollection> findCollectionById(@NonNull String collectionId);

  /**
   * Deletes a collection by its unique identifier.
   *
   * @param collectionId the collection ID
   */
  void deleteCollection(@NonNull String collectionId);

  /**
   * Finds a collection by title (case-insensitive).
   *
   * @param name the collection title
   * @return the collection if found
   */
  @NonNull List<RecipeCollection> findAllCollectionsByTitle(@NonNull String name);

  /** Lists all recipes in the specified collection. */
  @NonNull List<Recipe> listRecipes(@NonNull String collectionName);

  /**
   * Finds a recipe by title (case-insensitive). Returns any one match if multiple exist.
   *
   * @param title the recipe title
   * @return the recipe if found
   */
  Optional<Recipe> findRecipe(@NonNull String title);

  /**
   * Finds all recipes with the given title (for ambiguous match handling).
   *
   * @param title the recipe title
   * @return all matching recipes
   */
  @NonNull List<Recipe> findAllRecipesByTitle(@NonNull String title);

  /**
   * Resolves a recipe query using spec lookup order: (1) short ID prefix (case-insensitive), then
   * (2) title substring (case-insensitive). Short ID is the first 8 characters of the recipe's
   * internal ID.
   *
   * @param query the user's search string (short ID prefix or title substring)
   * @return matching recipes (empty if none; singleton if unique; multiple if ambiguous)
   */
  @NonNull List<Recipe> resolveRecipes(@NonNull String query);

  /** Searches recipes by ingredient name (case-insensitive substring match). */
  @NonNull List<Recipe> searchByIngredient(@NonNull String ingredientName);

  /** Deletes a recipe by ID and removes it from all collections. */
  void deleteRecipe(@NonNull String recipeId);

  /** Removes a recipe from one collection without deleting it from the library. */
  void removeRecipeFromCollection(@NonNull String collectionId, @NonNull String recipeId);

  /**
   * Saves a recipe and adds it to the specified collection.
   *
   * @param recipe the recipe to save
   * @param collectionId the collection ID to add it to
   */
  void saveRecipe(@NonNull Recipe recipe, @NonNull String collectionId);

  /**
   * Imports a recipe from a JSON file into the specified collection.
   *
   * @param file path to the JSON file
   * @param collectionName collection title (case-insensitive)
   * @return the imported recipe
   */
  @NonNull Recipe importFromJson(@NonNull Path file, @NonNull String collectionName);

  /** Lists all house conversion rules. */
  @NonNull List<ConversionRule> listHouseConversions();

  /**
   * Adds a house conversion rule.
   *
   * @param fromAmount from quantity amount
   * @param fromUnit from unit
   * @param ingredientName ingredient name or "any" for universal
   * @param toAmount to quantity amount
   * @param toUnit to unit
   * @throws IllegalArgumentException if amounts are invalid or rule is duplicate
   */
  void addHouseConversion(
      double fromAmount,
      @NonNull Unit fromUnit,
      @NonNull String ingredientName,
      double toAmount,
      @NonNull Unit toUnit);

  /**
   * Removes a house conversion rule by identifier.
   *
   * @param identifier e.g. "stick butter", "cup flour", "tbsp any"
   * @return true if a rule was removed
   */
  boolean removeHouseConversion(@NonNull String identifier);

  List<String> listAllIngredients();
}
