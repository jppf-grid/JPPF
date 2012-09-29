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

import org.jppf.client.*;
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
      jppfClient = new JPPFClient();
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
    int nbTasks = 40;
    int nbJobs = 2;
    //int maxNodes = Integer.MAX_VALUE;
    int maxChannels = 1;
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
      job.setResultListener(new JPPFResultCollector(job));
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
        MyCallable mc = new MyCallable(getId());
        String s = compute(mc);
        System.out.println("[node] result of MyCallable[id=" + getId() + "].call() = " + s);
        setResult(s);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        setException(e);
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
      System.out.println("[client] result of MyCallable[id=" + id + "].call() = " + callableResult);
      synchronized(this)
      {
        wait(100L);
      }
      return callableResult;
    }
  }
}
