package app.cookyourbooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.cookyourbooks.conversion.ConversionRegistry;
import app.cookyourbooks.conversion.ConversionRule;
import app.cookyourbooks.conversion.ConversionRulePriority;
import app.cookyourbooks.conversion.LayeredConversionRegistry;
import app.cookyourbooks.conversion.StandardConversions;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.repository.RecipeCollectionRepository;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.repository.RepositoryException;

/**
 * Single-file persistence for CookYourBooks data.
 *
 * <p>Stores all collections (with embedded recipes), recipes, and house conversion rules in a
 * single JSON file. Automatically persists on every mutation.
 */
public final class CybLibrary {

  private static final Logger LOG = LoggerFactory.getLogger(CybLibrary.class);

  private final Path libraryPath;
  private final ObjectMapper objectMapper;

  private final AtomicReference<Map<String, Recipe>> recipesRef;
  private final AtomicReference<List<RecipeCollection>> collectionsRef;
  private final AtomicReference<List<ConversionRule>> houseConversionsRef;
  private final AtomicReference<ConversionRegistry> conversionRegistryRef;

  private CybLibrary(Path libraryPath) {
    this.libraryPath = libraryPath;
    this.objectMapper = createObjectMapper();
    this.recipesRef = new AtomicReference<>(new LinkedHashMap<>());
    this.collectionsRef = new AtomicReference<>(new ArrayList<>());
    this.houseConversionsRef = new AtomicReference<>(new ArrayList<>());
    this.conversionRegistryRef = new AtomicReference<>(buildConversionRegistry(List.of()));
  }

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  private ConversionRegistry buildConversionRegistry(List<ConversionRule> houseRules) {
    return new LayeredConversionRegistry()
        .withRules(StandardConversions.getAllRules(), ConversionRulePriority.STANDARD)
        .withRules(houseRules, ConversionRulePriority.HOUSE);
  }

  /**
   * Loads the library from the given path, or creates an empty library if the file does not exist.
   *
   * @param libraryPath path to cyb-library.json
   * @return the loaded or newly created CybLibrary
   */
  public static CybLibrary load(Path libraryPath) {
    CybLibrary library = new CybLibrary(libraryPath);
    if (Files.exists(libraryPath)) {
      library.loadFromFile();
    }
    return library;
  }

  private void loadFromFile() {
    try {
      CybLibraryData data = objectMapper.readValue(libraryPath.toFile(), CybLibraryData.class);
      Map<String, Recipe> recipes = new LinkedHashMap<>();
      List<RecipeCollection> collections = data.collections();
      for (RecipeCollection coll : collections) {
        for (Recipe r : coll.getRecipes()) {
          recipes.put(r.getId(), r);
        }
      }
      recipesRef.set(recipes);
      collectionsRef.set(new ArrayList<>(collections));
      List<ConversionRule> houseRules =
          data.houseConversions() != null ? data.houseConversions() : List.of();
      houseConversionsRef.set(new ArrayList<>(houseRules));
      conversionRegistryRef.set(buildConversionRegistry(houseRules));
    } catch (IOException e) {
      throw new RepositoryException("Failed to load library from " + libraryPath, e);
    }
  }

  private void persist() {
    try {
      List<RecipeCollection> collections = new ArrayList<>(collectionsRef.get());
      List<ConversionRule> houseRules = new ArrayList<>(houseConversionsRef.get());
      CybLibraryData data = new CybLibraryData(collections, houseRules);
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(libraryPath.toFile(), data);
    } catch (IOException e) {
      LOG.error("Failed to save library: {}", e);
      System.err.println(
          "Warning: Failed to save changes to cyb-library.json: "
              + e.getMessage()
              + ". Your changes may be lost.");
    }
  }

  /** Returns the recipe repository backed by this library. */
  public RecipeRepository getRecipeRepository() {
    return new CybRecipeRepository();
  }

  /** Returns the collection repository backed by this library. */
  public RecipeCollectionRepository getCollectionRepository() {
    return new CybCollectionRepository();
  }

  /** Returns the conversion registry including house rules. */
  public ConversionRegistry getConversionRegistry() {
    ConversionRegistry reg = conversionRegistryRef.get();
    if (reg == null) {
      throw new IllegalStateException("Conversion registry not initialized");
    }
    return reg;
  }

  /** Adds a house conversion rule and persists. */
  public void addHouseConversion(ConversionRule rule) {
    List<ConversionRule> rules = new ArrayList<>(houseConversionsRef.get());
    rules.add(rule);
    houseConversionsRef.set(rules);
    conversionRegistryRef.set(buildConversionRegistry(rules));
    persist();
  }

