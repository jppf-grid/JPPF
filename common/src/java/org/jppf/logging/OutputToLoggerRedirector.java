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

package org.jppf.logging;

import java.io.PrintStream;

/**
 * Redirect {@code System.out.println()} calls to an SLF4J-based logger.
 * @author Laurent Cohen
 */
public final class OutputToLoggerRedirector extends PrintStream {
  /**
   * The initial {@code System.out} stream.
   */
  private static final PrintStream originalSystemOut = System.out;
  /**
   * An instance of this class which redirects to a logger.
   */
  private static OutputToLoggerRedirector redirector;
  /**
   * The name of the logger to redirect to.
   */
  private String packageOrClassToLog;

  /**
   * Enable forwarding System.out.println calls to the logger if the stacktrace contains the class parameter.
   * @param clazz the class whose output to redirect.
   */
  public static void enableForClass(final Class<?> clazz) {
    redirector = new OutputToLoggerRedirector(originalSystemOut, clazz.getName());
    System.setOut(redirector);
  }

  /**
   * Enable forwarding System.out.println calls to the logger if the stacktrace contains the package parameter.
   * @param packageToLog the name of the package whose output to redirect.
   */
  public static void enableForPackage(final String packageToLog) {
    redirector = new OutputToLoggerRedirector(originalSystemOut, packageToLog);
    System.setOut(redirector);
  }

  /**
   * Disable forwarding to the logger resetting the standard output to the console.
   */
  public static void disable() {
    System.setOut(originalSystemOut);
    redirector = null;
  }

  /**
   * Initialize with the specified initial print stream and logger name to redirect to.
   * @param original the initial {@code System.out} stream.
   * @param packageOrClassToLog the name of the logger to redirect to.
   */
  private OutputToLoggerRedirector(final PrintStream original, final String packageOrClassToLog) {
    super(original);
    this.packageOrClassToLog = packageOrClassToLog;
  }

  @Override
  public void println(final String line) {
    final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = findCaller(stack);
    if (caller == null) {
      super.println(line);
      return;
    }
    final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(caller.getClassName());
    log.info(line);
  }

  /**
   * Find in the call stack the caller of {@code println()}.
   * @param stack the call stack to look into.
   * @return a {@link StackTraceElement} or {@code null} if none was found that matches the looger name.
   */
  private StackTraceElement findCaller(final StackTraceElement[] stack) {
    for (StackTraceElement element : stack) {
      if (element.getClassName().startsWith(packageOrClassToLog)) return element;
    }
    return null;
  }
}
