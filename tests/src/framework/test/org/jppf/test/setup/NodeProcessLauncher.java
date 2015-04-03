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

import java.util.List;


/**
 * Used to launch a single node.
 * @author Laurent Cohen
 */
public class NodeProcessLauncher extends GenericProcessLauncher
{
  /**
   * Initialize the node launcher with the specified node id.
   * @param n the id of the node, used to determine which configuration files to use.
   */
  public NodeProcessLauncher(final int n)
  {
    super(n, "  node", "classes/tests/config/node.template.properties", "classes/tests/config/log4j-node.template.properties");
    setupCommon();
  }

  /**
   * Initialize the node launcher.
   * @param n the id of the driver, used to determine which configuration files to use.
   * @param driverConfig the path to the JPPF configuration template file.
   * @param log4jConfig the path to the log4j template file.
   * @param classpath the classpath elements for the node.
   * @param jvmOptions additional JVM options for the node.
   */
  public NodeProcessLauncher(final int n, final String driverConfig, final String log4jConfig, final List<String> classpath, final List<String> jvmOptions)
  {
    super(n, "  node", driverConfig, log4jConfig, classpath, jvmOptions);
    setMainClass("org.jppf.node.NodeRunner");
  }

  /**
   * Perform setup common to all configurations.
   */
  private void setupCommon()
  {
    setMainClass("org.jppf.node.NodeRunner");
    addClasspathElement("classes/tests/config");
    addJvmOption("-Djava.util.logging.config.file=classes/tests/config/logging-node" + n + ".properties");
  }
}
