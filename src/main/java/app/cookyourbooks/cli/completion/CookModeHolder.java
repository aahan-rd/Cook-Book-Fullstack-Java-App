package app.cookyourbooks.cli.completion;

/** Mutable holder for cook mode state, used by the completer to offer cook sub-commands. */
public final class CookModeHolder {

  private volatile boolean inCookMode;

  /** Returns true when the user is in interactive cook mode. */
  public boolean isInCookMode() {
    return inCookMode;
  }

  /** Sets whether the user is in cook mode. Called by CookModeController on enter/exit. */
  public void setInCookMode(boolean inCookMode) {
    this.inCookMode = inCookMode;
  }
}
