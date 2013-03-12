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

package org.jppf.test.scenario.jmxthreads;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXThreadsRunner extends AbstractScenarioRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JMXThreadsRunner.class);
  /**
   * The count of itearations runs.
   */
  private int iterationsCount = 0;
  /**
   * Executor used to restart the nodes as fast as possible.
   */
  private ExecutorService executor = null;

  @Override
  public void run()
  {
    try
    {
      TypedProperties config = getConfiguration().getProperties();
      int nbNodes = getConfiguration().getNbNodes();
      executor = Executors.newFixedThreadPool(nbNodes, new JPPFThreadFactory("DriverRestart"));
      int iterations = config.getInt("iterations", 10);
      output("performing test with " + nbNodes + " nodes, for " + iterations + " iterations");
      JMXDriverConnectionWrapper jmxDriver = ((JPPFClientConnectionImpl) getSetup().getClient().getClientConnection()).getJmxConnection();
      for (int i=1; i<=iterations; i++)
      {
        long start = System.nanoTime();
        getSetup().getJmxHandler().checkDriverAndNodesInitialized(1, nbNodes);
        //restartNodes(jmxDriver.nodesInformation());
        restartDriver();
        long elapsed = System.nanoTime() - start;
        output("iteration " + i + " performed in " + StringUtils.toStringDuration(elapsed/1000000L));
      }
      Thread.sleep(3000L);
      /*
      DiagnosticsMBean proxy = jmxDriver.getProxy(DiagnosticsMBean.MBEAN_NAME_DRIVER, DiagnosticsMBean.class);
      String[] threadNames = proxy.threadNames();
      */
      //StreamUtils.waitKeyPressed("press [Enter]");
    }
    catch (Exception e)
    {
      //e.printStackTrace();
    }
    finally
    {
      try
      {
        String[] threadNames = threadNames();
        int count = 0;
        for (String name: threadNames)
        {
          if (name.startsWith("JMX connection ")) count++;
        }
        output("*** found " + count + " 'JMX connection ...' threads ***");
        StreamUtils.waitKeyPressed("press [Enter]");
      }
      catch (Exception e2)
      {
        e2.printStackTrace();
      }
      if (executor != null) executor.shutdownNow();
    }
  }

  /**
   * Restart the specified nodes.
   * @param nodesInfo a collection of information used to connect tothe nodes.
   * @throws Exception if any error occurs.
   */
  private void restartNodes(final Collection<JPPFManagementInfo> nodesInfo) throws Exception
  {
    List<Future<RestartNode>> futures = new ArrayList<Future<RestartNode>>(nodesInfo.size());
    for (JPPFManagementInfo info: nodesInfo)
    {
      RestartNode task = new RestartNode(info);
      futures.add(executor.submit(task, task));
    }
    for (Future<RestartNode> f: futures)
    {
      RestartNode task = f.get();
      Exception e = task.getException();
      if (e != null) output("got exception: " + ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Restart the driver.
   * @throws Exception if any error occurs.
   */
  private void restartDriver() throws Exception
  {
    JMXDriverConnectionWrapper jmx = getSetup().getDriverManagementProxy();
    jmx.restartShutdown(100L, 100L);
    Thread.sleep(500L);
    int n = getConfiguration().getProperties().getInt("jppf.pool.size");
    int count = 0;
    while (count < n)
    {
      count = 0;
      List<JPPFClientConnection> list = getSetup().getClient().getAllConnections();
      for (JPPFClientConnection conn: list)
      {
        if (((AbstractJPPFClientConnection) conn).getStatus() == JPPFClientConnectionStatus.ACTIVE) count++;
      }
      if (count < n) Thread.sleep(100L);
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

  /**
   * Get the names of all threads in this JVM.
   * @return an array of thread names.
   */
  public String[] threadNames()
  {
    ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
    long[] ids = threadsBean.getAllThreadIds();
    ThreadInfo[] infos = threadsBean.getThreadInfo(ids, 0);
    String[] result = new String[infos.length];
    for (int i=0; i<infos.length; i++) result[i] = infos[i].getThreadName();
    return result;
  }

  /**
   * 
   */
  private class RestartNode implements Runnable
  {
    /**
     * The node information
     */
    private final JPPFManagementInfo info;
    /**
     * The eventual exception resulting from this task's execution.
     */
    private Exception exception = null;

    /**
     * Initialize this task.
     * @param info the node information.
     */
    public RestartNode(final JPPFManagementInfo info)
    {
      this.info = info;
    }

    @Override
    public void run()
    {
      JMXNodeConnectionWrapper node = null;
      try
      {
        node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), false);
        node.connect();
        while (!node.isConnected()) Thread.sleep(10L);
        node.restart();
      }
      catch (Exception e)
      {
        exception = e;
      }
      finally
      {
        try
        {
          if (node != null) node.close();
        }
        catch (Exception e)
        {
          if (exception != null) exception = e;
        }
      }
    }

    /**
     * Get the eventual exception resulting from this task's execution.
     * @return  an <code>Exception</code> or <code>null</code>.
     */
    public Exception getException()
    {
      return exception;
    }
  }
}
