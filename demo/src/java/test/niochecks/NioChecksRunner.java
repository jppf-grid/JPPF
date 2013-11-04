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
package test.niochecks;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;

/**
 * This is a template JPPF application runner.
 * @author Laurent Cohen
 */
public class NioChecksRunner
{
  /**
   * The JPPF client, handles all communications with the server.
   */
  private static JPPFClient jppfClient =  null;

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments.
   */
  public static void main(final String...args)
  {
    try
    {
      /*
      //JPPFConfiguration.getProperties().setProperty("jppf.pool.size", "2");
      System.out.println("initial start and close");
			jppfClient = new JPPFClient("Always the same UUID");
      Thread.sleep(2000L);
			jppfClient.close();
      System.out.println("restarting node");
      restartNode();
      Thread.sleep(2000L);
       */
      System.out.println("submitting jobs");
      JPPFConfiguration.getProperties().setProperty("jppf.pool.size", "1");
      jppfClient = new JPPFClient("Always the same UUID");
      NioChecksRunner runner = new NioChecksRunner();
      List<JPPFJob> jobList = new ArrayList<>();
      for (int i=1; i<=1; i++)
      {
        JPPFJob job = runner.createJob("job " + i);
        System.out.println("submitting job #" + job.getName());
        runner.executeJob(job);
        jobList.add(job);
      }
      for (JPPFJob job: jobList) runner.displayResults(job);
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
   * Create a JPPF job that can be submitted for execution.
   * @param name the name given to the job.
   * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public JPPFJob createJob(final String name) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    NioChecksTask task = new NioChecksTask(1000L);
    job.add(task);
    return job;
  }

  /**
   * Execute a job in non-blocking mode.
   * @param job the JPPF job to execute.
   * @throws Exception if an error occurs while executing the job.
   */
  public void executeJob(final JPPFJob job) throws Exception
  {
    job.setBlocking(false);
    job.setResultListener(new JPPFResultCollector(job));
    jppfClient.submitJob(job);
  }

  /**
   * Display rthe execution results of a job.
   * @param job the JPPF job to execute.
   * @throws Exception if any error occurs.
   */
  public void displayResults(final JPPFJob job) throws Exception
  {
    JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
    List<Task<?>> results = collector.awaitResults();
    System.out.println("***** results job #" + job.getName() + " *****\n");
    for (Task task: results)
    {
      if (task.getThrowable() != null)
      {
        System.out.println("Caught exception:");
        System.out.println(ExceptionUtils.getStackTrace(task.getThrowable()));
      }
      else
      {
        System.out.println("Result: " + task.getResult());
      }
    }
    System.out.println("");
  }

  /**
   * Retrieve the number of nodes from the server.
   */
  public static void restartNode()
  {
    try
    {
      JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper("localhost", 12001);
      node.connectAndWait(1000);
      node.restart();
      node.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
