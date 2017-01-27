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

package org.jppf.test.scenario.resubmit;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

import test.org.jppf.test.setup.common.*;

/**
 * Testing the resubmission of a job when the driver is disconnected.
 * @author Laurent Cohen
 */
public class ResubmitRunner extends AbstractScenarioRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ResubmitRunner.class);
  /**
   * 
   */
  private final AtomicBoolean dispatched = new AtomicBoolean(false);

  @Override
  public void run()
  {
    try
    {
      long start = System.nanoTime();
      JPPFJob job = BaseTestHelper.createJob("resubmit", false, false, 1, LifeCycleTask.class, 5000L);
      job.addJobListener(new MyJobListener());
      getSetup().getClient().submitJob(job);
      while (!dispatched.get()) Thread.sleep(1000L);
      getSetup().getDriverManagementProxy().restartShutdown(1L, 1L);
      job.awaitResults();
      long elapsed = System.nanoTime() - start;
      output("total time: " + StringUtils.toStringDuration(elapsed/1000000L));
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

  /**
   * Set the "dispatched" flag when at least one dispatch has occurred.
   */
  public class MyJobListener extends JobListenerAdapter
  {
    @Override
    public void jobDispatched(final JobEvent event)
    {
      dispatched.set(true);
    }
  }
}
