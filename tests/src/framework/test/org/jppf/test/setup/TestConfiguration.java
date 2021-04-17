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

import java.util.*;

/** */
public class TestConfiguration {
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
  public String clientConfig = "classes/tests/config/client.properties";

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
   * Create the default configuratin used when none is specified.
   * @return a {@link TestConfiguration} instance.
   */
  public static TestConfiguration newDefault() {
    final TestConfiguration config = new TestConfiguration();
    final List<String> commonCP = new ArrayList<>();
    final String dir = "classes/tests/config";
    commonCP.add("classes/addons");
    commonCP.add(dir);
    commonCP.add("../node/classes");
    commonCP.add("../common/classes");
    commonCP.add("../jmxremote-nio/classes");
    commonCP.add("../JPPF/lib/slf4j/slf4j-api-" + BaseSetup.SLF4J_VERSION + ".jar");
    commonCP.add("../JPPF/lib/slf4j/slf4j-log4j12-" + BaseSetup.SLF4J_VERSION + ".jar");
    commonCP.add("../JPPF/lib/log4j/*");
    commonCP.add("../JPPF/lib/LZ4/*");
    commonCP.add("../JPPF/lib/ApacheCommons/*");
    commonCP.add("../JPPF/lib/JNA/*");
    commonCP.add("../JPPF/lib/oshi/*");
    commonCP.add("lib/xstream.jar");
    commonCP.add("lib/xpp3_min.jar");
    commonCP.add("lib/xmlpull.jar");
  
    final List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../server/classes");
    driverCP.add("../JPPF/lib/Groovy/*");
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
}
