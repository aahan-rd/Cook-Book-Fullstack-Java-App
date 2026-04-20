# Cook Your Books

A production-grade recipe management desktop application demonstrating clean architecture, professional testing practices, and thoughtful UX design.

**Built for:** CS 3100: Program Design & Implementation 2 (Northeastern University)  
**Language:** Java 21 | **Framework:** JavaFX 21 | **Build:** Gradle

---

## Quick Start

### Run Locally

```bash
# Windows
gradlew.bat run

# macOS / Linux
./gradlew run
```

**Requirements:** Java 21+ (Gradle Wrapper included)

### Run Tests

```bash
./gradlew test
./gradlew coverageReport
```

---

## What This Is

A **complete desktop application** that manages recipes with a focus on:

- **Clean architecture** (MVVM + hexagonal design)
- **Testable code** (100+ unit tests, full test isolation)
- **Responsive UI** (debounced search, async operations)
- **Professional tooling** (static analysis, code style, coverage)

You can import recipes via OCR, edit them, search across your library, and export to PDF—all while maintaining a codebase that's easy to understand, test, and extend.

---

## Architecture

### MVVM with Hexagonal Layers

```
┌──────────────────────────────────┐
│  JavaFX View                     │  (FXML + Controller)
│  ↔ Observable Bindings           │  - Display state
├──────────────────────────────────┤  - User events
│  ViewModel                       │  (Observable properties)
│  ↔ Commands                      │  - Business logic
├──────────────────────────────────┤  - No GUI dependencies
│  Service Layer                   │  (Hexagonal core)
│  ↔ Use-cases                     │  - Recipe operations
├──────────────────────────────────┤  - Collection management
│  Adapters & Repository           │  (Ports)
│  - OCR (Gemini)                  │  - JSON persistence
│  - PDF export (PDFBox)           │  - External integrations
└──────────────────────────────────┘
```

**Why this structure:**
- **Testability:** ViewModels are unit-tested in isolation (no JavaFX thread in tests)
- **Independence:** Each layer has a clear responsibility
- **Flexibility:** Adapters (OCR, PDF, persistence) can be swapped or mocked
- **Maintainability:** New developers see clear data flow

### Dependency Injection

All services are wired explicitly in `CookYourBooksGuiApp`:

```java
// Manual DI keeps dependencies visible and testable
LibrarianService librarianService = new LibrarianServiceImpl(repository);
NavigationService navigationService = new NavigationServiceImpl(this);
SearchViewModel searchViewModel = new SearchViewModelImpl(
    librarianService,
    navigationService,
    Duration.ofMillis(300)  // Debounce delay
);
```

**Benefits:**
- No magic annotations or reflection
- Dependencies are explicit
- Easy to inject mocks in tests
- Compile-time safety

---

## Key Features

### 1. Search & Filter

Debounced, async search with race-condition protection:

- **300ms debounce:** Waits after user stops typing before querying
- **Generation counter:** Prevents stale results if searches complete out of order
- **Ingredient filtering:** Multiple filters use AND logic (not OR)
- **Keyboard navigation:** Arrow keys select results; Enter navigates

**Implementation highlight** (`SearchViewModelImpl`):
```java
private int searchGeneration = 0;

void search(String query) {
    int currentGen = ++searchGeneration;  // Increment generation
    
    // Start background search
    BackgroundTaskRunner.run(
        () -> librarianService.resolveRecipes(query),
        results -> {
            if (currentGen == searchGeneration) {  // Only apply if latest
                this.results.setAll(results);
            }
        }
    );
}
```

### 2. Recipe Editor

Edit recipes with change tracking and async persistence:

- **Dirty-state tracking:** Knows if changes have been made
- **Validation:** Title required, ingredients must be valid
- **Async save:** Persists on background thread; shows loading state
- **Error recovery:** Failed saves preserve edits and show error message

### 3. Import Workflow

OCR-powered recipe import:

- **State machine:** idle → processing → review → idle or error
- **Pre-save editing:** Review and edit extracted recipe before committing
- **Graceful errors:** Network failures and parsing errors are handled cleanly

