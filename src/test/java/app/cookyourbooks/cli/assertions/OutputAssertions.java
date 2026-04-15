package app.cookyourbooks.cli.assertions;

import org.assertj.core.api.Assertions;

/** Fluent assertions for CLI output. */
public final class OutputAssertions {

  private final String output;

  private OutputAssertions(String output) {
    this.output = output;
  }

  public static OutputAssertions assertThat(String output) {
    return new OutputAssertions(output);
  }

  public OutputAssertions contains(String expected) {
    Assertions.assertThat(output).contains(expected);
    return this;
  }

  public OutputAssertions doesNotContain(String... forbidden) {
    for (String s : forbidden) {
      Assertions.assertThat(output).doesNotContain(s);
    }
    return this;
  }
}
