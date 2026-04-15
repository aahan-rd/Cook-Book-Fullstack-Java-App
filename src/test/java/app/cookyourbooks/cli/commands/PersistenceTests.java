package app.cookyourbooks.cli.commands;

import static app.cookyourbooks.cli.assertions.OutputAssertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import app.cookyourbooks.cli.CliTestBase;

/** CLI tests for persistence: data survives across sessions. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class PersistenceTests extends CliTestBase {

  @Test
  void collectionCreate_persistsAcrossReload() throws Exception {
    sendCommands("collection create MyCookbook", "quit");
    runCli();

    assertThat(getOutput()).contains("Created personal collection 'MyCookbook'");

    resetCliSession();
    sendCommands("collections", "quit");
    runCli();

    assertThat(getOutput()).contains("MyCookbook");
  }
}
