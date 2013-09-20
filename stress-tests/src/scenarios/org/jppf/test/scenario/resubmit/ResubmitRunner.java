/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.List;
import java.util.concurrent.atomic.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
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
  private static final AtomicInteger RESTART_COUNT = new AtomicInteger(0);

  @Override
  public void run()
  {
    try
    {
      TypedProperties config = getConfiguration().getProperties();
      final int nbTasks = config.getInt("nbTasks");
      long taskDuration = config.getLong("taskDuration");
      long delay = config.getLong("taskDuration");
      long start = System.nanoTime();
      JPPFJob job = BaseTestHelper.createJob("resubmit", false, false, nbTasks, LifeCycleTask.class, taskDuration);
      JPPFResultCollector collector = new JPPFResultCollector(job) {
        @Override
        public synchronized void resultsReceived(final TaskResultEvent event) {
          super.resultsReceived(event);
          if (event.getThrowable() == null) {
            List<JPPFTask> list = event.getTaskList();
            output("received " + list.size() + " tasks, pending=" + (nbTasks - jobResults.size()) + ", results=" + jobResults.size());
          }
        }
      };
      job.setResultListener(collector);
      getSetup().getClient().submit(job);
      List<JPPFTask> results;
      while ((results = collector.waitForResults(5000L)) == null)
      {
        getSetup().getDriverManagementProxy().restartShutdown(1L, 1000L);
        RESTART_COUNT.incrementAndGet();
      }
      long elapsed = System.nanoTime() - start;
      output(job.getName() + " done, driver restarts: " + RESTART_COUNT + ", in " + StringUtils.toStringDuration(elapsed/1000000L));
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
