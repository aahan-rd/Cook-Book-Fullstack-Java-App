package app.cookyourbooks.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.cookyourbooks.adapters.gemini.GeminiAuthException;
import app.cookyourbooks.adapters.gemini.GeminiClient;
import app.cookyourbooks.adapters.gemini.GeminiRateLimitException;
import app.cookyourbooks.model.MeasuredIngredient;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.services.ocr.OcrException;

/** Tests for {@link GeminiOcrAdapter}. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway.Init")
class GeminiOcrAdapterTest {

  @Mock private GeminiClient mockClient;

  private GeminiOcrAdapter adapter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    adapter = new GeminiOcrAdapter(mockClient);
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void extractRecipe_parsesScrambledEggsFromGeminiFormat() throws Exception {
    // Gemini returns JSON in its prompt format (yield, value, consumedIngredients)
    String geminiResponse =
        """
        {
          "recipes": [{
            "title": "SIMPLE SCRAMBLED EGGS",
            "yield": { "type": "exact", "value": 2 },
            "ingredients": [
              {
                "type": "measured",
                "name": "eggs",
                "quantity": { "type": "exact", "value": 3.0, "unit": "WHOLE" }
              },
              {
                "type": "measured",
                "name": "milk",
                "quantity": { "type": "exact", "value": 2.0, "unit": "TABLESPOON" }
              },
              {
                "type": "measured",
                "name": "butter",
                "quantity": { "type": "exact", "value": 1.0, "unit": "TABLESPOON" }
              }
            ],
            "instructions": [
              {
                "stepNumber": 1,
                "text": "Beat eggs and milk together in a bowl",
                "consumedIngredients": [
                  { "ingredientName": "eggs", "quantity": { "type": "exact", "value": 3.0, "unit": "WHOLE" } },
                  { "ingredientName": "milk", "quantity": { "type": "exact", "value": 2.0, "unit": "TABLESPOON" } }
                ]
              },
              {
                "stepNumber": 2,
                "text": "Melt butter in a pan over medium heat",
                "consumedIngredients": [
                  { "ingredientName": "butter", "quantity": { "type": "exact", "value": 1.0, "unit": "TABLESPOON" } }
                ]
              }
            ]
          }]
        }
        """;

    when(mockClient.extractRecipeJson(any())).thenReturn(geminiResponse);

    Recipe recipe = adapter.extractRecipe(Path.of("test-image.jpg"));

    // Verify title
    assertThat(recipe.getTitle()).isEqualTo("SIMPLE SCRAMBLED EGGS");

    // Verify servings (transformed from yield)
    assertThat(recipe.getServings()).isNotNull();
    assertThat(Objects.requireNonNull(recipe.getServings()).getAmount()).isEqualTo(2);

    // Verify ingredients count
    assertThat(recipe.getIngredients()).hasSize(3);

    // Verify first ingredient (eggs)
    var eggs = recipe.getIngredients().get(0);
    assertThat(eggs.getName()).isEqualTo("eggs");
    assertThat(eggs).isInstanceOf(MeasuredIngredient.class);

    // Verify instructions
    assertThat(recipe.getInstructions()).hasSize(2);
    assertThat(recipe.getInstructions().get(0).getText())
        .isEqualTo("Beat eggs and milk together in a bowl");

    // Verify ingredientRefs (transformed from consumedIngredients)
    var step1Refs = recipe.getInstructions().get(0).getIngredientRefs();
    assertThat(step1Refs).hasSize(2);
    assertThat(step1Refs.get(0).ingredient().getName()).isEqualTo("eggs");
  }

  @Test
  void extractRecipe_matchesSampleRecipeJson() throws Exception {
    // Load expected recipe from sample-recipes
    Path expectedPath = Path.of("sample-recipes/easy/Scrambled-eggs.json");
    Recipe expected = objectMapper.readValue(expectedPath.toFile(), Recipe.class);

    // Gemini response that should produce a similar recipe
    String geminiResponse =
        """
        {
          "recipes": [{
            "title": "SIMPLE SCRAMBLED EGGS",
            "yield": { "type": "exact", "value": 2 },
            "ingredients": [
              { "type": "measured", "name": "eggs", "quantity": { "type": "exact", "value": 3.0, "unit": "WHOLE" } },
              { "type": "measured", "name": "milk", "quantity": { "type": "exact", "value": 2.0, "unit": "TABLESPOON" } },
              { "type": "measured", "name": "butter", "quantity": { "type": "exact", "value": 1.0, "unit": "TABLESPOON" } },
              { "type": "vague", "name": "salt", "description": "to taste" },
              { "type": "vague", "name": "pepper", "description": "to taste" }
            ],
            "instructions": [
              { "stepNumber": 1, "text": "Beat eggs and milk together in a bowl", "consumedIngredients": [
                { "ingredientName": "eggs", "quantity": { "type": "exact", "value": 3.0, "unit": "WHOLE" } },
                { "ingredientName": "milk", "quantity": { "type": "exact", "value": 2.0, "unit": "TABLESPOON" } }
              ]},
              { "stepNumber": 2, "text": "Melt butter in a pan over medium heat", "consumedIngredients": [
                { "ingredientName": "butter", "quantity": { "type": "exact", "value": 1.0, "unit": "TABLESPOON" } }
              ]},
              { "stepNumber": 3, "text": "Pour in egg mixture", "consumedIngredients": [
                { "ingredientName": "eggs", "quantity": { "type": "exact", "value": 3.0, "unit": "WHOLE" } },
                { "ingredientName": "milk", "quantity": { "type": "exact", "value": 2.0, "unit": "TABLESPOON" } }
              ]},
              { "stepNumber": 4, "text": "Stir gently until eggs are set but still moist", "consumedIngredients": [
                { "ingredientName": "eggs", "quantity": { "type": "exact", "value": 3.0, "unit": "WHOLE" } }
              ]},
              { "stepNumber": 5, "text": "Season with salt and pepper", "consumedIngredients": [
                { "ingredientName": "salt", "vague": true },
                { "ingredientName": "pepper", "vague": true }
              ]},
              { "stepNumber": 6, "text": "Serve immediately", "consumedIngredients": [] }
            ]
          }]
        }
        """;

    when(mockClient.extractRecipeJson(any())).thenReturn(geminiResponse);

    Recipe actual = adapter.extractRecipe(Path.of("test-image.jpg"));

    // Verify key fields match the expected sample recipe
    assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
    assertThat(actual.getServings()).isNotNull();
    assertThat(Objects.requireNonNull(actual.getServings()).getAmount())
        .isEqualTo(Objects.requireNonNull(expected.getServings()).getAmount());
    assertThat(actual.getIngredients()).hasSameSizeAs(expected.getIngredients());
    assertThat(actual.getInstructions()).hasSameSizeAs(expected.getInstructions());

    // Verify ingredient names match
    for (int i = 0; i < expected.getIngredients().size(); i++) {
      assertThat(actual.getIngredients().get(i).getName())
          .isEqualTo(expected.getIngredients().get(i).getName());
    }

    // Verify instruction texts match
    for (int i = 0; i < expected.getInstructions().size(); i++) {
      assertThat(actual.getInstructions().get(i).getText())
          .isEqualTo(expected.getInstructions().get(i).getText());
    }
  }

  @Test
  void extractRecipe_handlesMarkdownCodeFence() throws Exception {
    String geminiResponse =
        """
        ```json
        {
          "recipes": [{
            "title": "Test Recipe",
            "yield": { "type": "exact", "value": 4 },
            "ingredients": [],
            "instructions": []
          }]
        }
        ```
        """;

    when(mockClient.extractRecipeJson(any())).thenReturn(geminiResponse);

    Recipe recipe = adapter.extractRecipe(Path.of("test.jpg"));

    assertThat(recipe.getTitle()).isEqualTo("Test Recipe");
  }

  @Test
  void extractRecipe_translatesAuthExceptionToOcrException() throws Exception {
    when(mockClient.extractRecipeJson(any())).thenThrow(new GeminiAuthException());

    assertThatThrownBy(() -> adapter.extractRecipe(Path.of("test.jpg")))
        .isInstanceOf(OcrException.class)
        .hasMessageContaining("API key error");
  }

  @Test
  void extractRecipe_translatesRateLimitExceptionToOcrException() throws Exception {
    when(mockClient.extractRecipeJson(any())).thenThrow(new GeminiRateLimitException());

    assertThatThrownBy(() -> adapter.extractRecipe(Path.of("test.jpg")))
        .isInstanceOf(OcrException.class)
        .hasMessageContaining("rate limit");
  }

  @Test
  void extractRecipe_throwsOcrExceptionForEmptyRecipesArray() throws Exception {
    String emptyResponse =
        """
        { "recipes": [] }
        """;

    when(mockClient.extractRecipeJson(any())).thenReturn(emptyResponse);

    assertThatThrownBy(() -> adapter.extractRecipe(Path.of("test.jpg")))
        .isInstanceOf(OcrException.class)
        .hasMessageContaining("Could not extract a recipe");
  }
}