### 4. Library View

Browse and organize recipe collections:

- **Undo-delete:** 5-second window to recover deleted collections
- **Filtering:** Search collections by title (live, no delay)
- **Async loading:** Fetches collections on background thread

---

## User Flow

```
1. Import a Recipe
   └─ Select image → OCR extracts → Review extracted recipe

2. Edit the Recipe
   └─ Open in editor → Make changes → Save to disk

3. Find a Recipe
   └─ Search by title + filter by ingredient → Keyboard navigate → Open editor

4. Browse Collections
   └─ View all collections → Select one → See recipes → Open editor
```

Each transition uses `NavigationService` for clean, decoupled routing between views.

---

## Testing

### Approach

- **Unit tests on ViewModels** (isolated, no GUI)
- **Integration tests** (cross-feature workflows)
- **E2E tests with TestFX + Monocle** (headless GUI testing)
- **Mocked services** (deterministic, fast)

### Example Test

```java
@Test
void debounceWaitsBeforeFiring() {
    SearchViewModel vm = new SearchViewModelImpl(
        mockLibrarianService,
        mockNavigationService,
        Duration.ofMillis(50)  // Fast in tests
    );
    
    vm.search("pasta");
    vm.search("pizza");  // Resets debounce
    
    // Wait for background task
    Thread.sleep(100);
    Platform.runLater(() -> {
        assertThat(vm.getResultIds())
            .containsExactly("pizza-1", "pizza-2");
    });
}
```

### Run Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests SearchViewModelTest

# With coverage report
./gradlew coverageReport
```

**Coverage:** ~85% on ViewModel and service layers (UI bindings intentionally excluded).

---

## Code Quality

### Static Analysis

```bash
./gradlew check
```

Includes:
- **Error Prone + NullAway:** Catches common bugs at compile time
- **Spotless:** Google Java Format enforcement
- **Checkstyle:** Course-mandated style rules

### CI-Ready Headless Testing

Build is configured for headless testing (no display needed):

```gradle
tasks.test {
    systemProperty("monocle.platform", "Headless")
    systemProperty("glass.platform", "Monocle")
}
```

---

## Tech Stack

| Component | Choice | Why |
|-----------|--------|-----|
| **Language** | Java 21 | Modern syntax, strong typing, long-term support |
| **GUI** | JavaFX 21 | Native, cross-platform, FXML for declarative UI |
| **Build** | Gradle | Flexible, dependency management, faster incremental builds |
| **Persistence** | Jackson + JSON | Human-readable, easy to debug, lightweight |
| **OCR** | Google Gemini API | State-of-the-art, no local ML infrastructure |
| **PDF Export** | Apache PDFBox | Mature, reliable, good community support |
| **Testing** | JUnit 5 + Mockito + TestFX | Modern APIs, flexible mocking, GUI automation |
| **Code Quality** | Error Prone + NullAway + Spotless | Catches real bugs, prevents null errors, consistent style |

---

## Project Structure

```
src/main/java/app/cookyourbooks
├── CookYourBooksGuiApp.java       # Application entry point + DI wiring
│
├── gui/                            # MVVM Views & ViewModels
│   ├── view/                      # FXML files
│   ├── controller/                # FXML controllers
│   └── viewmodel/                 # Observable state & commands
│
├── services/                       # Core business logic
│   ├── LibrarianService.java      # Recipe/collection operations
│   ├── NavigationService.java     # View routing
│   └── RecipeOcrService.java      # OCR abstraction
│
├── repository/                     # Persistence interfaces
│   └── RecipeRepository.java      # Recipe storage
│
├── adapters/                       # External integrations
│   ├── GeminiOcrAdapter.java      # Gemini API wrapper
│   └── PdfExportAdapter.java      # PDFBox wrapper
│
└── model/                          # Domain objects
    ├── Recipe.java
    ├── RecipeCollection.java
    └── Ingredient.java
