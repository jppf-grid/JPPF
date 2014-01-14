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

package org.jppf.test.scenario.offline;

import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class OfflineNodeRunner extends AbstractScenarioRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(OfflineNodeRunner.class);
  /**
   * The count of itearations runs.
   */
  private int iterationsCount = 0;

  @Override
  public void run()
  {
    try
    {
      StreamUtils.waitKeyPressed();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message - the message to print.
   */
  private static void output(final String message)
  {
    System.out.println(message);
    log.info(message);
  }
}
