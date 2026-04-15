package app.cookyourbooks;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import app.cookyourbooks.adapters.MarkdownExporter;
import app.cookyourbooks.cli.CliContext;
import app.cookyourbooks.cli.CookYourBooksCli;
import app.cookyourbooks.cli.commands.CommandRegistry;
import app.cookyourbooks.cli.completion.CollectionNameCompleter;
import app.cookyourbooks.cli.completion.CommandCompleter;
import app.cookyourbooks.cli.completion.CookModeCompleter;
import app.cookyourbooks.cli.completion.CookModeHolder;
import app.cookyourbooks.cli.completion.CybCompleter;
import app.cookyourbooks.cli.completion.RecipeTitleCompleter;
import app.cookyourbooks.cli.completion.UnitCompleter;
import app.cookyourbooks.services.CookingServiceImpl;
import app.cookyourbooks.services.LibrarianServiceImpl;
import app.cookyourbooks.services.PlannerServiceImpl;
import app.cookyourbooks.services.ShoppingListAggregator;
import app.cookyourbooks.services.TransformerServiceImpl;

/** Main entry point for CookYourBooks CLI. */
public final class CookYourBooksApp {

  private CookYourBooksApp() {}

  /**
   * Creates a test harness for CLI E2E testing.
   *
   * <p>This factory creates a fully-wired CLI without exposing internal service implementations.
   * Tests interact only through terminal I/O and this harness API. This design ensures the provided
   * test suite works regardless of how students decompose their service layer.
   *
   * @param libraryPath path to the library JSON file (e.g. cyb-library.json)
   * @param terminal JLine terminal (typically dumb terminal with piped streams for tests)
   * @return a harness with run(), getCompletionValues(), and setCookMode()
   */
  public static TestCliHarness createTestHarness(Path libraryPath, Terminal terminal) {
    CybLibrary library = CybLibrary.load(libraryPath);
    var recipeRepo = library.getRecipeRepository();
    var collRepo = library.getCollectionRepository();

    var transformerService = new TransformerServiceImpl(library::getConversionRegistry);
    var librarianService = new LibrarianServiceImpl(recipeRepo, collRepo, library);
    var cookingService = new CookingServiceImpl();
    var plannerService =
        new PlannerServiceImpl(new ShoppingListAggregator(), new MarkdownExporter());

    CommandRegistry registry = new CommandRegistry();
    CookModeHolder cookModeHolder = new CookModeHolder();
    CybCompleter completer =
        new CybCompleter(
            new CommandCompleter(registry),
            new RecipeTitleCompleter(recipeRepo),
            new CollectionNameCompleter(collRepo),
            new UnitCompleter(),
            new CookModeCompleter(cookModeHolder));

    LineReader reader =
        LineReaderBuilder.builder()
            .terminal(terminal)
            .parser(createParser())
            .completer(completer)
            .build();

    CliContext context =
        new CliContext(
            librarianService,
            cookingService,
            plannerService,
            transformerService,
            terminal,
            reader,
            new PrintWriter(
                new OutputStreamWriter(terminal.output(), StandardCharsets.UTF_8), true),
            cookModeHolder);

    CookYourBooksCli cli = new CookYourBooksCli(context, registry);
    registry.register(new app.cookyourbooks.cli.commands.HelpCommand(registry));
    registry.register(new app.cookyourbooks.cli.commands.QuitCommand(cli::stop));
    registry.registerAlias("exit", registry.find("quit").orElseThrow());
    registry.register(new app.cookyourbooks.cli.commands.CollectionsCommand());
    registry.register(new app.cookyourbooks.cli.commands.CollectionCreateCommand());
    registry.register(new app.cookyourbooks.cli.commands.RecipesCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConversionsCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConversionAddCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConversionRemoveCommand());
    registry.register(new app.cookyourbooks.cli.commands.ShowCommand());
    registry.register(new app.cookyourbooks.cli.commands.SearchCommand());
    registry.register(new app.cookyourbooks.cli.commands.ImportJsonCommand());
    registry.register(new app.cookyourbooks.cli.commands.DeleteCommand());
    registry.register(new app.cookyourbooks.cli.commands.ScaleCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConvertCommand());
    registry.register(new app.cookyourbooks.cli.commands.ShoppingListCommand());
    registry.register(new app.cookyourbooks.cli.commands.CookCommand());
    registry.register(new app.cookyourbooks.cli.commands.ExportCommand());

    return new TestCliHarness(cli, reader, completer, cookModeHolder);
  }

