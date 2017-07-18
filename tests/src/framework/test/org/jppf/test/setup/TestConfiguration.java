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
   * Path to the driver JPPF config
   */
  public String driverJppf = "";
  /**
   * Path to the driver log4j config
   */
  public String driverLog4j = "";
  /**
   * Driver classpath elements.
   */
  public List<String> driverClasspath = new ArrayList<>();
  /**
   * Driver JVM options.
   */
  public List<String> driverJvmOptions = new ArrayList<>();
  /**
   * Path to the node JPPF config
   */
  public String nodeJppf = "";
  /**
   * Path to the node log4j config
   */
  public String nodeLog4j = "";
  /**
   * Node classpath elements.
   */
  public List<String> nodeClasspath = new ArrayList<>();
  /**
   * Node JVM options.
   */
  public List<String> nodeJvmOptions = new ArrayList<>();
  /** */
  public String clientConfig = "classes/tests/config/client.properties";

  /**
   * Copy this configuration to a new instance.
   * @return a {@link TestConfiguration} instance.
   */
  public TestConfiguration copy() {
    TestConfiguration copy = new TestConfiguration();
    copy.driverJppf = driverJppf;
    copy.driverLog4j = driverLog4j;
    copy.driverClasspath = new ArrayList<>(driverClasspath);
    copy.driverJvmOptions = new ArrayList<>(driverJvmOptions);
    copy.nodeJppf = nodeJppf;
    copy.nodeLog4j = nodeLog4j;
    copy.nodeClasspath = new ArrayList<>(nodeClasspath);
    copy.nodeJvmOptions = new ArrayList<>(nodeJvmOptions);
    copy.clientConfig = clientConfig;
    return copy;
  }
}