/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.test.redirect;

import java.io.*;

/**
 * Utility class to temporarily redirect the console output streams to in-memory buffers,
 * and retrieve the outputs later, for a given thread.
 */
public final class ConsoleOutputRedirector {
  /**
   * The initial console output stream (System.out).
   */
  private static final PrintStream consoleOut = System.out;
  /**
   * The initial console error stream (System.err).
   */
  private static final PrintStream consoleErr = System.err;
  /**
   * This is where the standard output is redirected.
   */
  private static ThreadLocalOutputStream outStream = new ThreadLocalOutputStream();
  /**
   * This is where the error output is redirected.
   */
  private static ThreadLocalOutputStream errStream = new ThreadLocalOutputStream();

  static {
    try {
      System.setOut(new RedirectPrintStream(outStream, consoleOut));
      System.setErr(new RedirectPrintStream(errStream, consoleErr));
    } catch(Throwable t) {
      t.printStackTrace(consoleErr);
    }
  }

  /**
   * Can't instantiate this class.
   */
  private ConsoleOutputRedirector() {
  }

  /**
   * Start redirecting the streams for the current thread.
   */
  public static void startRedirect() {
    outStream.set(new ByteArrayOutputStream());
    errStream.set(new ByteArrayOutputStream());
  }

  /**
   * Stop redirecting the streams and return their content for the current thread.
   * @return an array of strings where the first string is the standard output and
   * the second is the error output.
   */
  public static String[] endRedirect() {
    String[] output = new String[2];
    try {
      ByteArrayOutputStream out = outStream.get();
      output[0] = (out == null) ? null : new String(out.toByteArray());
      ByteArrayOutputStream err = errStream.get();
      output[1] = (err == null) ? null : new String(err.toByteArray());
    } finally {
      outStream.remove();
      errStream.remove();
    }
    return output;
  }

  /**
   * A thread local reference to an output stream to which the console output is redirected.
   */
  private static class ThreadLocalOutputStream extends ThreadLocal<ByteArrayOutputStream> {
    @Override
    public void remove() {
      OutputStream out = get();
      if (out != null) {
        try {
          set(null);
          out.close();
        } catch (Exception e) {
          e.printStackTrace(consoleErr);
        }
      }
    }
  }

  /**
   * Implementation of a PrintStream which redirects its output
   * to a <code>ByteArrayOutputStream</code> when redirection is activated
   * or to the console output when it is deactivated.
   */
  private static class RedirectPrintStream extends PrintStream {
    /**
     * this is where we print when output is not redirected.
     */
    private final PrintStream defaultStream;
    /**
     * A thread local reference to an output stream where the console output is redirected.
     */
    private final ThreadLocalOutputStream tlos;

    /**
     * Default constructor.
     * @param tlos .
     * @param defaultStream .
     */
    public RedirectPrintStream(final ThreadLocalOutputStream tlos, final PrintStream defaultStream) {
      // constructor requires a non-null output stream
      super(new ByteArrayOutputStream());
      this.tlos = tlos;
      this.defaultStream = defaultStream;
    }

    @Override
    public void write(final int b) {
      OutputStream out = tlos.get();
      try {
        if (out != null) out.write(b);
        else defaultStream.write(b);
      } catch(IOException e) {
        e.printStackTrace(defaultStream);
      }
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) {
      OutputStream out = tlos.get();
      try {
        if (out != null) out.write(buf, off, len);
        else defaultStream.write(buf, off, len);
      } catch(IOException e) {
        e.printStackTrace(defaultStream);
      }
    }
  }
}
