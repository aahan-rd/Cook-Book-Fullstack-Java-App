## Feature Summary

![alt text](<Final Export PDF Button on RE.png>)
![alt text](<save pdf.png>)
![alt text](<pdf with recipie.png>)
![alt text](<export conformation.png.png>)

### Integration Notes
The Export to PDF feature integrates seamlessly with the Recipe Editor and the rest of the app. It is triggered via the "Export PDF" button in the Recipe Editor view. The RecipePdfExporter class handles the PDF generation, ensuring that the exported document includes the recipe title, ingredients, and steps in a clean and readable layout. The feature uses Apache PDFBox for PDF creation and supports basic text formatting.
It also includes a conformation page to allow the user to rename the file, chose the destination of the file, the tags for the file, etc before it gets saved. After that, the user is returned to the RE page where they get a conformation message that it was succesfull.

The RecipePdfExporter sanitizes text to prevent crashes caused by unsupported characters in the embedded font. It replaces or removes characters that cannot be rendered, ensuring the export process completes successfully. The export functionality is fully encapsulated within the RecipePdfExporter class, requiring no changes to other parts of the app.

### Status
Complete:

"Export PDF" button added to the Recipe Editor view.
PDF generation includes recipe title, ingredients, and steps.
Allows user to mdoify name and location of file before creating it
Text sanitization prevents crashes from unsupported characters.
Temporary files are cleaned up after tests.
All RecipePdfExporterTest unit tests passing.

### Known issues:

Due to the sanitization, any text that isnt in english or ACII 128 gets converted into "?" and not the language it was in or atleast a translated version of it. It also can not be shown as a msg but only replaced by ? for every instance. 
