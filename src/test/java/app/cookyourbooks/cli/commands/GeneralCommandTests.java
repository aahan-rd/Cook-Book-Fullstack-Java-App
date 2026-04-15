package app.cookyourbooks.cli.commands;

import static app.cookyourbooks.cli.assertions.OutputAssertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;

/** CLI tests for general commands: help, quit, exit. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class GeneralCommandTests extends CliTestBase {

  @Test
  void help_displaysAllCommands() throws Exception {
    sendCommands("help", "quit");
    runCli();

    assertThat(getOutput()).contains("CookYourBooks Commands");
    assertThat(getOutput()).contains("Library");
    assertThat(getOutput()).contains("Recipe");
    assertThat(getOutput()).contains("Tools");
    assertThat(getOutput()).contains("collections");
    assertThat(getOutput()).contains("show");
    assertThat(getOutput()).contains("scale");
  }

  @Test
  void help_withCommand_displaysDetailedHelp() throws Exception {
    sendCommands("help show", "quit");
    runCli();

    assertThat(getOutput()).contains("show <recipe>");
    assertThat(getOutput()).contains("Display full recipe details");
  }

  @Test
  void help_unknownCommand_displaysUnknownMessage() throws Exception {
    sendCommands("help nonexistent", "quit");
    runCli();

    assertThat(getOutput()).contains("Unknown command");
    assertThat(getOutput()).contains("nonexistent");
  }

  @Test
  void quit_displaysGoodbyeAndExits() throws Exception {
    sendCommands("quit");
    runCli();

    assertThat(getOutput()).contains("Goodbye!");
  }

  @Test
  void exit_displaysGoodbyeAndExits() throws Exception {
    sendCommands("exit");
    runCli();

    assertThat(getOutput()).contains("Goodbye!");
  }

  @Test
  void unknownCommand_displaysUnknownMessage() throws Exception {
    sendCommands("foobar", "quit");
    runCli();

    assertThat(getOutput()).contains("Unknown command");
    assertThat(getOutput()).contains("foobar");
  }

  @Test
  void help_scale_displaysDetailedHelp() throws Exception {
    sendCommands("help scale", "quit");
    runCli();

    assertThat(getOutput()).contains("scale <recipe> <servings>");
    assertThat(getOutput()).contains("Scale");
  }

  @Test
  void help_convert_displaysDetailedHelp() throws Exception {
    sendCommands("help convert", "quit");
    runCli();

    assertThat(getOutput()).contains("convert <recipe> <unit>");
  }
}
