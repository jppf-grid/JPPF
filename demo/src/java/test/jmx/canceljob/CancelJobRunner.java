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
package test.jmx.canceljob;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class CancelJobRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(CancelJobRunner.class);
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
      configure();
      jppfClient = new JPPFClient();
      long duration = 5000L;
      int nbTasks = 2;
      int maxChannels = 1;
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(20L);
      print("submitting a job with " + nbTasks + " tasks");
      String name = "[test job]";
      JPPFJob job = new JPPFJob(name);
      job.setBlocking(false);
      job.getClientSLA().setMaxChannels(maxChannels);
      //job.getSLA().setSuspended(true);
      for (int i=1; i<=nbTasks; i++) job.addTask(new LifeCycleTask(duration)).setId(name + ":task-" + i);
      jppfClient.submit(job);
      JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
      JMXDriverConnectionWrapper jmx = c.getJmxConnection();
      DriverJobManagementMBean jobProxy = jmx.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
      for (int i=1; i<=1; i++)
      {
        //Thread.sleep(1500L);
        //jobProxy.resumeJob(job.getUuid());
        Thread.sleep(1500L);
        jobProxy.cancelJob(job.getUuid());
      }
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      List<JPPFTask> results = collector.waitForResults();
      print("********** got results for job '" + job.getName() + "' **********");
      for (JPPFTask task: results)
      {
        Exception e = task.getException();
        if (e != null) print("task '" + task.getId() + "' raised an exception: " + ExceptionUtils.getStackTrace(e));
        else print("result for task '" + task.getId() + "' : " + task.getResult());
      }
      Thread.sleep(1000L);
      String[] ids = jobProxy.getAllJobIds();
      print("jobs remaining in server queue: " + Arrays.asList(ids));
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
   * 
   */
  private static void configure()
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
}
