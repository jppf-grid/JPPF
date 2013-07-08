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

package test.driver.restart;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.management.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class TestDriverRestart
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TestDriverRestart.class);
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;
  /**
   * 
   */
  private static final AtomicInteger RESTART_COUNT = new AtomicInteger(0);

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      configureClient();
      client = new JPPFClient();
      int iterations = 1;
      for (int i=1; i<=iterations; i++)
      {
        JPPFJob job = createJPPFJob("test_job_" + i, 1000, 100L, false);
        JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
        client.submit(job);
        List<JPPFTask> results;
        while ((results = collector.waitForResults(5000L)) == null)
        {
          new KillDriverTask().run();
          RESTART_COUNT.incrementAndGet();
        }
        print(job.getName() + " done, driver restarts: " + RESTART_COUNT);
        RESTART_COUNT.set(0);
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (client != null) client.close();
    }
  }

  /**
   * Create a job with the specified parameters.
   * @param name the name given to the job.
   * @param nbTasks the number of tasks in the job.
   * @param duration the duration of each of task in the job.
   * @param blocking whether the job is block.
   * @return the created {@link JPPFJob}.
   * @throws Exception if any error occurs.
   */
  private static JPPFJob createJPPFJob(final String name, final int nbTasks, final long duration, final boolean blocking) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    job.setBlocking(blocking);
    for (int i=0; i<nbTasks; i++) job.addTask(new LongTask(1000L, true)).setId("task_" + (i+1));
    job.setResultListener(new JPPFResultCollector(job) {
      @Override
      public synchronized void resultsReceived(final TaskResultEvent event) {
        super.resultsReceived(event);
        if (event.getThrowable() == null) {
          List<JPPFTask> list = event.getTaskList();
          print("received " + list.size() + " tasks, pending=" + (nbTasks - jobResults.size()) + ", results=" + jobResults.size());
        }
      }
    });
    return job;
  }

  /**
   * Test the connectAndWait() method with the JMXMP connector.
   * @return a {@link JPPFDriverAdminMBean} instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFDriverAdminMBean getDriverJmx() throws Exception
  {
    JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) client.getClientConnection();
    JMXDriverConnectionWrapper jmx = c.getJmxConnection();
    long start = System.nanoTime();
    while (!jmx.isConnected()) Thread.sleep(10L);
    long elapsed = System.nanoTime() - start;
    //System.out.println("actually waited for " + (elapsed/1000000) + " ms");
    return jmx;
  }

  /**
   * 
   */
  private static class KillDriverTask extends TimerTask
  {
    @Override
    public void run()
    {
      try
      {
        JPPFDriverAdminMBean jmx = getDriverJmx();
        jmx.restartShutdown(10L, 10L);
      }
      catch (Exception e)
      {
      }
    }
  }

  /**
   * Configure the JPPF client.
   */
  private static void configureClient()
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.pool.size", "1");
  }

  /**
   * Prints and logs the specified message.
   * @param message the message to print.
   */
  private static void print(final String message)
  {
    log.info(message);
    System.out.println(message);
  }
}
