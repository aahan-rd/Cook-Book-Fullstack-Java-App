package app.cookyourbooks.cli.commands;

import org.jspecify.annotations.NonNull;

/** Base class for commands with common behavior. */
public abstract class AbstractCommand implements Command {

  private final String name;
  private final String description;
  private final String detailedHelp;
  private final String category;

  protected AbstractCommand(String name, String description, String detailedHelp, String category) {
    this.name = name;
    this.description = description;
    this.detailedHelp = detailedHelp;
    this.category = category;
  }

  @Override
  public @NonNull String getName() {
    return name;
  }

  @Override
  public @NonNull String getDescription() {
    return description;
  }

  @Override
  public @NonNull String getDetailedHelp() {
    return detailedHelp;
  }

  @Override
  public @NonNull String getCategory() {
    return category;
  }
}
