package app.cookyourbooks.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.cookyourbooks.CookYourBooksApp;
import app.cookyourbooks.CookYourBooksApp.TestCliHarness;
import app.cookyourbooks.model.Recipe;

/** Abstract base for CLI E2E tests using JLine dumb terminal. */
public abstract class CliTestBase {

  protected Path tempDir;
  protected Path libraryPath;
  protected Terminal terminal;
  protected ByteArrayOutputStream output;
  protected PipedInputStream pipedIn;
  protected PipedOutputStream commandInput;
  protected TestCliHarness harness;

  private final Set<String> createdCollections = new HashSet<>();
  private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  @BeforeEach
  void setUpBase() throws Exception {
    tempDir = Files.createTempDirectory("cyb-test");
    libraryPath = tempDir.resolve("cyb-library.json");
    output = new ByteArrayOutputStream();
    pipedIn = new PipedInputStream();
    commandInput = new PipedOutputStream(pipedIn);
    createdCollections.clear();

    // Wrap output in SynchronizedOutputStream to prevent interleaving when JLine's LineReader
    // and the main CLI thread write concurrently. Set explicit size to avoid 0x0 Display behavior.
    terminal =
        TerminalBuilder.builder()
            .system(false)
            .type(Terminal.TYPE_DUMB)
            .streams(pipedIn, new SynchronizedOutputStream(output))
            .size(new Size(80, 24))
            .build();

    harness = CookYourBooksApp.createTestHarness(libraryPath, terminal);
  }

  @AfterEach
  void tearDownBase() throws Exception {
    if (commandInput != null) {
      commandInput.close();
    }
    if (pipedIn != null) {
      pipedIn.close();
    }
    if (tempDir != null) {
      try (var stream = Files.walk(tempDir)) {
        stream
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(
                p -> {
                  try {
                    Files.deleteIfExists(p);
                  } catch (IOException ignored) {
                  }
                });
      } catch (IOException ignored) {
      }
    }
  }

  protected void sendCommand(String command) throws IOException {
    commandInput.write((command.endsWith("\n") ? command : command + "\n").getBytes(UTF_8));
    commandInput.flush();
  }

  protected void sendCommands(String... commands) throws IOException {
    for (String cmd : commands) {
      sendCommand(cmd);
    }
  }

  protected void runCli() {
    harness.run();
  }

  protected String getOutput() {
    return output.toString(UTF_8);
  }

  /**
   * Resets the CLI session with fresh pipes and reloads the library from disk. Use to verify
   * persistence: run commands, quit, call resetCliSession(), then run more commands to confirm data
   * survived.
   */
  protected void resetCliSession() throws Exception {
    if (commandInput != null) {
      commandInput.close();
    }
    if (pipedIn != null) {
      pipedIn.close();
    }
    output.reset();
    pipedIn = new PipedInputStream();
    commandInput = new PipedOutputStream(pipedIn);
    terminal =
        TerminalBuilder.builder()
            .system(false)
            .type(Terminal.TYPE_DUMB)
            .streams(pipedIn, new SynchronizedOutputStream(output))
            .size(new Size(80, 24))
            .build();

    harness = CookYourBooksApp.createTestHarness(libraryPath, terminal);
  }

  /** Creates a collection via CLI. Queued until runCli() is called. */
  protected void setupCollection(String name) throws IOException {
    if (createdCollections.add(name)) {
      sendCommand("collection create \"" + name + "\"");
    }
  }

  /**
   * Creates a collection and imports a recipe via CLI. Queued until runCli() is called.
   *
   * @param recipe the recipe to import
   * @param collectionTitle the collection to create (if needed) and import into
   */
  protected void setupRecipeInCollection(Recipe recipe, String collectionTitle) throws IOException {
    setupCollection(collectionTitle);
    Path jsonPath = tempDir.resolve(UUID.randomUUID() + ".json");
    Files.writeString(jsonPath, OBJECT_MAPPER.writeValueAsString(recipe));
    String pathForCli = jsonPath.toAbsolutePath().toString().replace("\\", "/");
    sendCommand("import json \"" + pathForCli + "\" \"" + collectionTitle + "\"");
  }

  /**
   * Gets completion candidates for a partial command. Uses the CLI's wired LineReader and
   * completer. Cursor is at end of the string.
   */
  protected List<String> getCompletionValues(String partialCommand) {
    return harness.getCompletionValues(partialCommand);
  }

  /**
   * Sets cook mode state for completion testing. CookModeCompleter only completes when in cook
   * mode.
   */
  protected void setCookMode(boolean inCookMode) {
    harness.setCookMode(inCookMode);
  }
}