```

---

## Configuration

### OCR API Key

To use the import feature, you need a Google Gemini API key:

1. Get a key from [Google AI Studio](https://aistudio.google.com)
2. Set it one of these ways:

   ```bash
   export GOOGLE_API_KEY=your_key_here
   ```

   Or in `~/.env`:
   ```
   GOOGLE_API_KEY=your_key_here
   ```

3. Run the app—OCR will work

### Without API Key

The app runs fully functional without an API key. Import feature will show a placeholder or disabled state.

---

## Distribution

### For Development

```bash
./gradlew run
```

### For Distribution (Native App Image)

Build a standalone executable with bundled Java runtime:

```bash
# Windows
./gradlew jpackageWindows

# macOS
./gradlew jpackageMacOS

# Linux
./gradlew jpackageLinux
```

**Output:**
- Windows: `build/jpackage/CookYourBooks.exe`
- macOS: `build/jpackage/CookYourBooks.app`
- Linux: `build/jpackage/bin/cookyourbooks`

**Trade-off:** Native app images must be built on each target OS (jpackage doesn't cross-compile). Upside: double-click launch, bundled runtime, no Java installation needed.

---

## What This Demonstrates

For **technical interviews:**
- ✅ Clean architecture (MVVM, hexagonal design, dependency injection)
- ✅ Async GUI patterns (debounce, race-condition handling, background tasks)
- ✅ Comprehensive testing (unit, integration, GUI automation)
- ✅ Production-ready tooling (static analysis, code style, CI configuration)
- ✅ Professional collaboration (code review, documentation, team architecture)

For **code review:**
- Testable ViewModels isolated from GUI complexity
- Observable properties for clean data binding
- Service layer abstraction for flexibility
- Explicit dependency injection for clarity
- Comprehensive test coverage for confidence

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| **MVVM** | Separates UI binding logic from business logic; ViewModels are independently testable |
| **Debounced search** | Prevents UI thrash and reduces redundant queries during rapid typing |
| **Generation counter** | Ensures race-condition safety when searches complete out of order |
| **Manual DI** | Dependencies are explicit and visible; no reflection magic; easy to test |
| **JSON persistence** | Human-readable; easy to inspect during development; sufficient for this scale |
| **Async-first** | Long operations (OCR, save) don't freeze UI; responsive feel |
| **Undo-delete** | Reduces accidental data loss; improves user confidence |

---

## If You're Reviewing This

**Start here:**
1. Run `./gradlew test` — see all tests pass
2. Run `./gradlew run` — launch the app
3. Open `src/main/java/app/cookyourbooks/gui/viewmodel/SearchViewModelImpl.java` — see debounce + race protection
4. Open `src/test/java/app/cookyourbooks/gui/viewmodel/SearchViewModelTest.java` — see how it's tested

**Questions I'm ready for:**
- "Why MVVM instead of MVC?" → Better for JavaFX; ViewModel is testable without GUI
- "How do you handle race conditions in search?" → Generation counter; only apply results if they're from the latest search
- "Why manual DI instead of Spring?" → Explicit, lightweight, easy to test; Spring adds complexity for this project's scale
- "How do you test async GUI code?" → Mockito for services, TestFX for user events, ConfigurableExecutors for timing

---

## Next Steps

### To Extend This

1. **Add a backend API** (currently in-memory; could integrate REST)
2. **Collaborative features** (share recipes, ratings, comments)
3. **Advanced search** (filter by cook time, difficulty, dietary restrictions)
4. **Mobile companion app** (sync recipes via API)

### To Contribute

Follow the existing patterns:
- New ViewModels extend the MVVM contract
- New adapters implement port interfaces
- All new code includes unit tests
- Static analysis must pass (`./gradlew check`)

---

## Acknowledgments

Built as the capstone project for **CS 3100: Program Design & Implementation 2** at Northeastern University.

Course emphasis: **clean architecture, professional testing, and thoughtful collaboration.**

---

**Interested in the details?** Explore the code:
- Architecture: `CookYourBooksGuiApp.java` (DI wiring)
- Search logic: `SearchViewModelImpl.java` (debounce + race protection)
- Testing: `src/test/java/` (100+ unit and integration tests)
- Build config: `build.gradle` (tooling stack)

Built with attention to detail and designed to be understood. 🚀
