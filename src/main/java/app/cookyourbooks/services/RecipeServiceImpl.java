package app.cookyourbooks.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.cookyourbooks.conversion.ConversionRegistry;
import app.cookyourbooks.conversion.ConversionRulePriority;
import app.cookyourbooks.exception.UnsupportedConversionException;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.ShoppingList;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.repository.RecipeCollectionRepository;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.services.parsing.RecipeTextParser;

/**
 * Implementation of {@link RecipeService} that coordinates parsing, transformation, and
 * persistence.
 */
public final class RecipeServiceImpl implements RecipeService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecipeServiceImpl.class);

  private final RecipeRepository recipeRepository;
  private final RecipeCollectionRepository collectionRepository;
  private final ConversionRegistry conversionRegistry;
  private final RecipeTextParser recipeTextParser;
  private final ShoppingListAggregator aggregator;
  private final ObjectMapper objectMapper;

  /**
   * Constructs the service with the required dependencies.
   *
   * @param recipeRepository repository for recipes
   * @param collectionRepository repository for collections
   * @param conversionRegistry registry for unit conversions
   */
  public RecipeServiceImpl(
      RecipeRepository recipeRepository,
      RecipeCollectionRepository collectionRepository,
      ConversionRegistry conversionRegistry) {
    this.recipeRepository = recipeRepository;
    this.collectionRepository = collectionRepository;
    this.conversionRegistry = conversionRegistry;
    this.recipeTextParser = new RecipeTextParser();
    this.aggregator = new ShoppingListAggregator();
    this.objectMapper = createObjectMapper();
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Override
  public @NonNull Recipe importFromJson(@NonNull Path jsonFile, @NonNull String collectionId) {
    RecipeCollection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(() -> new CollectionNotFoundException(collectionId));

    String content;
    try {
      content = Files.readString(jsonFile);
    } catch (IOException e) {
      LOGGER.error("Failed to read file: {}", jsonFile, e);
      throw new ImportException("Could not read file: " + jsonFile, e);
    }

    Recipe recipe;
    try {
      recipe = objectMapper.readValue(content, Recipe.class);
    } catch (IOException e) {
      LOGGER.error("Failed to parse recipe from JSON: {}", jsonFile, e);
      throw new ImportException("Could not parse recipe from " + jsonFile, e);
    }

    recipeRepository.save(recipe);
    RecipeCollection updated = collection.addRecipe(recipe);
    collectionRepository.save(updated);

    LOGGER.info(
        "Imported recipe '{}' from JSON to collection '{}'", recipe.getTitle(), collectionId);
    return recipe;
  }

  @Override
  public @NonNull Recipe importFromText(@NonNull String recipeText, @NonNull String collectionId)
      throws ParseException {
    RecipeCollection collection =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(() -> new CollectionNotFoundException(collectionId));

    LOGGER.debug("Parsing recipe text ({} characters)", recipeText.length());
    Recipe recipe;
    try {
      recipe = recipeTextParser.parse(recipeText);
    } catch (ParseException e) {
      LOGGER.error("Failed to parse recipe text", e);
      throw e;
    }

    LOGGER.debug("Parsed {} ingredients", recipe.getIngredients().size());
    LOGGER.debug("Parsed {} instructions", recipe.getInstructions().size());

    recipeRepository.save(recipe);
    RecipeCollection updated = collection.addRecipe(recipe);
    collectionRepository.save(updated);

    LOGGER.info(
        "Imported recipe '{}' from text to collection '{}'", recipe.getTitle(), collectionId);
    return recipe;
  }

  @Override
  public @NonNull Recipe scaleRecipe(@NonNull String recipeId, int targetServings) {
    if (targetServings <= 0) {
      throw new IllegalArgumentException("targetServings must be positive");
    }

    Recipe original =
        recipeRepository
            .findById(recipeId)
            .orElseThrow(() -> new RecipeNotFoundException(recipeId));

    if (original.getServings() == null) {
      throw new IllegalArgumentException("Recipe has no servings information");
    }

    int originalServings = original.getServings().getAmount();
    double scaleFactor = (double) targetServings / originalServings;
    LOGGER.debug("Scaling factor: {}", scaleFactor);

    Recipe scaled = original.scale(scaleFactor);
    recipeRepository.save(scaled);
    LOGGER.info(
        "Scaled recipe '{}' from {} to {} servings",
        scaled.getTitle(),
        originalServings,
        targetServings);
    return scaled;
  }

  @Override
  public @NonNull Recipe convertRecipe(@NonNull String recipeId, @NonNull Unit targetUnit)
      throws UnsupportedConversionException {
    Recipe original =
        recipeRepository
            .findById(recipeId)
            .orElseThrow(() -> new RecipeNotFoundException(recipeId));

    ConversionRegistry enhanced =
        original.getConversionRules().isEmpty()
            ? conversionRegistry
            : conversionRegistry.withRules(
                original.getConversionRules(), ConversionRulePriority.RECIPE);

    Recipe converted = original.convert(targetUnit, enhanced);
    recipeRepository.save(converted);

    LOGGER.info("Converted recipe '{}' to {}", converted.getTitle(), targetUnit);
    return converted;
  }

  @Override
  public @NonNull ShoppingList generateShoppingList(@NonNull List<String> recipeIds) {
    List<Ingredient> allIngredients = new ArrayList<>();
    for (String id : recipeIds) {
      Recipe recipe =
          recipeRepository.findById(id).orElseThrow(() -> new RecipeNotFoundException(id));
      LOGGER.debug("Aggregating ingredients from recipe '{}'", recipe.getTitle());
      allIngredients.addAll(recipe.getIngredients());
    }

    ShoppingList list = aggregator.aggregate(allIngredients);
    LOGGER.info("Generated shopping list from {} recipes", recipeIds.size());
    return list;
  }

  @Override
  public @NonNull List<Recipe> findByIngredient(@NonNull String ingredientName) {
    List<Recipe> all = recipeRepository.findAll();
    String lower = ingredientName.toLowerCase(Locale.ROOT);
    List<Recipe> matches =
        all.stream()
            .filter(
                r ->
                    r.getIngredients().stream()
                        .anyMatch(i -> i.getName().toLowerCase(Locale.ROOT).contains(lower)))
            .toList();
    LOGGER.info("Found {} recipes containing '{}'", matches.size(), ingredientName);
    return matches;
  }
}
