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

/**
 * Used to launch a driver.
 * @author Laurent Cohen
 */
public class DriverProcessLauncher extends GenericProcessLauncher
{
  /**
   * Initialize the driver launcher.
   */
  public DriverProcessLauncher()
  {
    super();
    setMainClass("org.jppf.server.JPPFDriver");
    //addArgument("noLauncher");
    setJppfConfig("config/driver.properties");
    setLog4j("config/log4j-driver.properties");
    addClasspathElement("test/classes");
    addClasspathElement("../common/classes");
    addClasspathElement("../server/classes");
  }
}
