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

package org.jppf.test.scenario.s3;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.slf4j.*;

import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * 
 * @author Laurent Cohen
 */
public class S3Runner extends AbstractScenarioRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(S3Runner.class);
  /**
   * The count of itearations runs.
   */
  private int iterationsCount = 0;

  @Override
  public void run()
  {
    try
    {
      TypedProperties props = getConfiguration().getProperties();
      int nbJobs = props.getInt("nbJobs", 1);
      int nbTasks = props.getInt("nbTasks", 1);
      long count = props.getInt("count", 1);
      output("submitting " + nbJobs + " jobs with " + nbTasks + " each, count=" + count);
      //StreamUtils.waitKeyPressed("press any key to continue ...");
  
      long totalIterationTime = 0L;
      long min = Long.MAX_VALUE;
      long max = 0L;
  
      for (int i=1; i<=nbJobs; i++)
      {
        long start = System.nanoTime();
        JPPFJob job = BaseTestHelper.createJob("S3-job-" + i, true, false, nbTasks, MyTask.class, count);
        job.getMetadata().setParameter("job.name", "job-" + i);
        List<Task<?>> results = getSetup().getClient().submitJob(job);
        long elapsed = (System.nanoTime() - start) / 1000000L;
        if (elapsed < min) min = elapsed;
        if (elapsed > max) max = elapsed;
        totalIterationTime += elapsed;
        output("Iteration #" + i + " performed in " + StringUtils.toStringDuration(elapsed));
      }
      output("Average iteration time: " + StringUtils.toStringDuration(totalIterationTime / nbJobs) +
          ", min = " + StringUtils.toStringDuration(min) + ", max = " + StringUtils.toStringDuration(max) + 
          ", total time: " + StringUtils.toStringDuration(totalIterationTime));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private static void output(final String message)
  {
    System.out.println(message);
    log.info(message);
  }
}
