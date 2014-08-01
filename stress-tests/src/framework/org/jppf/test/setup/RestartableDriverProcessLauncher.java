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
 * Used to launch a driver.
 * @author Laurent Cohen
 */
public class RestartableDriverProcessLauncher extends RestartableProcessLauncher {
  /**
   * Initialize the driver launcher.
   * @param n the id of the driver, used to determine which configuration files to use.
   * @param scenarioConfig the scenario configuration.
   */
  public RestartableDriverProcessLauncher(final int n, final ScenarioConfiguration scenarioConfig) {
    super(n, "driver", scenarioConfig);
    setJppfConfig(doConfigOverride("driver.template.properties", "driver-" + n + ".properties"));
    setJVMOptions();
    String s = doConfigOverride("log4j-driver.template.properties", "log4j-driver-" + n + ".properties");
    try {
      setLog4j(new File(s).toURI().toURL().toString());
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
    setLogging(doConfigOverride("logging-driver.template.properties", "logging-driver-" + n + ".properties"));
    setupCommon();
  }

  /**
   * Perform setup common to all ocnfigurations.
   */
  private void setupCommon() {
    setMainClass("org.jppf.server.JPPFDriver");
    addClasspathElement("classes/tests/config");
    addClasspathElement("../server/classes");
    addJvmOption("-Djava.util.logging.config.file=classes/tests/config/logging-driver.properties");
    //addJvmOption("-Xrunjdwp:transport=dt_socket,address=localhost:800" + n +",server=y,suspend=y");
  }
}
