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
import java.text.SimpleDateFormat;
import java.util.*;

import org.jppf.client.JPPFClient;
import org.jppf.jmx.JMXHelper;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.streams.StreamUtils;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.*;

/**
 * Base class for JPPF automated tests.
 * @author Laurent Cohen
 */
public class BaseTest {
  static {
    Locale.setDefault(Locale.US);
  }
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger("TEST");
  /** */
  protected static final String JMX_REMOTE_PROTOCOL = JPPFConfiguration.get(JPPFProperties.JMX_REMOTE_PROTOCOL);
  /** */
  protected static final int DRIVER_MANAGEMENT_PORT_BASE = (JMXHelper.JMXMP_PROTOCOL.equals(JMX_REMOTE_PROTOCOL)) ? 11200 : 11100;
  /** */
  protected static final int SSL_DRIVER_MANAGEMENT_PORT_BASE = (JMXHelper.JMXMP_PROTOCOL.equals(JMX_REMOTE_PROTOCOL)) ? 12200 : 12100;
  /** */
  protected static final int NODE_MANAGEMENT_PORT_BASE = 12300;
  /** */
  private static PrintStream stdOut, stdErr;
  /** */
  protected static JPPFClient client;
  /** */
  private static final FileFilter logFileFilter = new FileFilter() {
    @Override
    public boolean accept(final File path) {
      if (path.isDirectory()) return false;
      final String s = path.getName();
      return (s != null) && s.endsWith(".log");
    }
  };
  /** */
  @ClassRule
  public static final TestWatcher classWatcher = new BaseTestClassWatcher();
  /** */
  @Rule
  public final TestWatcher instanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      print("***** start of method %s() *****", description.getMethodName());
    }
  };
  /**
   * Used to format timestamps in the std and err outputs.
   */
  private static final SimpleDateFormat SDF = new SimpleDateFormat("hh:mm:ss.SSS");

  /**
   * Zip all log files into the file {@code logs/<className>.zip}.
   * @param className the name of the class for which to zip the logs.
   */
  private static void zipLogs(final String className) {
    final File dir = new File(System.getProperty("user.dir"));
    final File[] logFiles = dir.listFiles(logFileFilter);
    if ((logFiles == null) || (logFiles.length <= 0)) return;
    final File logDir = new File(dir, "logs/");
    if (!logDir.exists()) logDir.mkdirs();
    final File outZip = new File(logDir, className + ".zip");
    final String[] logPaths = new String[logFiles.length];
    for (int i=0; i<logFiles.length; i++) logPaths[i] = logFiles[i].getPath();
    ZipUtils.zipFile(outZip.getPath(), logPaths);
  }

  /**
   * Print a formatted message to the shell output only.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void printOut(final String format, final Object...params) {
    print(true, false, format, params);
  }

  /**
   * Print a formatted message to the shell output and to the log file.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void print(final String format, final Object...params) {
    print(false, true, format, params);
  }

  /**
   * Print a formatted message to the shell output and to the log file.
   * @param decorate whether to add decorations around the message.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void print(final boolean decorate, final String format, final Object...params) {
    print(false, decorate, format, params);
  }

  /**
   * Print a formatted message to the shell output and to the log file.
   * @param systemOutOnly whether to print to {@code System.out} only.
   * @param decorate whether to add decorations around the message.
   * @param format the format.
   * @param params the parameter values.
   */
  public static void print(final boolean systemOutOnly, final boolean decorate, final String format, final Object...params) {
    final String message = String.format(Locale.US, format, params);
    System.out.printf(Locale.US, "[  client] [%s] %s%n", getFormattedTimestamp(), message);
    if (!systemOutOnly) {
      String s = "";
      if (decorate) {
        final StringBuilder sb = new StringBuilder("*****");
        for (int i=0; i<message.length()-10; i++) sb.append('-');
        sb.append("*****");
        s = sb.toString();
      }
      if (decorate) log.info(s);
      log.info(message);
      if (decorate) log.info(s);
    }
  }

  /**
   * Get the current timestamp as a formatted string.
   * @return the timestamp formatted according to {@link #SDF}.
   */
  public static String getFormattedTimestamp() {
     return getFormattedTimestamp(new Date());
  }

  /**
   * Get the current timestamp as a formatted string.
   * @param timestamp the timestamp to format.
   * @return the timestamp formatted according to {@link #SDF}.
   */
  public static String getFormattedTimestamp(final long timestamp) {
     return getFormattedTimestamp(new Date(timestamp));
  }

  /**
   * Get the current timestamp as a formatted string.
   * @param date the date to format.
   * @return the timestamp formatted according to {@link #SDF}.
   */
  public static String getFormattedTimestamp(final Date date) {
    synchronized(SDF) {
      return SDF.format(date);
    }
  }

  /**
   * Throw the specified {@link Throwable} by either casting to its real type or wrapping it in a {@link RuntimeException}.
   * @param t the {@link Throwable} to handle.
   * @throws Exception if {@code t} is not null.
   */
  public static void throwUnknown(final Throwable t) throws Exception {
    if (t instanceof Exception) throw (Exception) t;
    else if (t instanceof Error) throw (Error) t;
    else if (t != null) throw new RuntimeException(t);
  }

  /**
   * Execute a script on the specified driver.
   * @param jmx JMX wrapper for the driver.
   * @param script the script to execute.
   * @return whatever the script returns.
   * @throws Exception if any error occurs.
   */
  public static Object executeScriptOnServer(final JMXDriverConnectionWrapper jmx, final String script) throws Exception {
    return jmx.invoke("org.jppf:name=debug,type=driver", "executeScript", new Object[] { "javascript", script}, new String[] {"java.lang.String", "java.lang.String"});
  }

  /**
   * Print the specified messages in the specified driver's log.
   * @param jmx JMX connection to the driver.
   * @param msg the message format to print.
   * @param params the message parameters.
   * @throws Exception if any error occurs.
   */
  public static void logInServer(final JMXDriverConnectionWrapper jmx, final String msg, final Object...params) throws Exception {
    final String[] messages = { String.format(msg, params) };
    jmx.invoke("org.jppf:name=debug,type=driver", "log", new Object[] { messages }, new String[] { String[].class.getName() });
  }

  /**
   * Print the specified messages in the specified driver's log.
   * @param jmx JMX connection to the driver.
   * @param messages the messages to print.
   * @throws Exception if any error occurs.
   */
  public static void logInServer(final JMXDriverConnectionWrapper jmx, final String...messages) throws Exception {
    jmx.invoke("org.jppf:name=debug,type=driver", "log", new Object[] { messages }, new String[] { String[].class.getName() });
  }

  /** */
  public static class BaseTestClassWatcher extends TestWatcher {
    @Override
    protected void starting(final Description description) {
      // delete the drivers and nodes log files if they exist
      org.apache.log4j.LogManager.resetConfiguration();
      final File dir = new File(System.getProperty("user.dir"));
      final File[] logFiles = dir.listFiles(logFileFilter);
      if (logFiles != null) {
        for (final File file: logFiles) {
          if (file.exists()) {
            if (!file.delete()) {
              print(true, true, "Could not delete %s%n", file);
            }
          }
        }
      }
      final File slavesDir = new File(dir, "slave_nodes");
      if (slavesDir.exists() && !deletePath(slavesDir)) print(true, true, "Could not delete '%s'", slavesDir);
      org.apache.log4j.PropertyConfigurator.configure("classes/tests/config/log4j-client.properties");
      // redirect System.out and System.err to files
      stdOut = System.out;
      stdErr = System.err;
      try {
        System.setOut(new PrintStream("std_out.log"));
        System.setErr(new PrintStream("std_err.log"));
      } catch (final Exception e) {
        print("Error redirecting std_out or std_err: %s", ExceptionUtils.getStackTrace(e));
      }
      print("***** start of class %s *****", description.getClassName());
    }

    @Override
    protected void finished(final Description description) {
      print("***** finished class %s *****", description.getClassName());
      try {
        // redirect System.out and System.err back to their original destination
        if ((stdOut != null) && (stdOut != System.out)) {
          final PrintStream tmp = System.out;
          System.setOut(stdOut);
          tmp.close();
        }
        if ((stdErr != null) && (stdErr != System.err)) {
          final PrintStream tmp = System.err;
          System.setErr(stdErr);
          tmp.close();
        }
      } catch (final Exception e) {
        print("Error restoring std_out or std_err: %s", ExceptionUtils.getStackTrace(e));
      }
      final File dir = new File(System.getProperty("user.dir"));
      final File slavesDir = new File(dir, "slave_nodes");
      if (slavesDir.exists() && slavesDir.isDirectory()) {
        final File[] subdirs = slavesDir.listFiles(new FileFilter() {
          @Override
          public boolean accept(final File file) {
            return file.isDirectory();
          }
        });
        if (subdirs != null) {
          for (final File subdir: subdirs) {
            final File[] logFiles = subdir.listFiles(logFileFilter);
            if (logFiles != null) {
              for (final File logFile: logFiles) {
                final String path = subdir.getName() + "_" + logFile.getName();
                try {
                  StreamUtils.copyFile(logFile, new File(dir, path));
                } catch (final IOException e) {
                  print("Error copying '%s' to '%s': %s", ExceptionUtils.getStackTrace(e));
                }
              }
            }
          }
        }
      }
      zipLogs(description.getClassName());
    }
  }

  /**
   * Check the result of a comparison and throw an {@code AssertionError} if the comparison fails (i.e. return true).
   * @param op the comparison operator to use.
   * @param expected the expected value to test against.
   * @param actual the actual value to test.
   */
  public static void assertCompare(final ComparisonOperator op, final long expected, final long actual) {
    //if (!op.evaluate(actual, expected)) Assert.fail(String.format("expected <%d> to be '%s' <%d>", actual, op, expected));
    if (!op.evaluate(actual, expected)) Assert.fail(String.format("expected: <%s %d> but was: <%d>", op, expected, actual));
  }

  /**
   * Assert that an {@code ExceptionRunnable} throws an expected {@code Throwable}.
   * @param expectedClass the class of the expected throwable.
   * @param runnable the {@code Callable} to check.
   */
  public static void assertThrows(final Class<? extends Throwable> expectedClass, final ExceptionRunnable runnable) {
    try {
      runnable.run();
      Assert.fail(String.format("Expected <%s> to be thrown, but got no exception", expectedClass.getName()));
    } catch (final Throwable e) {
      final Class<? extends Throwable> actualClass = e.getClass();
      Assert.assertTrue(String.format("Expected <%s> to be thrown, but got <%S>", expectedClass.getName(), actualClass.getName()), expectedClass.isAssignableFrom(actualClass));
    }
  }

  /** */
  public static interface ExceptionRunnable {
    /**
     * @throws Exception if any error occurs.
     */
    void run() throws Exception;
  }

  /**
   * Delete the specified path, recursively if this is a directory.
   * @param path the path to delete.
   * @return true if the folder and all contained files and subfolders were deleted, false otherwise.
   */
  private static boolean deletePath(final File path) {
    if ((path == null) || !path.exists()) return false;
    boolean success = true;
    try {
      if (path.isDirectory()) {
        final File[] files = path.listFiles();
        if (files != null) {
          for (final File child: files) {
            if (!deletePath(child)) success = false;
          }
        }
      }
      if (!path.delete()) success = false;
    } catch (@SuppressWarnings("unused") final Exception e) {
      success = false;
    }
    return success;
  }
}
