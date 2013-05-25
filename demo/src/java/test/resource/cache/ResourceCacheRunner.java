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
package test.resource.cache;

import java.io.File;
import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * This is a template JPPF application runner.
 * @author Laurent Cohen
 */
public class ResourceCacheRunner
{
  /**
   * The JPPF client, handles all communications with the server.
   */
  private static JPPFClient jppfClient =  null;
  /**
   * 
   */
  private static JMXDriverConnectionWrapper jmxDriver = null;
  /**
   * 
   */
  private static JPPFNodeForwardingMBean proxy = null;

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments.
   */
  public static void main(final String...args)
  {
    try
    {
      System.out.println("submitting jobs");
      JPPFConfiguration.getProperties().setProperty("jppf.pool.size", "1");
      //jppfClient = new JPPFClient("Always the same UUID");
      jppfClient = new JPPFClient();
      createFiles();
      ResourceCacheRunner runner = new ResourceCacheRunner();
      List<JPPFJob> jobList = new ArrayList<JPPFJob>();
      for (int i=1; i<=2; i++)
      {
        requestCacheReset();
        JPPFJob job = runner.createJob("job " + i);
        System.out.println("submitting job #" + job.getName());
        runner.executeJob(job);
        runner.displayResults(job);
        System.out.println("... waiting a while ...");
        Thread.sleep(20000L);
      }
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
    for (int i=1; i<=1000; i++)
    {
      ResourceCacheTask task = new ResourceCacheTask(80L, "res/res_" + StringUtils.padLeft(Integer.toString(i), '0', 4) + ".txt");
      job.addTask(task);
    }
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
    jppfClient.submit(job);
  }

  /**
   * Display rthe execution results of a job.
   * @param job the JPPF job to execute.
   * @throws Exception if any error occurs.
   */
  public void displayResults(final JPPFJob job) throws Exception
  {
    JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
    List<JPPFTask> results = collector.waitForResults();
    System.out.println("***** results job #" + job.getName() + " *****");
    int errors = 0;
    int successes = 0;
    for (JPPFTask task: results)
    {
      if (task.getException() != null)
      {
        System.out.println("Caught exception:");
        System.out.println(ExceptionUtils.getStackTrace(task.getException()));
        errors++;
      }
      else
      {
        //System.out.println("Result: " + task.getResult());
        successes++;
      }
    }
    System.out.println("successful tasks: " + successes + ", errors: " + errors);
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

  /**
   * 
   * @throws Exception .
   */
  private static void createFiles() throws Exception
  {
    File dir = new File("external/res/");
    System.out.println("deleting files ...");
    if (dir.exists()) FileUtils.deletePath(dir);
    System.out.println("creating files ...");
    if (!dir.exists()) dir.mkdirs();
    for (int i=1; i<=1000; i++)
    {
      String nb = StringUtils.padLeft(Integer.toString(i), '0', 4);
      String name = "res_" + nb + ".txt";
      String text = "this is resource number '" + nb + "'";
      File file = new File(dir, name);
      FileUtils.writeTextFile(file, text);
    }
    System.out.println("files created");
  }

  /**
   * 
   * @throws Exception .
   */
  private static void requestCacheReset() throws Exception
  {
    if (jmxDriver == null)
    {
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(1L);
      jmxDriver = ((AbstractJPPFClientConnection) jppfClient.getClientConnection(false)).getJmxConnection();
      while (!jmxDriver.isConnected()) Thread.sleep(1L);
    }
    if (proxy == null) proxy = jmxDriver.getProxy(JPPFNodeForwardingMBean.MBEAN_NAME, JPPFNodeForwardingMBean.class);
    proxy.forwardInvoke(NodeSelector.ALL_NODES, JPPFNodeMaintenanceMBean.MBEAN_NAME, "requestResourceCacheReset");
  }
}
