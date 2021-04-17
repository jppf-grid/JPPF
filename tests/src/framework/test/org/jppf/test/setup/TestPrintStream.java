/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.test.setup;

import java.io.*;

/**
 * A {@link PrintStream} which adds a label and time stamps at the beginning of the messages it prints.
 * @author Laurent Cohen
 */
public class TestPrintStream extends PrintStream {
  /**
   * Client header string.
   */
  private static final String CLIENT = "[  client] ";

  /**
   * 
   * @param fileName the path to a file to print to.
   * @throws FileNotFoundException if the file doezs not exist in the file system.
   */
  public TestPrintStream(final String fileName) throws FileNotFoundException {
    super(fileName);
  }

  @Override
  public synchronized PrintStream printf(final String format, final Object... params) {
    return super.printf(getHeader() + format, params);
  }

  @Override
  public synchronized void println(final String message) {
    super.println(getHeader() + message);
  }

  @Override
  public synchronized void print(final String s) {
    if (s == null) super.print((String) null);
    else {
      final String header = getHeader();
      super.print(s.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\n" + header));
    }
  }

  /**
   * 
   * @param s .
   */
  public synchronized void printNoHeader(final String s) {
    super.print(s);
  }

  /**
   * 
   * @param label .
   * @param s .
   */
  public synchronized void print(final String label, final String s) {
    if (s == null) super.print((String) null);
    else {
      if (s.startsWith("driver process id:")) {
        final StringBuilder sb = new StringBuilder("\\r = " + (int) '\r' + ", \\n = " + (int) '\n' + ", driver string:");
        for (final char c: s.toCharArray()) sb.append(' ').append((int) c);
        System.out.println(sb.toString());
      }
      //super.print(s.replace("\r\n", "\n").replace("\n\r", "\n").replace("\r", "\n").replace("\n", "\n" + getHeader(label)));
      super.print(s.replace("\n", "\n" + getHeader(label)));
    }
  }

  /**
   * Get a header for printed messages.
   * @return the header as a string.
   */
  private static String getHeader() {
    final String s = "["  + BaseTest.getFormattedTimestamp() + "] ";
    return BaseSetup.isTestWithEmbeddedGrid() ? s : CLIENT + s;
  }

  /**
   * Get a header for printed messages.
   * @param label .
   * @return the header as a string.
   */
  public static String getHeader(final String label) {
    return label + "["  + BaseTest.getFormattedTimestamp() + "] ";
  }
}
