package app.cookyourbooks.gui.export;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.RecipeFixtures;
import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;

class RecipePdfExporterTest {

  @Test
  void exportToPdf_createsFile() throws IOException {
    Recipe recipe = recipeWithIngredientsAndSteps();
    Path outputPath = Files.createTempFile("recipe-export-", ".pdf");

    RecipePdfExporter.exportToPdf(recipe, outputPath);

    assertTrue(Files.exists(outputPath));
  }

  @Test
  void exportToPdf_containsTitle() throws IOException {
    Recipe recipe = recipeWithIngredientsAndSteps();
    Path outputPath = Files.createTempFile("recipe-title-", ".pdf");

    RecipePdfExporter.exportToPdf(recipe, outputPath);

    String extractedText = extractPdfText(outputPath);
    assertTrue(extractedText.contains(recipe.getTitle()));
  }

  @Test
  void exportToPdf_containsIngredients() throws IOException {
    Recipe recipe = recipeWithIngredientsAndSteps();
    Path outputPath = Files.createTempFile("recipe-ingredients-", ".pdf");

    RecipePdfExporter.exportToPdf(recipe, outputPath);

    String extractedText = extractPdfText(outputPath);
    assertTrue(extractedText.contains("flour"));
  }

  @Test
  void exportToPdf_containsSteps() throws IOException {
    Recipe recipe = recipeWithIngredientsAndSteps();
    Path outputPath = Files.createTempFile("recipe-steps-", ".pdf");

    RecipePdfExporter.exportToPdf(recipe, outputPath);

    String extractedText = extractPdfText(outputPath);
    assertTrue(extractedText.contains("Whisk the batter until smooth."));
  }

  @Test
  void exportToPdf_emptyIngredientsAndSteps() throws IOException {
    Recipe recipe = RecipeFixtures.makeRecipe("r-empty", "Empty Recipe");
    Path outputPath = Files.createTempFile("recipe-empty-", ".pdf");

    assertDoesNotThrow(() -> RecipePdfExporter.exportToPdf(recipe, outputPath));
    assertTrue(Files.exists(outputPath));
  }

  private static Recipe recipeWithIngredientsAndSteps() {
    Ingredient flour = RecipeFixtures.measuredCups("flour", 2.0);
    Ingredient salt = RecipeFixtures.vague("salt", "to taste");

    List<Instruction> instructions =
        List.of(
            new Instruction(1, "Mix flour and salt in a bowl.", List.of()),
            new Instruction(2, "Whisk the batter until smooth.", List.of()));

    return new Recipe(
        "r-pdf", "Simple Pancakes", null, List.of(flour, salt), instructions, List.of());
  }

  private static String extractPdfText(Path outputPath) throws IOException {
    try (PDDocument document = Loader.loadPDF(outputPath.toFile())) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    }
  }
}
