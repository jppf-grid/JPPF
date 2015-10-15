/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.client.JPPFClient;
import org.junit.AfterClass;

import test.org.jppf.test.setup.BaseSetup.Configuration;

/**
 * Base test setup for testing offline nodes.
 * @author Laurent Cohen
 */
public class AbstractSetupOfflineNode
{
  /**
   * The jppf client to use.
   */
  protected static JPPFClient client = null;

  /**
   * Create the drivers and nodes configuration.
   * @return a {@link Configuration} instance.
   * @throws Exception if a process could not be started.
   */
  protected static Configuration createConfig() throws Exception
  {
    Configuration testConfig = new Configuration();
    List<String> commonCP = new ArrayList<>();
    commonCP.add("classes/addons");
    commonCP.add("classes/tests/config");
    commonCP.add("../node/classes");
    commonCP.add("../common/classes");
    commonCP.add("../server/classes");
    commonCP.add("../JPPF/lib/slf4j/slf4j-api-1.6.1.jar");
    commonCP.add("../JPPF/lib/slf4j/slf4j-log4j12-1.6.1.jar");
    commonCP.add("../JPPF/lib/log4j/log4j-1.2.15.jar");
    commonCP.add("../JPPF/lib/jmxremote/" + BaseSetup.JMX_REMOTE_JAR);
    testConfig.driverJppf = "classes/tests/config/driver.template.properties";
    testConfig.driverLog4j = "classes/tests/config/log4j-driver.template.properties";
    testConfig.driverClasspath = commonCP;
    testConfig.driverJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-driver.properties");
    testConfig.nodeJppf = "classes/tests/config/node-offline.template.properties";
    testConfig.nodeLog4j = "classes/tests/config/log4j-node.template.properties";
    testConfig.nodeClasspath = commonCP;
    testConfig.nodeJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-node1.properties");
    return testConfig;
  }

  /**
   * Stops the driver and nodes and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception
  {
    BaseSetup.cleanup();
  }
}
