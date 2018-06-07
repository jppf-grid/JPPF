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