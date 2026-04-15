# GA1: CookYourBooks JavaFX GUI
In this assignment, your team implements the four core GUI features for CookYourBooks. Each team member owns one feature and is individually accountable for their ViewModel and View implementation, which will be manually evaluated by course staff. Teams collaborate on shared infrastructure, integration, and code review. Teams of three may omit the "Search & Filter" feature (as in GA0); the omitted feature is not reassigned—each of the remaining three core features must have one owner, and grading applies only to the features you implement.

Read the complete assignment specification [here](https://neu-pdi.github.io/cs3100-public-resources/assignments/cyb11-core-features), and note the [Git workflow requirements](https://neu-pdi.github.io/cs3100-public-resources/assignments/git-workflow).

## Running

**Build everything (compile, test, checkstyle, format):**
```bash
./gradlew build
```

**Run the GUI app:**
```bash
./gradlew run
```
This launches `CookYourBooksGuiApp`, which loads `cyb-library.json` from the project root.

**Run the CLI app (A5 reference implementation):**
```bash
./gradlew shadowJar && java -jar build/libs/cookyourbooks-all.jar
```

**Run tests:**
```bash
./gradlew test
```

**Auto-format code:**
```bash
./gradlew spotlessApply
```

## Gemini API Setup (Import Feature)

The Import feature uses Google's Gemini API to extract recipes from images. The handout includes a `StubRecipeOcrService` that returns a sample recipe after a short delay — this lets you develop and test the Import UI without an API key.

When you're ready to connect to the real Gemini API, you'll need to implement `RecipeOcrService` using the `com.google.genai` SDK (already in `build.gradle`).

### Setting Up Your API Key

1. **Get a Gemini API key** from [Google AI Studio](https://aistudio.google.com/apikey) (free tier is fine)

2. **Create a `.env` file** in the project root (next to `build.gradle`):
    ```bash
    cp .env.example .env
    ```

3. **Add your API key** to `.env`:
    ```
    GOOGLE_API_KEY=your-actual-api-key-here
    ```

The app automatically loads `.env` at startup via `dotenv-java`. The `.env` file is in `.gitignore`, so your key won't be committed.

**Alternative:** You can also export `GOOGLE_API_KEY` as a shell environment variable if you prefer.

### Implementing the OCR Service

Swap the stub for your real implementation in `CookYourBooksGuiApp.java`:
```java
// var ocrService = new StubRecipeOcrService();
var ocrService = new GeminiRecipeOcrService();  // your implementation
```

The `RecipeOcrService` interface (`src/main/java/app/cookyourbooks/services/RecipeOcrService.java`) has a single method:
```java
Recipe parseRecipeFromImage(Path imagePath) throws IOException;
```

Your implementation should send the image to Gemini, parse the response into a `Recipe`, and throw `IOException` on failure. The `StubRecipeOcrService` shows the expected return shape.

**Important:** Never commit your API key — always use `.env` or environment variables.
