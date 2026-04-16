package app.cookyourbooks.gui.export;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import app.cookyourbooks.model.Ingredient;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;

/** Utility for exporting recipes into a simple PDF layout. */
public final class RecipePdfExporter {

  private static final float MARGIN_X = 50F;
  private static final float BOTTOM_MARGIN = 50F;
  private static final float TITLE_FONT_SIZE = 18F;
  private static final float HEADER_FONT_SIZE = 14F;
  private static final float BODY_FONT_SIZE = 12F;
  private static final float TITLE_SPACING = 28F;
  private static final float HEADER_SPACING = 20F;
  private static final float LINE_SPACING = 16F;
  private static final String REGULAR_FONT_RESOURCE =
      "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf";
  private static final String BOLD_FONT_RESOURCE = REGULAR_FONT_RESOURCE;

  // If a character isn't supported by the embedded font, replace it with this.
  private static final char UNSUPPORTED_GLYPH_REPLACEMENT = '?';

  private RecipePdfExporter() {
    // Utility class.
  }

  /** Exports the given recipe to a PDF file at the provided output path. */
  public static void exportToPdf(Recipe recipe, Path outputPath) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDFont regularFont = loadEmbeddedFont(document, REGULAR_FONT_RESOURCE);
      PDFont boldFont = loadEmbeddedFont(document, BOLD_FONT_RESOURCE);

      try (PdfWriteState state = new PdfWriteState(document, PDRectangle.LETTER)) {
        state.ensurePageSpace();
        writeLine(state.contentStream, recipe.getTitle(), boldFont, TITLE_FONT_SIZE, state.y);
        state.y -= TITLE_SPACING;

        state.ensurePageSpace();
        writeLine(state.contentStream, "Ingredients:", boldFont, HEADER_FONT_SIZE, state.y);
        state.y -= HEADER_SPACING;

        for (Ingredient ingredient : recipe.getIngredients()) {
          state.ensurePageSpace();
          writeLine(
              state.contentStream, ingredient.getName(), regularFont, BODY_FONT_SIZE, state.y);
          state.y -= LINE_SPACING;
        }

        state.y -= 6F;
        state.ensurePageSpace();
        writeLine(state.contentStream, "Steps:", boldFont, HEADER_FONT_SIZE, state.y);
        state.y -= HEADER_SPACING;

        int instructionNumber = 1;
        for (Instruction instruction : recipe.getInstructions()) {
          state.ensurePageSpace();
          String line = instructionNumber + ". " + instruction.getText();
          writeLine(state.contentStream, line, regularFont, BODY_FONT_SIZE, state.y);
          state.y -= LINE_SPACING;
          instructionNumber++;
        }
      }

      document.save(outputPath.toFile());
    }
  }

  private static void writeLine(
      PDPageContentStream contentStream, String text, PDFont font, float fontSize, float y)
      throws IOException {
    boolean textModeStarted = false;
    try {
      contentStream.beginText();
      textModeStarted = true;
      contentStream.setFont(font, fontSize);
      contentStream.newLineAtOffset(MARGIN_X, y);

      String safe = sanitizeForFont(text == null ? "" : text, font);
      contentStream.showText(safe);
    } finally {
      if (textModeStarted) {
        contentStream.endText();
      }
    }
  }

  /**
   * Best-effort sanitizer to prevent PDFBox from crashing when the chosen font doesn't contain a
   * glyph for some Unicode characters (e.g., CJK).
   */
  private static String sanitizeForFont(String text, PDFont font) {
    if (text == null || text.isEmpty()) {
      return "";
    }

    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); ) {
      final int cp = text.codePointAt(i);
      i += Character.charCount(cp);

      // Keep newlines/tabs out (showText can't render them anyway in one call)
      if (cp == '\n' || cp == '\r' || cp == '\t') {
        out.append(' ');
        continue;
      }

      // Fast-path: ASCII is safe for LiberationSans.
      if (cp >= 0x20 && cp <= 0x7E) {
        out.append((char) cp);
        continue;
      }

      // Best-effort glyph support check.
      boolean supported = true;
      try {
        // PDFBox throws IllegalArgumentException for unsupported glyphs in many fonts.
        font.encode(new String(Character.toChars(cp)));
      } catch (Exception e) {
        supported = false;
      }

      out.append(supported ? new String(Character.toChars(cp)) : UNSUPPORTED_GLYPH_REPLACEMENT);
    }
    return out.toString();
  }

  private static PDFont loadEmbeddedFont(PDDocument document, String resourcePath)
      throws IOException {
    try (InputStream stream = RecipePdfExporter.class.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IOException("Unable to load bundled font resource: " + resourcePath);
      }
      return PDType0Font.load(document, stream, true);
    }
  }

  private static final class PdfWriteState implements AutoCloseable {
    private final PDDocument document;
    private final PDRectangle pageSize;
    private PDPageContentStream contentStream;
    private float y;

    private PdfWriteState(PDDocument document, PDRectangle pageSize) throws IOException {
      this.document = document;
      this.pageSize = pageSize;
      startNewPage();
    }

    private void ensurePageSpace() throws IOException {
      if (y < BOTTOM_MARGIN) {
        startNewPage();
      }
    }

    private void startNewPage() throws IOException {
      if (contentStream != null) {
        contentStream.close();
      }
      PDPage page = new PDPage(pageSize);
      document.addPage(page);
      contentStream = new PDPageContentStream(document, page);
      y = page.getMediaBox().getHeight() - MARGIN_X;
    }

    @Override
    public void close() throws IOException {
      if (contentStream != null) {
        contentStream.close();
      }
    }
  }
}
