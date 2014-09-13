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

package org.jppf.example.datadependency;

import org.jppf.utils.JPPFConfiguration;


/**
 * Runner for the sample.
 * @author Laurent Cohen
 */
public class DataDependency
{
  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments.
   */
  public static void main(final String...args)
  {
    try
    {
      // create the JPPFClient
      AbstractTradeUpdater.openJPPFClient();
      String s = JPPFConfiguration.getProperties().getString("runMode", "event");

      // create a runner instance.
      AbstractTradeUpdater tradeUpdater = "event".equalsIgnoreCase(s) ? new EventBasedTradeUpdater() : new SnapshotBasedTradeUpdater();
      tradeUpdater.run();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      AbstractTradeUpdater.closeJPPFClient();
    }
  }
}
