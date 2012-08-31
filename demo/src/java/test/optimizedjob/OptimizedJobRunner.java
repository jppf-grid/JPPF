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
package test.optimizedjob;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class OptimizedJobRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(OptimizedJobRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      jppfClient = new JPPFClient();
      /*
			while (!jppfClient.hasAvailableConnection()) Thread.sleep(50L);
			jppfClient.setLocalExecutionEnabled(true);
       */
      perform();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform() throws Exception
  {
    int nbJobs = 1;
    int nbTasks = 10;
    long time = 2000L;

    output("Running demo with time = " + time + " for " + nbJobs + " jobs");
    long totalTime = System.currentTimeMillis();
    List<JPPFJob> jobs = new ArrayList<JPPFJob>();
    for (int i=0; i<nbJobs; i++)
    {
      final JPPFJob job = new JPPFJob();
      job.addJobListener(new JobListener() {
        @Override
        public void jobStarted(final JobEvent event) {
          output("Job '" + job.getName() + "' starting");
        }
        @Override
        public void jobEnded(final JobEvent event) {
          output("Job '" + job.getName() + "' ended");
        }
      });
      job.setName("demo job " + (i+1));
      for (int j=0; j<nbTasks; j++) job.addTask(new OptimizedJobTask(time, (j+1)));
      JPPFResultCollector collector = new JPPFResultCollector(job);
      job.setResultListener(collector);
      job.setBlocking(false);
      jobs.add(job);
      jppfClient.submit(job);
    }
    output("" + nbJobs + " job" + (nbJobs > 1 ? "s" : "") + " submitted");
    for (JPPFJob job: jobs)
    {
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      List<JPPFTask> results = collector.waitForResults();
      output("\n***** results for job " + job.getName() + " *****\n");
      for (JPPFTask t: results) output((String) t.getResult());
    }
    totalTime = System.currentTimeMillis() - totalTime;
    output("Computation time: " + StringUtils.toStringDuration(totalTime));
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
