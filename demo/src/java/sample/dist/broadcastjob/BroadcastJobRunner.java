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
package sample.dist.broadcastjob;

import static org.jppf.utils.StringUtils.*;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.classloader.*;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the &quot;Nroadcast Job&quot; demo.
 * @author Laurent Cohen
 */
public class BroadcastJobRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(BroadcastJobRunner.class);
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
      jppfClient = new JPPFClient();
      jppfClient.setLocalExecutionEnabled(true);
      TypedProperties props = JPPFConfiguration.getProperties();
      int nbTask = 1;
      int iterations = 2;
      print(buildString("Running Broadcast Job demo with ", nbTask, " for ", iterations, " iterations"));
      perform(nbTask, iterations);
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
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @param nbTasks the number of tasks to send at each iteration.
   * @param iterations the number of times the the tasks will be sent.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform(final int nbTasks, final int iterations) throws Exception
  {
    try
    {
      // perform "iteration" times
      long totalTime = 0L;
      for (int iter=1; iter<=iterations; iter++)
      {
        long start = System.currentTimeMillis();
        // create a task for each row in matrix a
        JPPFJob job = new JPPFJob();
        job.setName("broadcast " + iter);
        //job.getJobSLA().setMaxNodes(1);
        for (int i=1; i<=nbTasks; i++)
        {
          BroadcastTask task = new BroadcastTask();
          task.setId("" + iter + " : " + i);
          job.addTask(task);
        }
        job.getSLA().setBroadcastJob(true);
        // submit the tasks for execution
        List<JPPFTask> results = jppfClient.submit(job);
        for (JPPFTask task: results)
        {
          Exception e = task.getException();
          if (e != null) throw e;
        }
        long elapsed = System.currentTimeMillis() - start;
        print("Iteration #"+(iter+1)+" performed in " + toStringDuration(elapsed));
        totalTime += elapsed;
      }
      print("Average iteration time: " + toStringDuration(totalTime/iterations));
    }
    catch(Exception e)
    {
      throw new JPPFException(e.getMessage(), e);
    }
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    log.info(msg);
    System.out.println(msg);
  }

  /**
   * 
   */
  private static class BroadcastTask extends JPPFTask
  {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
      try
      {
        DelegationModel model = JPPFClassLoader.getDelegationModel();
        System.out.println("delegation model: "  + model);
      }
      catch (Throwable t)
      {
        t.printStackTrace();
      }
      System.out.println("broadcast task " + getId());
    }
  }
  
}
