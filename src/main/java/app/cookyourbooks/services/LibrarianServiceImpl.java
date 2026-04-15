package app.cookyourbooks.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.cookyourbooks.CybLibrary;
import app.cookyourbooks.conversion.ConversionRule;
import app.cookyourbooks.model.CookbookImpl;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.PersonalCollectionImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.SourceType;
import app.cookyourbooks.model.Unit;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.model.WebCollectionImpl;
import app.cookyourbooks.repository.RecipeCollectionRepository;
import app.cookyourbooks.repository.RecipeRepository;

/** Implementation of {@link LibrarianService} for the Librarian actor. */
public final class LibrarianServiceImpl implements LibrarianService {

  private final RecipeRepository recipeRepository;
  private final RecipeCollectionRepository collectionRepository;
  private final CybLibrary library;
  private final ObjectMapper objectMapper;

  /**
   * Constructs a new LibrarianServiceImpl.
   *
   * @param recipeRepository recipe repository
   * @param collectionRepository collection repository
   * @param library CybLibrary for house conversion management
   */
  public LibrarianServiceImpl(
      RecipeRepository recipeRepository,
      RecipeCollectionRepository collectionRepository,
      CybLibrary library) {
    this.recipeRepository = recipeRepository;
    this.collectionRepository = collectionRepository;
    this.library = library;
    this.objectMapper = createObjectMapper();
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Override
  public @NonNull List<RecipeCollection> listCollections() {
    return collectionRepository.findAll();
  }

  @Override
  public @NonNull List<Recipe> listAllRecipes() {
    return recipeRepository.findAll();
  }

  @Override
  public @NonNull RecipeCollection createCollection(@NonNull String name) {
    return createCollection(name, SourceType.PERSONAL);
  }

  @Override
  public @NonNull RecipeCollection createCollection(
      @NonNull String name, @NonNull SourceType sourceType) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Collection name cannot be blank");
    }
    String trimmed = name.trim();
    RecipeCollection collection =
        switch (sourceType) {
          case PERSONAL -> PersonalCollectionImpl.builder().title(trimmed).build();
          case PUBLISHED_BOOK -> CookbookImpl.builder().title(trimmed).build();
          case WEBSITE ->
              WebCollectionImpl.builder()
                  .title(trimmed)
                  .sourceUrl(java.net.URI.create("https://example.com"))
                  .build();
        };
    collectionRepository.save(collection);
    return collection;
  }

  @Override
  public Optional<RecipeCollection> findCollectionById(@NonNull String collectionId) {
    return collectionRepository.findById(collectionId);
  }

  @Override
  public void deleteCollection(@NonNull String collectionId) {
    collectionRepository.delete(collectionId);
  }

  @Override
  public @NonNull List<RecipeCollection> findAllCollectionsByTitle(@NonNull String name) {
    return collectionRepository.findAllByTitle(name);
  }

  @Override
  public @NonNull List<Recipe> listRecipes(@NonNull String collectionName) {
    RecipeCollection coll = findSingleCollection(collectionName);
    return coll.getRecipes();
  }

  @Override
  public @NonNull Optional<Recipe> findRecipe(@NonNull String title) {
    return recipeRepository.findByTitle(title);
  }

  @Override
  public @NonNull List<Recipe> findAllRecipesByTitle(@NonNull String title) {
    return recipeRepository.findAllByTitle(title);
  }

  @Override
  public @NonNull List<Recipe> resolveRecipes(@NonNull String query) {
    String q = query.trim();
    if (q.isEmpty()) {
      return List.of();
    }
    String lower = q.toLowerCase(Locale.ROOT);
    List<Recipe> all = recipeRepository.findAll();

    // Step 1: Try short ID prefix (case-insensitive)
    List<Recipe> byId =
        all.stream().filter(r -> r.getId().toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    if (!byId.isEmpty()) {
      return byId;
    }

    // Step 2: Fall back to title substring (case-insensitive)
    return all.stream().filter(r -> r.getTitle().toLowerCase(Locale.ROOT).contains(lower)).toList();
  }

  @Override
  public @NonNull List<Recipe> searchByIngredient(@NonNull String ingredientName) {
    String lower = ingredientName.toLowerCase(Locale.ROOT);
    return recipeRepository.findAll().stream()
        .filter(
            r ->
                r.getIngredients().stream()
                    .anyMatch(i -> i.getName().toLowerCase(Locale.ROOT).contains(lower)))
        .toList();
  }

  @Override
  public void deleteRecipe(@NonNull String recipeId) {
    recipeRepository.delete(recipeId);
  }

  @Override
  public void removeRecipeFromCollection(@NonNull String collectionId, @NonNull String recipeId) {
    RecipeCollection coll =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(() -> new CollectionNotFoundException(collectionId));
    RecipeCollection updated = coll.removeRecipe(recipeId);
    collectionRepository.save(updated);
  }

  @Override
  public void saveRecipe(@NonNull Recipe recipe, @NonNull String collectionId) {
    recipeRepository.save(recipe);
    RecipeCollection coll =
        collectionRepository
            .findById(collectionId)
            .orElseThrow(() -> new CollectionNotFoundException(collectionId));
    RecipeCollection updated = coll.addRecipe(recipe);
    collectionRepository.save(updated);
  }

  @Override
  public @NonNull Recipe importFromJson(@NonNull Path file, @NonNull String collectionName) {
    RecipeCollection coll = findSingleCollection(collectionName);
    String content;
    try {
      content = Files.readString(file);
    } catch (IOException e) {
      throw new ImportException("Could not read file: " + file, e);
    }
    Recipe recipe;
    try {
      recipe = objectMapper.readValue(content, Recipe.class);
    } catch (IOException e) {
      throw new ImportException("Could not parse recipe from " + file, e);
    }
    recipeRepository.save(recipe);
    RecipeCollection updated = coll.addRecipe(recipe);
    collectionRepository.save(updated);
    return recipe;
  }

  @Override
  public @NonNull List<ConversionRule> listHouseConversions() {
    return library.getHouseConversions();
  }

  @Override
  public void addHouseConversion(
      double fromAmount,
      @NonNull Unit fromUnit,
      @NonNull String ingredientName,
      double toAmount,
      @NonNull Unit toUnit) {
    if (fromAmount <= 0 || toAmount <= 0) {
      throw new IllegalArgumentException("Invalid amount. Please enter a number.");
    }
    String ing = ingredientName.trim().equalsIgnoreCase("any") ? null : ingredientName.trim();
    double factor = toAmount / fromAmount;
    ConversionRule rule = new ConversionRule(fromUnit, toUnit, factor, ing);

    List<ConversionRule> existing = library.getHouseConversions();
    String newId =
        fromUnit.getAbbreviation().toLowerCase(Locale.ROOT)
            + " "
            + (ing != null ? ing.toLowerCase(Locale.ROOT) : "any");
    for (ConversionRule r : existing) {
      String existingId =
          r.fromUnit().getAbbreviation().toLowerCase(Locale.ROOT)
              + " "
              + (r.ingredientName() != null ? r.ingredientName().toLowerCase(Locale.ROOT) : "any");
      if (existingId.equals(newId)) {
        String displayId =
            ing != null
                ? fromUnit.getAbbreviation() + " " + ing
                : fromUnit.getAbbreviation() + " any";
        throw new IllegalArgumentException(
            "A conversion for '" + displayId + "' already exists. Remove it first to replace.");
      }
    }
    library.addHouseConversion(rule);
  }

  @Override
  public boolean removeHouseConversion(@NonNull String identifier) {
    return library.removeHouseConversion(identifier);
  }

  private RecipeCollection findSingleCollection(String name) {
    List<RecipeCollection> matches = collectionRepository.findAllByTitle(name);
    if (matches.isEmpty()) {
      throw new CollectionNotFoundException(
          "'" + name + "'. Use 'collections' to see available collections.");
    }
    if (matches.size() > 1) {
      throw new IllegalArgumentException(
          "Multiple collections match. Please specify the full collection name.");
    }
    return matches.get(0);
  }

  @Override
  public List<String> listAllIngredients() {
    return collectionRepository.findAll().stream()
        .flatMap(collection -> collection.getRecipes().stream())
        .flatMap(recipe -> recipe.getIngredients().stream())
        .map(
            ingredient -> {
              if (ingredient instanceof MeasuredIngredient mi) {
                return mi.getName();
              } else if (ingredient instanceof VagueIngredient vi) {
                return vi.getName();
              }
              return ingredient.toString();
            })
        .distinct()
        .sorted()
        .toList();
  }
}
