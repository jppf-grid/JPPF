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
 * Used to launch a driver.
 * @author Laurent Cohen
 */
public class DriverProcessLauncher extends GenericProcessLauncher
{
  /**
   * Initialize the driver launcher.
   * @param n the id of the driver, used to determine which configuration files to use.
   */
  public DriverProcessLauncher(final int n)
  {
    super(n, "driver", "classes/tests/config/driver.template.properties", "classes/tests/config/log4j-driver.template.properties");
    setupCommon();
  }

  /**
   * Initialize the driver launcher.
   * @param n the id of the driver, used to determine which configuration files to use.
   * @param jppfConfig the JPPF configuration to use for the process.
   */
  public DriverProcessLauncher(final int n, final TypedProperties jppfConfig)
  {
    super(n, "driver");
    setJppfConfig(createTempConfigFile(jppfConfig));
    setLog4j("file:/" + createTempConfigFile(createConfigFromTemplate("classes/tests/config/log4j-driver.template.properties", n)));
    setupCommon();
  }

  /**
   * Perform setup common to all ocnfigurations.
   */
  private void setupCommon()
  {
    setMainClass("org.jppf.server.JPPFDriver");
    addClasspathElement("classes/tests/config");
    addClasspathElement("../common/classes");
    addClasspathElement("../server/classes");
    addJvmOption("-Djava.util.logging.config.file=classes/tests/config/logging-driver.properties");
    //addJvmOption("-Xrunjdwp:transport=dt_socket,address=localhost:800" + n +",server=y,suspend=y");
  }
}
