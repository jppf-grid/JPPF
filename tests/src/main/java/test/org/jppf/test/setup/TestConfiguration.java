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

import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jppf.JPPFError;

/** */
public class TestConfiguration {
  /**
   * The list of dependencies.
   */
  public static final List<String> JARS = getJars();
  /**
   * The driver configuration.
   */
  public ProcessConfig driver = new ProcessConfig();
  /**
   * The node configuration.
   */
  public ProcessConfig node = new ProcessConfig();
  /**
   * Path to the client config file.
   */
  public String clientConfig = BaseTest.CONFIG_ROOT_DIR + "client.properties";

  /**
   * Copy this configuration to a new instance.
   * @return a {@link TestConfiguration} instance.
   */
  public TestConfiguration copy() {
    final TestConfiguration copy = new TestConfiguration();
    copy.driver = driver.copy();
    copy.node = node.copy();
    copy.clientConfig = clientConfig;
    return copy;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("clientConfig=").append(clientConfig)
      .append(", driver=[").append(driver).append(']')
      .append(", node=[").append(node).append(']')
      .toString();
  }

  /**
   * Retrieve the list of dependcencies.
   * @return a list of jar file paths.
   */
  private static List<String> getJars() {
    try {
      final List<String> jars = Files.walk(Paths.get("target/lib"))
        .filter(path -> !Files.isDirectory(path))
        .map(path -> path.toString())
        .collect(Collectors.toList());
      return jars;
    } catch(final Exception e) {
      throw new JPPFError(e);
    }
  }

  /**
   * Create the default configuratin used when none is specified.
   * @return a {@link TestConfiguration} instance.
   */
  public static TestConfiguration newDefault() {
    final TestConfiguration config = new TestConfiguration();
    final List<String> commonCP = new ArrayList<>();
    final String dir = BaseTest.CONFIG_ROOT_DIR;
    
    commonCP.add("target/classes");
    commonCP.add(dir);
    commonCP.add("../node/target/classes");
    commonCP.add("../common/target/classes");
    commonCP.add("../jmxremote-nio/target/classes");
    commonCP.addAll(getMatches(JARS, "*slf4j*"));
    commonCP.addAll(getMatches(JARS, "*log4j*"));
    commonCP.addAll(getMatches(JARS, "*lz4*"));
    commonCP.addAll(getMatches(JARS, "*jna*"));
    commonCP.addAll(getMatches(JARS, "*oshi*"));
    commonCP.addAll(getMatches(JARS, "*commons-io*"));
    commonCP.add("lib/xstream.jar");
    commonCP.add("lib/xpp3_min.jar");
    commonCP.add("lib/xmlpull.jar");
  
    final List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../server/target/classes");
    driverCP.addAll(getMatches(JARS, "*groovy*"));
    config.driver.jppf = dir + "/driver.template.properties";
    config.driver.log4j = dir + "/log4j-driver.template.properties";
    config.driver.classpath = driverCP;
    config.node.jppf = dir + "/node.template.properties";
    config.node.log4j = dir + "/log4j-node.template.properties";
    config.node.classpath = commonCP;
    config.clientConfig = dir + "/client.properties";
    return config;
  }

  /**
   * Find all tne entries in the input list that match the specified wildcard pattern.
   * @param source the source of entries to match.
   * @param pattern the pattern to match against.
   * @return a list, possibly empty but never null, of matching entries.
   */
  public static List<String> getMatches(final List<String> source, final String pattern) {
    final Pattern regex = Pattern.compile(pattern.replace("*", ".*").replace("?", "."));
    final List<String> result = new ArrayList<>();
    source.stream().filter(entry -> regex.matcher(entry).matches()).forEach(result::add);
    return result;
  }

  /**
   * Config for the drivers or nodes.
   */
  public static class ProcessConfig {
    /**
     * Path to the JPPF config.
     */
    public String jppf = "";
    /**
     * Path to the log4j config.
     */
    public String log4j = "";
    /**
     * Classpath elements.
     */
    public List<String> classpath = new ArrayList<>();
    /**
     * JVM options.
     */
    public List<String> jvmOptions = new ArrayList<>();

    /**
     * Copy this configuration to a new instance.
     * @return a new {@link ProcessConfig} instance.
     */
    public ProcessConfig copy() {
      final ProcessConfig copy = new ProcessConfig();
      copy.jppf = jppf;
      copy.log4j = log4j;
      copy.classpath = new ArrayList<>(classpath);
      copy.jvmOptions = new ArrayList<>(jvmOptions);
      return copy;
    }

    @Override
    public String toString() {
      return new StringBuilder().append("jppf=").append(jppf)
        .append(", log4j=").append(log4j)
        .append(", classpath=").append(classpath)
        .append(", jvmOptions=").append(jvmOptions)
        .toString();
    }
  }

  public static void main(final String[] args) throws Throwable {
    System.out.println("JARS = " + JARS);
    final String pattern = "*slf4j*";
    System.out.println("JARS matches for '" + pattern + "' = " + getMatches(JARS, pattern));
  }
}
