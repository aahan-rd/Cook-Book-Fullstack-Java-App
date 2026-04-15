package app.cookyourbooks.gui;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeAll;

public abstract class ViewModelTestBase {
  @BeforeAll
  static void initToolkit() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException e) {
      // already initialized
    }
  }

  protected static void waitForFxEvents() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(latch::countDown);
    latch.await();
  }
}
