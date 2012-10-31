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
package sample.test.jppfcallable;

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFCallable;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class JPPFCallableRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JPPFCallableRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * Used to test JPPFTask.compute(JPPFCallable) in method {@link #testComputeCallable()}.
   */
  private static String callableResult = "";

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
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
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform() throws Exception
  {
    int nbTasks = 400;
    int nbJobs = 1;
    int maxChannels = 1;
    configure();
    jppfClient = new JPPFClient();
    while (!jppfClient.hasAvailableConnection()) Thread.sleep(20L);
    print("submitting " + nbJobs + " jobs with " + nbTasks + " tasks");
    List<JPPFJob> jobList = new ArrayList<JPPFJob>();
    for (int n=1; n<=nbJobs; n++)
    {
      String name = "job-" + n;
      JPPFJob job = new JPPFJob(name);
      job.getClientSLA().setMaxChannels(maxChannels);
      job.setBlocking(false);
      for (int i=1; i<=nbTasks; i++) job.addTask(new MyTask()).setId(name + ":task-" + i);
      job.setResultListener(new JPPFResultCollector(job)
      {
        @Override
        public synchronized void resultsReceived(final TaskResultEvent event)
        {
          super.resultsReceived(event);
          if (event.getTaskList() != null) System.out.println("received " + jobResults.size() + " results");
        }
      });
      //job.addJobListener(new MyJobListener());
      jobList.add(job);
    }
    callableResult = "from MyCallable";
    for (JPPFJob job: jobList) jppfClient.submit(job);
    for (JPPFJob job: jobList)
    {
      JPPFResultCollector coll = (JPPFResultCollector) job.getResultListener();
      List<JPPFTask> results = coll.waitForResults();
      print("got results for job '" + job.getName() + "'");
    }
  }

  /**
   * Perform optional configuration before creating the JPPF client.
   */
  public static void configure()
  {
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
  public static class MyTask extends JPPFTask
  {
    @Override
    public void run()
    {
      try
      {
        /*
        */
        MyCallable mc = new MyCallable(getId());
        String s = compute(mc);
        //System.out.println("[node] result of MyCallable[id=" + getId() + "].call() = " + s);
        setResult(s);
      }
      catch (Throwable t)
      {
        //t.printStackTrace();
        setException(t instanceof Exception ? (Exception) t : new JPPFException(t));
      }
    }
  }

  /**
   * 
   */
  public static class MyCallable implements JPPFCallable<String>
  {
    /**
     * 
     */
    private String id = null;
    /**
     * 
     */
    private byte[] data = null;

    /**
     * 
     */
    public MyCallable()
    {
    }

    /**
     * 
     * @param id the id of the task.
     */
    public MyCallable(final String id)
    {
      this.id = id;
    }

    @Override
    public String call() throws Exception
    {
      //System.out.println("[client] result of MyCallable[id=" + id + "].call() = " + callableResult);
      data = new byte[1024];
      synchronized(this)
      {
        wait(10L);
      }
      //throw new RuntimeException();
      throw new Error();
      //return callableResult;
    }
  }

  /**
   * 
   */
  public static class MyJobListener extends JobListenerAdapter
  {
    /**
     * 
     */
    private int lastCount = 0;

    @Override
    public void jobReturned(final JobEvent event)
    {
      System.out.println("job '" + event.getJob().getName() + "' returned");
      JPPFJob job = event.getJob();
      int size = job.getResults().size();
      if (size - lastCount > 100)
      {
        System.out.println("received " + size + " tasks for job '" + job.getName() + "'");
        lastCount = size;
      }
    }

    @Override
    public void jobStarted(final JobEvent event)
    {
      System.out.println("job '" + event.getJob().getName() + "' started");
    }

    @Override
    public void jobEnded(final JobEvent event)
    {
      System.out.println("job '" + event.getJob().getName() + "' ended");
    }

    @Override
    public void jobDispatched(final JobEvent event)
    {
      System.out.println("job '" + event.getJob().getName() + "' dispatched");
    }
  }
}
