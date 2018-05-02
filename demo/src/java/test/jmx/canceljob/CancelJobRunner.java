/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import org.jppf.job.JobSelector;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.Operator;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class CancelJobRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(CancelJobRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient client = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      configure();
      client = new JPPFClient();
      final long duration = 100_000L;
      final int n = 30;
      final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      pool.setSize(n);
      print("waiting for " + n + " client connections ...");
      client.awaitConnectionPools(Operator.EQUAL, n, 100_000L, JPPFClientConnectionStatus.workingStatuses());
      final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
      final JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
      final DriverJobManagementMBean jobManager = jmx.getJobManager();
      forwarder.provisionSlaveNodes(NodeSelector.ALL_NODES, n - 1);
      int idleNodes;
      print("waiting for " + (n-1) + " slave nodes ...");
      while ((idleNodes = jmx.nbIdleNodes()) < n) Thread.sleep(10L);
      final List<JPPFJob> jobs = new ArrayList<>();
      for (int i=0; i<n; i++) {
        final JPPFJob job = new JPPFJob();
        job.setName("Cancel-" + i);
        job.setBlocking(false);
        job.add(new LifeCycleTask(duration)).setId(job.getName() + ":task-0");
        jobs.add(job);
      }
      print("submitting " + n + " jobs and waiting for tasks notifications ...");
      new AwaitTaskNotificationListener(client, LifeCycleTask.MSG, n).submitAndAwait(jobs);
      print("got all tasks notifications");
      print("cancelling jobs");
      jobManager.cancelJobs(JobSelector.ALL_JOBS);
      print("jobs cancel request submitted, waiting for results");
      for (JPPFJob job: jobs) {
        job.awaitResults();
        /*
        print("********** got results for job '" + job.getName() + "' **********");
        for (Task task : results) {
          Throwable e = task.getThrowable();
          if (e != null) print("task '" + task.getId() + "' raised an exception: " + ExceptionUtils.getStackTrace(e));
          else print("result for task '" + task.getId() + "' : " + task.getResult());
        }
        */
      }
      idleNodes = jmx.nbIdleNodes();
      if (idleNodes < n) {
        print(String.format("got all results. There are %d out of %d nodes idle, waiting for all to be idle", idleNodes, n));
        while ((idleNodes = jmx.nbIdleNodes()) < n) Thread.sleep(10L);
      } else print("got all results");
      print("end: nb idle nodes = " + idleNodes);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * 
   */
  private static void configure() {
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg) {
    log.info(msg);
    System.out.println(msg);
  }
}
