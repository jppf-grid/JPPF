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

import java.util.Map;

/**
 * Used to launch a driver.
 * @author Laurent Cohen
 */
public class DriverProcessLauncher extends GenericProcessLauncher {
  /**
   * Initialize the driver launcher.
   * @param n the id of the driver, used to determine which configuration files to use.
   * @param config the process configuration.
   * @param bindings variable bindings used in 'expr:' script expressions.
   */
  public DriverProcessLauncher(final int n, final TestConfiguration.ProcessConfig config, final Map<String, Object> bindings) {
    super(n, "driver", config, bindings);
    addClasspathElement("target/lib/HikariCP-java7-2.4.11.jar");
    setMainClass("org.jppf.server.JPPFDriver");
  }
}
