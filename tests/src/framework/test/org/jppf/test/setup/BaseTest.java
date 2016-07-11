/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.location.*;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class BaseTest {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger("TEST");
  /** */
  private static String name;
  /** */
  private static FileFilter logFileFilter = new FileFilter() {
    @Override
    public boolean accept(final File path) {
      if (path.isDirectory()) return false;
      String s = path.getName();
      return (s != null) && s.endsWith(".log") && !s.startsWith("jppf-client");
    }
  };

  /** */
  @ClassRule
  public static TestWatcher classWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      print("***** start of class %s *****", description.getClassName());
      // delete the drivers and nodes log files if they exist
      File dir = new File(System.getProperty("user.dir"));
      File[] logFiles = dir.listFiles(logFileFilter);
      if (logFiles != null) {
        for (File file: logFiles) {
          if (file.exists() && !file.delete()) System.err.printf("Could not delete %s%n", file);
        }
      }
    }

    @Override
    protected void finished(final Description description) {
      print("***** finished class %s *****", description.getClassName());
      zipLogs(description.getClassName());
    }
  };

  /** */
  @Rule
  public TestWatcher instanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      print("***** start of method %s() *****", description.getMethodName());
    }
  };

  /**
   * Zip the drivers and nodes log files into the file {@code logs/<className>.zip}.
   * @param className the name of the class for which to zip the logs.
   */
  private static void zipLogs(final String className) {
    FileLocation src = new FileLocation("jppf-client.log");
    try {
      src.copyTo(new FileLocation(new File("client.log")));
    } catch (Exception e) {
      e.printStackTrace();
    }
    File dir = new File(System.getProperty("user.dir"));
    File[] logFiles = dir.listFiles(logFileFilter);
    if ((logFiles == null) || (logFiles.length <= 0)) return;
    File logDir = new File(dir, "logs/");
    if (!logDir.exists()) logDir.mkdirs();
    File outZip = new File(logDir, className + ".zip");
    String[] logPaths = new String[logFiles.length];
    for (int i=0; i<logFiles.length; i++) logPaths[i] = logFiles[i].getPath();
    ZipUtils.zipFile(outZip.getPath(), logPaths);
  }

  /**
   * Print a formatted message to the shell output and to the log file.
   * @param format the format.
   * @param params the parameter values.
   */
  private static void print(final String format, final Object...params) {
    String message = String.format(format, params);
    System.out.println(message);
    StringBuilder sb = new StringBuilder("*****");
    for (int i=0; i<message.length()-10; i++) sb.append('-');
    sb.append("*****");
    String s = sb.toString();
    log.info(s);
    log.info(message);
    log.info(s);
  }
}
