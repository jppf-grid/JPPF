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

package sample.allinone;

import org.jppf.node.NodeRunner;
import org.jppf.server.JPPFDriver;

/**
 * Sample code demonstrating how to run a driver and node in the same JVM.
 * @author Laurent Cohen
 */
public class AllInOne
{
  /**
   * Entry point for this program.
   * @param args no used.
   */
  public static void main(final String... args)
  {
    try
    {
      Thread driverThread = new Thread()
      {
        @Override
        public void run()
        {
          JPPFDriver.main("noLauncher");
        }
      };
      Thread nodeThread = new Thread()
      {
        @Override
        public void run()
        {
          NodeRunner.main("noLauncher");
        }
      };
      driverThread.start();
      System.out.println("Driver started");
      nodeThread.start();
      System.out.println("Node started");
      driverThread.join();
      nodeThread.join();
    }
    catch (Throwable t)
    {
      t.printStackTrace();
    }
  }
}