  /**
   * Removes a house conversion rule by identifier.
   *
   * <p>Identifier format: "{from-unit} {ingredient}" e.g. "stick butter", "cup flour". Use "any"
   * for universal conversions e.g. "tbsp any".
   *
   * @param identifier the rule identifier
   * @return true if a rule was removed, false if no matching rule found
   */
  public boolean removeHouseConversion(String identifier) {
    String normalized = identifier.trim().toLowerCase(Locale.ROOT);
    List<ConversionRule> rules = new ArrayList<>(houseConversionsRef.get());
    boolean removed =
        rules.removeIf(
            r -> {
              String ruleId =
                  r.fromUnit().getAbbreviation().toLowerCase(Locale.ROOT)
                      + " "
                      + (r.ingredientName() != null
                          ? r.ingredientName().toLowerCase(Locale.ROOT)
                          : "any");
              return ruleId.equals(normalized);
            });
    if (removed) {
      houseConversionsRef.set(rules);
      conversionRegistryRef.set(buildConversionRegistry(rules));
      persist();
    }
    return removed;
  }

  /** Returns all house conversion rules. */
  public List<ConversionRule> getHouseConversions() {
    return List.copyOf(houseConversionsRef.get());
  }

  private record CybLibraryData(
      List<RecipeCollection> collections, List<ConversionRule> houseConversions) {}

  private final class CybRecipeRepository implements RecipeRepository {

    @Override
    public void save(Recipe recipe) {
      Map<String, Recipe> recipes = new LinkedHashMap<>(Objects.requireNonNull(recipesRef.get()));
      recipes.put(recipe.getId(), recipe);
      recipesRef.set(recipes);

      List<RecipeCollection> collections =
          new ArrayList<>(Objects.requireNonNull(collectionsRef.get()));
      for (int i = 0; i < collections.size(); i++) {
        RecipeCollection coll = collections.get(i);
        if (coll.containsRecipe(recipe.getId())) {
          collections.set(i, coll.removeRecipe(recipe.getId()).addRecipe(recipe));
        }
      }
      collectionsRef.set(collections);
      persist();
    }

    @Override
    public Optional<Recipe> findById(String id) {
      Map<String, Recipe> recipes = recipesRef.get();
      return recipes != null ? Optional.ofNullable(recipes.get(id)) : Optional.empty();
    }

    @Override
    public Optional<Recipe> findByTitle(String title) {
      Map<String, Recipe> recipes = recipesRef.get();
      return recipes != null
          ? recipes.values().stream().filter(r -> r.getTitle().equalsIgnoreCase(title)).findFirst()
          : Optional.empty();
    }

    @Override
    public List<Recipe> findAllByTitle(String title) {
      Map<String, Recipe> recipes = recipesRef.get();
      return recipes != null
          ? recipes.values().stream().filter(r -> r.getTitle().equalsIgnoreCase(title)).toList()
          : List.of();
    }

    @Override
    public List<Recipe> findAll() {
      Map<String, Recipe> recipes = recipesRef.get();
      return recipes != null ? List.copyOf(recipes.values()) : List.of();
    }

    @Override
    public void delete(String id) {
      Map<String, Recipe> recipes = new LinkedHashMap<>(Objects.requireNonNull(recipesRef.get()));
      recipes.remove(id);
      recipesRef.set(recipes);

      List<RecipeCollection> collections =
          new ArrayList<>(Objects.requireNonNull(collectionsRef.get()));
      for (int i = 0; i < collections.size(); i++) {
        RecipeCollection coll = collections.get(i);
        if (coll.containsRecipe(id)) {
          collections.set(i, coll.removeRecipe(id));
        }
      }
      collectionsRef.set(collections);
      persist();
    }
  }

  private final class CybCollectionRepository implements RecipeCollectionRepository {

    @Override
    public void save(RecipeCollection collection) {
      Map<String, Recipe> recipes = new LinkedHashMap<>(Objects.requireNonNull(recipesRef.get()));
      for (Recipe r : collection.getRecipes()) {
        recipes.put(r.getId(), r);
      }
      recipesRef.set(recipes);

      List<RecipeCollection> collections =
          new ArrayList<>(Objects.requireNonNull(collectionsRef.get()));
      collections.removeIf(c -> c.getId().equals(collection.getId()));
      collections.add(collection);
      collectionsRef.set(collections);
      persist();
    }

    @Override
    public Optional<RecipeCollection> findById(String id) {
      List<RecipeCollection> collections = collectionsRef.get();
      return collections != null
          ? collections.stream().filter(c -> c.getId().equals(id)).findFirst()
          : Optional.empty();
    }

    @Override
    public Optional<RecipeCollection> findByTitle(String title) {
      List<RecipeCollection> collections = collectionsRef.get();
      return collections != null
          ? collections.stream().filter(c -> c.getTitle().equalsIgnoreCase(title)).findFirst()
          : Optional.empty();
    }

    @Override
    public List<RecipeCollection> findAllByTitle(String title) {
      List<RecipeCollection> collections = collectionsRef.get();
      return collections != null
          ? collections.stream().filter(c -> c.getTitle().equalsIgnoreCase(title)).toList()
          : List.of();
    }

    @Override
    public List<RecipeCollection> findAll() {
      List<RecipeCollection> collections = collectionsRef.get();
      return collections != null ? List.copyOf(collections) : List.of();
    }

    @Override
    public void delete(String id) {
      List<RecipeCollection> collections =
          new ArrayList<>(Objects.requireNonNull(collectionsRef.get()));
      collections.removeIf(c -> c.getId().equals(id));
      collectionsRef.set(collections);
      persist();
    }
  }
}
