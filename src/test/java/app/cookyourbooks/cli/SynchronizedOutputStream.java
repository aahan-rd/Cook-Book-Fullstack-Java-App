package app.cookyourbooks.cli;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that synchronizes all writes to prevent character-level interleaving when multiple
 * threads (e.g. JLine's LineReader and the main CLI thread) write to the same stream concurrently.
 */
final class SynchronizedOutputStream extends FilterOutputStream {

  SynchronizedOutputStream(OutputStream out) {
    super(out);
  }

  @Override
  public synchronized void write(int b) throws IOException {
    out.write(b);
  }

  @Override
  public synchronized void write(byte[] b) throws IOException {
    out.write(b);
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
  }

  @Override
  public synchronized void flush() throws IOException {
    out.flush();
  }
}
