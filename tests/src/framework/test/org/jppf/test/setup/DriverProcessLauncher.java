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

package test.org.jppf.test.setup;

import java.util.List;


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
   * @param driverConfig the path to the JPPF configuration template file.
   * @param log4jConfig the path to the log4j template file.
   * @param classpath the classpath elements for the driver.
   * @param jvmOptions additional JVM options for the driver.
   */
  public DriverProcessLauncher(final int n, final String driverConfig, final String log4jConfig, final List<String> classpath, final List<String> jvmOptions)
  {
    super(n, "driver", driverConfig, log4jConfig, classpath, jvmOptions);
    setMainClass("org.jppf.server.JPPFDriver");
  }

  /**
   * Perform setup common to all ocnfigurations.
   */
  private void setupCommon()
  {
    setMainClass("org.jppf.server.JPPFDriver");
    addClasspathElement("classes/addons");
    addClasspathElement("classes/tests/config");
    addClasspathElement("../server/classes");
    addClasspathElement("../JPPF/lib/Groovy/groovy-all-1.6.5.jar");
    addJvmOption("-Djava.util.logging.config.file=classes/tests/config/logging-driver.properties");
    //addJvmOption("-Xrunjdwp:transport=dt_socket,address=localhost:800" + n +",server=y,suspend=y");
  }
}
