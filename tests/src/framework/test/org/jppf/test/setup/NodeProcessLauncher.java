/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.jppf.utils.TypedProperties;

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
   * @param jppfConfig the JPPF configuration to use for the process.
   */
  public NodeProcessLauncher(final int n, final TypedProperties jppfConfig)
  {
    super(n, "  node");
    setJppfConfig(ConfigurationHelper.createTempConfigFile(jppfConfig));
    setLog4j(getFileURL(ConfigurationHelper.createTempConfigFile(ConfigurationHelper.createConfigFromTemplate("classes/tests/config/log4j-node.template.properties", n))));
    setupCommon();
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
