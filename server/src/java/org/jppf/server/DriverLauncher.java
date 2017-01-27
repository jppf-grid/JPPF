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
package org.jppf.server;

import org.jppf.process.ProcessLauncher;

/**
 * <p>This class is intended as a controller for the JPPF driver, to enable stopping and restarting it when requested.
 * <p>It performs the following operations:
 * <ul>
 * <li>open a server socket the driver will listen to (port number is dynamically attributed)</li>
 * <li>Start the JPPF driver as a subprocess, sending the the server socket port number as an argument</li>
 * <li>Wait for the subprocess to exit</li>
 * <li>If the subprocess exit code is equal to 2, the subprocess is restarted</li>
 * </ul>
 * @author Laurent Cohen
 * @exclude
 */
public class DriverLauncher
{
  /**
   * Start this application, then the JPPF driver a subprocess.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      ProcessLauncher launcher = new ProcessLauncher("org.jppf.server.JPPFDriver", false);
      launcher.run();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    System.exit(0);
  }
}
