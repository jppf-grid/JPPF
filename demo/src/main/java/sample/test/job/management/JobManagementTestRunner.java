/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package sample.test.job.management;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class JobManagementTestRunner {
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;
  /**
   * 
   */
  private static JMXDriverConnectionWrapper driver = null;

  /**
   * Run the first test.
   * @param jobName name given to the job.
   * @throws Exception if any error occurs.
   */
  public void runTest1(final String jobName) throws Exception {
    final TypedProperties props = JPPFConfiguration.getProperties();
    final int nbTasks = props.getInt("job.management.nbTasks", 2);
    final long duration = props.getLong("job.management.duration", 1000L);
    final JPPFJob job = new JPPFJob(jobName);
    job.setName(jobName);
    for (int i = 0; i < nbTasks; i++) job.add(new LongTask(duration)).setId(jobName + " - task " + i);
    client.submitAsync(job);
    // wait to ensure the job has been dispatched to the nodes
    Thread.sleep(1000);
    driver.cancelJob(job.getUuid());
    final List<Task<?>> results = job.awaitResults();
    for (final Task<?> task : results) {
      final Throwable e = task.getThrowable();
      if (e != null) System.out.println("" + task.getId() + " has an exception: " + ExceptionUtils.getStackTrace(e));
      else System.out.println("Result for " + task.getId() + ": " + task.getResult());
    }
    System.out.println("Test ended");
  }

  /**
   * Run the first test.
   * @throws Exception if any error occurs.
   */
  public void runTest2() throws Exception {
    JMXNodeConnectionWrapper[] nodes = null;
    try {
      final Collection<JPPFManagementInfo> coll = driver.nodesInformation();
      nodes = new JMXNodeConnectionWrapper[2];
      int count = 0;
      for (final JPPFManagementInfo info : coll) {
        @SuppressWarnings("resource")
        final JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
        node.connectAndWait(0L);
        nodes[count++] = node;
      }
      for (JMXNodeConnectionWrapper node : nodes) node.updateThreadPoolSize(4);
      Thread.sleep(500L);
      client.submit(createJob("broadcast1"));
      Thread.sleep(500L);
      final NodeSelector selector = new ExecutionPolicySelector(new AtLeast("jppf.processing.threads", 4));
      int n = driver.nbNodes(selector);
      System.out.println("found " + n + " nodes, expected = 2");
      nodes[1].updateThreadPoolSize(2);
      Thread.sleep(500L);
      client.submit(createJob("broadcast2"));
      Thread.sleep(500L);
      n = driver.nbNodes(selector);
      System.out.println("found " + n + " nodes, expected = 1");
    } finally {
      if (nodes != null) for (JMXNodeConnectionWrapper node : nodes) node.close();
    }
  }

  /**
   * Create a broadcast job.
   * @param id the job id.
   * @return a <code>JPPFJob</code> instance.
   * @throws Exception if any error occurs.
   */
  protected JPPFJob createJob(final String id) throws Exception {
    final JPPFJob job = new JPPFJob(id);
    job.setName(id);
    job.add(new MyBroadcastTask());
    job.getSLA().setBroadcastJob(true);
    return job;
  }

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      System.out.println("Initializing client ...");
      client = new JPPFClient("client");
      System.out.println("Awaiting server connection ...");
      while (!client.hasAvailableConnection()) Thread.sleep(100L);
      System.out.println("Awaiting JMX connection ...");
      driver = client.getConnectionPool().getJmxConnection();
      while (!driver.isConnected()) driver.connectAndWait(100L);
      final JobManagementTestRunner runner = new JobManagementTestRunner();
      System.out.println("Running test 1 ...");
      runner.runTest1("job1");
      System.out.println("Running test 2 ...");
      runner.runTest1("job2");
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (driver != null) {
        try {
          driver.close();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
      if (client != null) client.close();
    }
  }

  /**
   * A simple task.
   */
  public static class MyBroadcastTask extends AbstractTask<String> {
    @Override
    public void run() {
      System.out.println("broadcast of " + getClass().getName());
    }
  }
}
