/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.test.setup;

import java.io.*;

import org.jppf.test.scenario.ScenarioConfiguration;

/**
 * Used to launch a single node.
 * @author Laurent Cohen
 */
public class RestartableNodeProcessLauncher extends RestartableProcessLauncher
{
  /**
   * Initialize the node launcher.
   * @param n the id of the node, used to determine which configuration files to use.
   * @param scenarioConfig the scenario configuration.
   */
  public RestartableNodeProcessLauncher(final int n, final ScenarioConfiguration scenarioConfig)
  {
    super(n, "  node", scenarioConfig);
    setJppfConfig(doConfigOverride("node.template.properties", "driver-" + n + ".properties"));
    setJVMOptions();
    String s = doConfigOverride("log4j-node.template.properties", "log4j-node-" + n + ".properties");
    try
    {
      setLog4j(new File(s).toURI().toURL().toString());
    }
    catch(IOException e)
    {
      throw new RuntimeException(e);
    }
    setLogging(doConfigOverride("logging-node.template.properties", "logging-node-" + n + ".properties"));
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
