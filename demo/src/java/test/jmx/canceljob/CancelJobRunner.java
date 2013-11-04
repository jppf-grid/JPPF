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
package test.jmx.canceljob;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
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
      long duration = 1000L;
      int nbTasks = 10;
      int maxChannels = 1;
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(20L);
      print("submitting a job with " + nbTasks + " tasks");
      String name = "[test job]";
      JPPFJob job = new JPPFJob(name);
      job.setBlocking(false);
      job.getClientSLA().setMaxChannels(maxChannels);
      job.getSLA().setSuspended(true);
      for (int i=1; i<=nbTasks; i++) job.add(new LifeCycleTask(duration)).setId(name + ":task-" + i);
      jppfClient.submitJob(job);
      Thread.sleep(900L);
      print("cancelling job");
      /*
      JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
      JMXDriverConnectionWrapper jmx = c.getJmxConnection();
      DriverJobManagementMBean jobProxy = jmx.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
      */
      //jobProxy.cancelJob(job.getUuid());
      //jppfClient.cancelJob(job.getUuid());
      print("job cancelled, waiting for results");
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      List<Task<?>> results = collector.awaitResults();
      print("********** got results for job '" + job.getName() + "' **********");
      for (Task task: results)
      {
        Throwable e = task.getThrowable();
        if (e != null) print("task '" + task.getId() + "' raised an exception: " + ExceptionUtils.getStackTrace(e));
        else print("result for task '" + task.getId() + "' : " + task.getResult());
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
