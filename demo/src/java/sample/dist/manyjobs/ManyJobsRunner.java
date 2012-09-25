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
package sample.dist.manyjobs;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ManyJobsRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ManyJobsRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      TypedProperties props = JPPFConfiguration.getProperties();
      props.setProperty("jppf.discovery.enabled", "true");
      props.setProperty("jppf.pool.size", "17");
      jppfClient = new JPPFClient();
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(10L);
      int length = 50;
      int nbTask = 100;
      int nbJobs = 50;
      print("Running " + nbJobs+ " jobs with " + nbTask + " tasks of length = " + length + " ms");
      perform(nbTask, length, nbJobs);
      //performLong(size, iterations);
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
   * Perform the multiplication of 2 matrices with the specified size, for a specified number of times.
   * @param nbTask the number of tasks to send at each iteration.
   * @param length the executionlength of each task.
   * @param nbJobs the of jobs to submit.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform(final int nbTask, final int length, final int nbJobs) throws Exception
  {
    try
    {
      JPPFJob[] jobs = new JPPFJob[nbJobs];
      for (int n=0; n<nbJobs; n++)
      {
        long start = System.currentTimeMillis();
        // create a task for each row in matrix a
        jobs[n] = new JPPFJob();
        String s = StringUtils.padLeft(""+(n+1), '0', 4);
        jobs[n].setName("JPPF Job " + s);
        jobs[n].setBlocking(false);
        //job.getJobSLA().setMaxNodes(1);
        jobs[n].getClientSLA().setMaxNodes(1);
        for (int i=0; i<nbTask; i++)
        {
          jobs[n].addTask(new LongTask(length, false)).setId("job-" + (n+1) + ':' + (i+1));
        }
        /*
				JPPFSchedule schedule = new JPPFSchedule(5000L);
				job.getJobSLA().setJobSchedule(schedule);
				job.getJobSLA().setSuspended(true);
         */
        // submit the tasks for execution
        JPPFResultCollector collector = new JPPFResultCollector(jobs[n]);
        jobs[n].setResultListener(collector);
        jppfClient.submit(jobs[n]);
      }
      print("submitted " + nbJobs + " jobs");
      for (int n=0; n<nbJobs; n++)
      {
        JPPFResultCollector collector = (JPPFResultCollector) jobs[n].getResultListener();
        List<JPPFTask> results = collector.waitForResults();
      }
      print("got all " + nbJobs + " result lists");
    }
    catch(Exception e)
    {
      throw new JPPFException(e.getMessage(), e);
    }
  }

  /**
   * Print a message to the log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    log.info(msg);
    System.out.println(msg);
  }
}
