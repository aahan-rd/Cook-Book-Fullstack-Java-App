## 1. Git History (Incremental Progress)

PDF export was implemented in small, reviewable commits across three pull requests.

- 7e461ca: Added PDFBox dependency and implemented RecipePdfExporter core class with basic PDF generation.
- 03b25da: Added Export PDF button to Recipe Editor UI and wired file chooser dialog.
- 8a1b51a: Added comprehensive PDF export tests covering file creation, content validation, and edge cases.
- 9f07bdc: Added feature documentation files (.md files) to meet project requirements.
- 000ad33: Removed placeholder/duplicate feature content from documentation.
- 6561682: Removed redundant rationale sections.
- f49c050: Fixed non-English character rendering (mappo tofu recipe bug) by adding font sanitization and included iPad design wireframes (v1 and v2).
- 1abd903: Final merge of feature/export-to-pdf into main.

This sequence shows: core feature -> UI wiring -> testing -> documentation refinement -> bug fixes -> polish.

## 2. PR History and Review

- PR #1: https://github.com/neu-cs3100/sp26-hw-cyb12-group-4616/pull/1
  - **Title:** Add PDFBox dependency and RecipePdfExporter core class
  - **Commits:** 2 (dependency setup + core implementation)
  - **Key changes:** Added Apache PDFBox library, created RecipePdfExporter utility class with PDF document generation logic

- PR #2: https://github.com/neu-cs3100/sp26-hw-cyb12-group-4616/pull/2
  - **Title:** Added PDF tests
  - **Commits:** 1
  - **Key changes:** Created RecipePdfExporterTest.java with 5 unit tests covering file creation, title/ingredient/step content, and empty recipe handling

- PR #8: https://github.com/neu-cs3100/sp26-hw-cyb12-group-4616/pull/8
  - **Title:** Identified and patched bug with Export and added wireframes
  - **Commits:** 4 (documentation, bug fix, design assets)
  - **Key changes:** Fixed non-English character rendering by implementing font sanitization; added feature documentation and iPad wireframes (v1 and v2)

Meaningful review outcomes captured in commit history:
- PDFBox library was selected as the PDF generation tool early and integrated cleanly into the build.
- Unicode/CJK character support was discovered during testing (mappo tofu recipe with Chinese characters) and addressed with glyph sanitization.
- Documentation was initially structured with placeholder files; review feedback validated the final structure.
- Design wireframes from iPad mockups were included to show intended UI placement and styling.

## 3. Decision Log

### Decision: Use Apache PDFBox for PDF generation with embedded TrueType fonts and implement glyph sanitization for Unicode support

Context:
The feature needed to export recipes in a universally portable format. PDFs were the clear choice, but the implementation required handling:
1. Which PDF library to use
2. Font strategy (standard vs. embedded)
3. Character encoding for international recipes

#### Option A: Use PDFBox with standard Type1 fonts (Helvetica)
- Pros: Simplest implementation, no external font files needed.
- Cons: Limited character support; crashes when encountering CJK or special Unicode glyphs (discovered bug with "mappo tofu").

#### Option B: Use a different library (e.g., iText)
- Pros: Alternative with more built-in Unicode support.
- Cons: Licensing complexity, larger dependency, steeper learning curve.

#### Option C (chosen): Use PDFBox with embedded TrueType fonts + glyph sanitization
- Pros: Full control over fonts, embedded Liberation Sans supports broad Unicode, fallback replacement for unsupported glyphs prevents crashes.
- Cons: Requires loading font resources; sanitization logic adds complexity but is maintainable and testable.

Rationale:
Option C was chosen because it balanced robustness with simplicity. Embedding fonts ensures the PDF renders consistently across devices. The glyph sanitization strategy (replace unsupported glyphs with '?') allows the export to gracefully degrade rather than crash—a better user experience than rejecting recipes with non-ASCII characters. This decision was validated when the "mappo tofu" recipe test case revealed the bug and the fix proved effective.

## 4. Testing and Quality

### Unit tests for the feature
- Test file: [src/test/java/app/cookyourbooks/gui/export/RecipePdfExporterTest.java](src/test/java/app/cookyourbooks/gui/export/RecipePdfExporterTest.java)
- Test strategy: Uses temporary files and PDFBox's PDFTextStripper to extract and validate content.
- Tests cover:
  1. **exportToPdf_createsFile()** — Verifies a PDF file is created at the specified path.
  2. **exportToPdf_containsTitle()** — Extracts text from PDF and confirms recipe title is present.
  3. **exportToPdf_containsIngredients()** — Confirms ingredient content (flour) is in the PDF.
  4. **exportToPdf_containsSteps()** — Confirms instruction text is present.
  5. **exportToPdf_emptyIngredientsAndSteps()** — Ensures export handles recipes with no ingredients/steps without throwing exceptions.

### Accessibility and robustness checks
- **File chooser dialog:** Uses standard JavaFX FileChooser, which is keyboard-accessible by default (Tab to navigate, Enter to confirm, Escape to cancel).
- **Button placement:** Export PDF button is a standard JavaFX Button in the recipe editor header, accessible via keyboard navigation.
- **Unicode support:** Glyph sanitization ensures recipes with non-ASCII characters (CJK, accents, etc.) export gracefully with replacement glyphs rather than crashing.

### Known limitations
- Glyph replacement strategy (using '?') is a best-effort approach; unsupported characters are replaced rather than preserved. For CJK, users should be aware some characters may not render perfectly.
- Multi-page support is basic; there is no explicit page break management for very long recipes (relies on PDFBox's default page overflow behavior).
- No styling customization in the exported PDF (font sizes, colors, margins are hardcoded); future iterations could expose these as preferences.