  /** Test harness for CLI E2E tests. Opaque wrapper around wired CLI components. */
  public static final class TestCliHarness {

    private final CookYourBooksCli cli;
    private final LineReader lineReader;
    private final CybCompleter completer;
    private final CookModeHolder cookModeHolder;

    TestCliHarness(
        CookYourBooksCli cli,
        LineReader lineReader,
        CybCompleter completer,
        CookModeHolder cookModeHolder) {
      this.cli = cli;
      this.lineReader = lineReader;
      this.completer = completer;
      this.cookModeHolder = cookModeHolder;
    }

    /** Runs the CLI main loop. */
    public void run() {
      cli.run();
    }

    /**
     * Gets completion candidates for a partial command. Cursor is at end of the string.
     *
     * @param partialCommand the command line as typed so far
     * @return list of completion values
     */
    public List<String> getCompletionValues(String partialCommand) {
      var parser = lineReader.getParser();
      int cursor = partialCommand.length();
      ParsedLine parsed = parser.parse(partialCommand, cursor);
      List<Candidate> candidates = new ArrayList<>();
      completer.complete(lineReader, parsed, candidates);
      return candidates.stream().map(Candidate::value).toList();
    }

    /**
     * Sets cook mode state for completion testing. CookModeCompleter only completes when in cook
     * mode.
     *
     * @param inCookMode true if simulating cook mode for completion tests
     */
    public void setCookMode(boolean inCookMode) {
      cookModeHolder.setInCookMode(inCookMode);
    }
  }

  public static void main(String[] args) {
    Path libraryPath = Path.of("cyb-library.json");
    CybLibrary library = CybLibrary.load(libraryPath);

    var recipeRepo = library.getRecipeRepository();
    var collRepo = library.getCollectionRepository();

    var transformerService = new TransformerServiceImpl(library::getConversionRegistry);
    var librarianService = new LibrarianServiceImpl(recipeRepo, collRepo, library);
    var cookingService = new CookingServiceImpl();
    var plannerService =
        new PlannerServiceImpl(new ShoppingListAggregator(), new MarkdownExporter());

    Terminal terminal;
    try {
      terminal = TerminalBuilder.builder().system(true).build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create terminal", e);
    }

    CommandRegistry registry = new CommandRegistry();
    registry.register(new app.cookyourbooks.cli.commands.HelpCommand(registry));
    registry.register(new app.cookyourbooks.cli.commands.CollectionsCommand());
    registry.register(new app.cookyourbooks.cli.commands.CollectionCreateCommand());
    registry.register(new app.cookyourbooks.cli.commands.RecipesCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConversionsCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConversionAddCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConversionRemoveCommand());
    registry.register(new app.cookyourbooks.cli.commands.ShowCommand());
    registry.register(new app.cookyourbooks.cli.commands.SearchCommand());
    registry.register(new app.cookyourbooks.cli.commands.ImportJsonCommand());
    registry.register(new app.cookyourbooks.cli.commands.DeleteCommand());
    registry.register(new app.cookyourbooks.cli.commands.ScaleCommand());
    registry.register(new app.cookyourbooks.cli.commands.ConvertCommand());
    registry.register(new app.cookyourbooks.cli.commands.ShoppingListCommand());
    registry.register(new app.cookyourbooks.cli.commands.CookCommand());
    registry.register(new app.cookyourbooks.cli.commands.ExportCommand());

    CookModeHolder cookModeHolder = new CookModeHolder();
    CybCompleter completer =
        new CybCompleter(
            new CommandCompleter(registry),
            new RecipeTitleCompleter(recipeRepo),
            new CollectionNameCompleter(collRepo),
            new UnitCompleter(),
            new CookModeCompleter(cookModeHolder));

    LineReader reader =
        LineReaderBuilder.builder()
            .terminal(terminal)
            .parser(createParser())
            .completer(completer)
            .build();

    CliContext context =
        new CliContext(
            librarianService,
            cookingService,
            plannerService,
            transformerService,
            terminal,
            reader,
            new PrintWriter(
                new OutputStreamWriter(terminal.output(), StandardCharsets.UTF_8), true),
            cookModeHolder);

    CookYourBooksCli cli = new CookYourBooksCli(context, registry);
    registry.register(new app.cookyourbooks.cli.commands.QuitCommand(cli::stop));
    registry.registerAlias("exit", registry.find("quit").orElseThrow());

    cli.run();
  }

  private static DefaultParser createParser() {
    DefaultParser parser = new DefaultParser();
    parser.setEscapeChars(new char[0]);
    return parser;
  }
}
