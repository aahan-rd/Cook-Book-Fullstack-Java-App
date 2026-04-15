package app.cookyourbooks.gui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javafx.concurrent.Task;

/**
 * Utility for running background work off the JavaFX Application Thread.
 *
 * <p>Every GUI feature in CookYourBooks requires at least one async operation. This helper wraps
 * {@link javafx.concurrent.Task} creation, daemon thread management, and FX-thread callback
 * delivery into a single method call.
 *
 * <h2>How it works</h2>
 *
 * <ol>
 *   <li>The {@code callable} runs on a <b>background (daemon) thread</b> — use it for I/O, network
 *       calls, or heavy computation that would freeze the UI.
 *   <li>On success, {@code onSuccess} is called on the <b>FX Application Thread</b> with the
 *       result.
 *   <li>On failure, {@code onFailure} is called on the <b>FX Application Thread</b> with the
 *       exception.
 * </ol>
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * BackgroundTaskRunner.run(
 *     () -> librarianService.listCollections(),   // background thread
 *     collections -> {                            // FX thread (success)
 *         this.collections.setAll(collections);
 *         loading.set(false);
 *     },
 *     error -> {                                  // FX thread (failure)
 *         statusMessage.set("Failed: " + error.getMessage());
 *         loading.set(false);
 *     }
 * );
 * }</pre>
 *
 * <h2>Cancellation behavior</h2>
 *
 * <p><b>Important:</b> {@code Task.cancel()} triggers the task's {@code onCancelled} handler,
 * <b>not</b> {@code onFailed}. Since this utility only sets {@code onSucceeded} and {@code
 * onFailed}, cancelling a task returned by {@link #run} will cause <b>neither</b> callback to fire.
 * If you cancel a task, your code must handle the resulting state transition directly (e.g., in
 * your {@code cancelImport()} method), rather than relying on the {@code onFailure} callback.
 *
 * <p><b>TA meeting question:</b> "Walk me through what {@code BackgroundTaskRunner.run()} does
 * internally. What thread does your callable run on? What thread does {@code onSuccess} run on?
 * What would break if {@code onSuccess} ran on the background thread?"
 */
public final class BackgroundTaskRunner {

  private BackgroundTaskRunner() {}

  /**
   * Runs a callable on a background thread and delivers the result on the FX thread.
   *
   * @param <T> the result type
   * @param callable the work to do off the FX thread
   * @param onSuccess called on the FX thread with the result
   * @param onFailure called on the FX thread with the exception
   * @return the Task (for cancellation support — call {@code task.cancel()} to interrupt)
   */
  public static <T> Task<T> run(
      Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return callable.call();
          }
        };
    task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
    task.setOnFailed(e -> onFailure.accept(task.getException()));
    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
    return task;
  }
}
