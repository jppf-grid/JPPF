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

package test.org.jppf.test.runner;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jppf.utils.FileUtils;
import org.junit.runner.JUnitCore;

import test.org.jppf.test.setup.BaseSetup;

/**
 * This class is intended to run the JPPF JUnit tests, either as a standalone application,
 * or by sending a test request to a web application, for testing the JCA connector.
 * @author Laurent Cohen
 */
public class JPPFTestRunner {
  /**
   * Run all the test classes specified in the file.
   * @param classNamesFile contains the names of test classes to run, one per line.
   * @param out used to log the results during execution of the tests.
   * @return a <code>ResultHolder</code> object.
   */
  public ResultHolder runTests(final String classNamesFile, final PrintStream out) {
    ResultHolder result = new ResultHolder();
    InputStream is = null;
    try {
      is = JPPFTestRunner.class.getClassLoader().getResourceAsStream(classNamesFile);
      if (is != null) {
        List<String> list = FileUtils.textFileAsLines(new InputStreamReader(is));
        List<Class<?>> classes = new ArrayList<>(list.size());
        for (String name: list) {
          String s = name.trim();
          if ("".equals(s) || s.startsWith("#")) continue;
          try {
            System.out.println("loading " + s);
            Class<?> clazz = Class.forName(s);
            classes.add(clazz);
          } catch(Exception e) {
            result.addException(new ExceptionHolder(s, e));
          }
        }
        randomizeList(classes);
        System.out.println("test classes " + classes);
        out.println("test classes " + classes);
        JUnitCore core = new JUnitCore();
        TestRunListener listener = new TestRunListener(result, out);
        core.addListener(listener);
        core.run(classes.toArray(new Class<?>[classes.size()]));
      }
      else result.addException(new ExceptionHolder(classNamesFile, new IllegalArgumentException("class names file '" + classNamesFile + "' not found")));
    } catch(Exception e) {
      result.addException(new ExceptionHolder("Exception while attempting to run the tests", e));
    }
    return result;
  }

  /**
   * Serialize the test results to an output stream.
   * @param result the result of the tests execution.
   * @param out the output stream to serialize the rssults to.
   * @throws Exception if any error occurs.
   */
  public void sendResults(final ResultHolder result, final OutputStream out) throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(result);
    oos.flush();
  }

  /**
   * Send a request to run the unit tezsts to the specified web application.
   * @param webAppUrl the URL for the web application.
   * @return a {@link ResultHolder} object holding the test results.
   * @throws Exception if any error occurs.
   */
  public ResultHolder sendTestRequest(final URL webAppUrl) throws Exception {
    try {
      BaseSetup.Configuration config = new BaseSetup.Configuration();
      List<String> commonCP = new ArrayList<>();
      commonCP.add("../jmxremote/classes");
      commonCP.add("../lib/jppf-common.jar");
      commonCP.add("../lib/jppf-node.jar");
      commonCP.add("../lib/slf4j-api-1.6.1.jar");
      commonCP.add("../lib/slf4j-log4j12-1.6.1.jar");
      commonCP.add("../lib/log4j-1.2.15.jar");
      config.driverJppf = "config/driver.template.properties";
      config.driverLog4j = "config/log4j-driver.template.properties";
      config.driverClasspath.addAll(commonCP);
      config.driverClasspath.add("../lib/jppf-server.jar");
      config.driverJvmOptions.add("-Djava.util.logging.config.file=config/logging-driver.properties");
      config.nodeJppf = "config/node.template.properties";
      config.nodeLog4j = "config/log4j-node.template.properties";
      config.nodeClasspath.addAll(commonCP);
      config.nodeJvmOptions.add("-Djava.util.logging.config.file=config/logging-node1.properties");
      BaseSetup.setup(1, 1, false, config);
      HttpURLConnection conn = (HttpURLConnection) webAppUrl.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("GET");
      conn.connect();
      InputStream in = conn.getInputStream();
      ObjectInputStream ois = new ObjectInputStream(in);
      Object o = ois.readObject();
      return (ResultHolder) o;
    } finally {
      BaseSetup.cleanup();
    }
  }

  /**
   * Run the tests standalone or on a remote application server and display the results.
   * @param args arguments specifying how the tests are run.
   */
  public static void main(final String[] args) {
    int exitCode = 0;
    try {
      ResultHolder result = null;
      String type = args[0];
      String outputFile = "tests-results.txt";
      if ("-s".equals(type)) {
        System.out.println("Running standalone tests");
        if ((args.length > 1) && (args[1] != null)) outputFile = args[1];
        PrintStream out = new PrintStream(new FileOutputStream("test-output.txt"));
        result = new JPPFTestRunner().runTests("TestClasses.txt", out);
        out.flush();
        out.close();
      } else if ("-u".equals(type)) {
        System.out.println("Running tests at " + args[1]);
        URL url = new URL(args[1]);
        if ((args.length > 2) && (args[2] != null)) outputFile = args[2];
        result = new JPPFTestRunner().sendTestRequest(url);
      }
      else printUsage();
      if (!result.getExceptions().isEmpty() || (result.getFailureCount() > 0)) exitCode = 1;
      TestResultRenderer renderer = new TextResultRenderer(result);
      renderer.render();
      String s = new StringBuilder(renderer.getHeader()).append(renderer.getBody()).toString();
      System.out.println(s);
      FileUtils.writeTextFile(outputFile, s);
      System.out.println("test results are stored in file '" + outputFile + "'");
    } catch (Throwable t) {
      t.printStackTrace();
    }
    System.exit(exitCode);
  }

  /**
   * Randomize the elements of the specified list.
   * @param <T> the type of the elements in the list.
   * @param list the input list.
   */
  private <T> void randomizeList(final List<T> list) {
    List<T> result = new ArrayList<>(list.size());
    Random rand = new Random(System.nanoTime());
    while (!list.isEmpty()) {
      int n = rand.nextInt(list.size());
      result.add(list.remove(n));
    }
    list.addAll(result);
  }

  /**
   * Print usage instructions for this runner.
   */
  private static void printUsage() {
    System.out.println("Usage is one of the following: \n");
    System.out.println("  -s     [output_file] : run standalone tests");
    System.out.println("  -u url [output_file] : run the test web app at the specfied url");
    System.out.println("  [output_file] is an optional output file where results are stored, defaults to \"test-results.txt\"");
  }
}
