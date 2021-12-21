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

package sample.test.deadlock;

import java.io.*;
import java.util.*;

import org.jppf.client.*;
import org.jppf.client.utils.AbstractJPPFJobStream;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.node.policy.IsMasterNode;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.slf4j.*;

/**
 * An illustration of the patterns for submitting multiple jobs in parallel.
 */
public class DeadlockRunner {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DeadlockRunner.class);
  /** */
  private static JMXDriverConnectionWrapper jmx;

  /**
   * Entry point for this demo.
   * @param args the first argument is a function number that determines which method to call.
   */
  public static void main(final String[] args) {
    try {
      final DeadlockRunner runner = new DeadlockRunner();
      runner.jobStreaming();
      //runner.testNodes();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Execute a stream of non-blocking jobs from a single thread, process the results asynchronously.
   */
  public void jobStreaming() {
    final RunOptions ro = new RunOptions();
    print("Running with conccurencyLimit=%,d; streamDuration=%,d; nbJobs=%,d; tasksPerJob=%,d; taskDuration=%,d",
      ro.concurrencyLimit, ro.streamDuration, ro.nbJobs, ro.tasksPerJob, ro.taskOptions.taskDuration);
    ProvisioningThread pt = null;
    final JMXDriverConnectionWrapper jmx;
    try (final JPPFClient client = new JPPFClient(); JobStreamImpl jobProvider = new JobStreamImpl(ro)) {
      jmx = getJmxConnection(client);
      ro.callback = new ScriptedJobCallback();
      ensureSufficientConnections(client, ro.clientConnections);
      if (ro.slaves >= 0) updateSlaveNodes(client, ro.slaves);
      if (ro.simulateNodeCrashes) pt = new ProvisioningThread(client, ro.waitTime);
      try {
        if (ro.simulateNodeCrashes) new Thread(pt, "ProvisioningThread").start();
        int count = 0;
        final TimeMarker marker = new TimeMarker().start();
        for (final JPPFJob job: jobProvider) {
          if ((job != null) && !client.isClosed()) client.submitAsync(job);
          if (count == ro.triggerNodeDeadlockAfter) {
            final JPPFJob deadlockingJob = new JPPFJob();
            deadlockingJob.setName("deadlock trigger job");
            deadlockingJob.add(new DeadlockingTask());
            client.submitAsync(deadlockingJob);
          }
          count++;
        }
        print("submitted all jobs, awaiting end of job stream");
        if (ro.simulateNodeCrashes) pt.setStopped(true);
        jobProvider.awaitEndOfStream();
        print("reached end of job stream");
        jobProvider.awaitStreamCompletion();
        marker.stop();
        printStats(jmx, jobProvider, marker);
      } finally {
        if (ro.simulateNodeCrashes) pt.setStopped(true);
      }
      printDebugInfo(client);
    } catch (final Exception e) {
      print("error in job stremaing: %s", ExceptionUtils.getStackTrace(e));
    }
  }

  /** */
  public void testNodes() {
    final RunOptions ro = new RunOptions();
    try (final JPPFClient jppfClient = new JPPFClient()) {
      ensureSufficientConnections(jppfClient, 1);
      updateSlaveNodes(jppfClient, ro.slaves);
      final ProvisioningThread pt = new ProvisioningThread(jppfClient, ro.waitTime);
      final TimeMarker marker = new TimeMarker().start();
      ThreadUtils.startThread(pt, "node provisioning");
      Thread.sleep(ro.streamDuration);
      print("total time: %s", marker.stop().getLastElapsedAsString());
      pt.setStopped(true);
      updateSlaveNodes(jppfClient, 0);
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Process the results of a job.
   * @param job the JPPF job whose results are printed.
   */
  public static void processResults(final JPPFJob job) {
    final List<Task<?>> results = job.getAllResults();
    int nbExceptions = 0, nbNoResult = 0;
    for (Task<?> task: results) {
      if (task.getThrowable() != null) nbExceptions++;
      else if (task.getResult() == null) nbNoResult++;
    }
    print("*** results for job '%s' : exceptions = %4d, no result = %4d ***", job.getName(), nbExceptions, nbNoResult);
  }

  /**
   * Ensure that the JPPF client has the specified number of connections.
   * @param client the jppf client.
   * @param nbConnections the desired number of connections.
   * @throws Exception if any error occurs.
   */
  private static void ensureSufficientConnections(final JPPFClient client, final int nbConnections) throws Exception {
    print("***** ensuring %d connections ...", nbConnections);
    final JPPFConnectionPool pool = client.awaitConnectionPool();
    if (pool.getSize() == nbConnections) return;
    print("***** ensuring %d connections, found pool = %s", nbConnections, pool);
    pool.setSize(nbConnections);
    print("***** ensuring %d connections, called setSize(%d)", nbConnections, nbConnections);
    pool.awaitActiveConnections(Operator.AT_LEAST, nbConnections);
    print("***** ensuring %d connections, after pool.await()", nbConnections);
  }

  /**
   * Update the number of running slave nodes.
   * @param client the JPPF client to get a JMX connection from.
   * @param nbSlaves the number of slave nodes to reach.
   * @throws Exception if any error occurs.
   */
  private static void updateSlaveNodes(final JPPFClient client, final int nbSlaves) throws Exception {
    print("ensuring %d slaves ...", nbSlaves);
    final JMXDriverConnectionWrapper jmx = getJmxConnection(client);
    if (jmx.nbNodes() == nbSlaves + 1) return;
    final NodeForwardingMBean forwarder = jmx.getForwarder();
    final NodeSelector masterSelector = new ExecutionPolicySelector(new IsMasterNode());
    // request that <nbSlaves> slave nodes be provisioned
    final TimeMarker marker = new TimeMarker().start();
    forwarder.provisionSlaveNodes(masterSelector, nbSlaves, null);
    while (jmx.nbNodes() != nbSlaves + 1) Thread.sleep(10L);
    print("slaves confirmation wait time: %s", marker.stop().getLastElapsedAsString());
  }

  /**
   * Get a JMX connectionf rom the specified client.
   * @param client the client ot get the connection from.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  static synchronized JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    if (jmx == null) jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    return jmx;
  }

  /**
   * Submit a job with a single task wich triggers a deadlock int he node where it executes.
   * @param client the client which submits the job.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unused")
  private static void submitDeadlockingJob(final JPPFClient client) throws Exception {
    final JPPFJob job = new JPPFJob();
    job.setName("Deadlocking job");
    job.add(new DeadlockingTask());
    client.submit(job);
  }

  /**
   * Print statistics to the console.
   * @param jmx the jmx connection.
   * @param jobProvider the job stream that provided all the jobs.
   * @param marker contains the time it toolk to perform the test.
   * @throws Exception if any error occurs.
   */
  private static void printStats(final JMXDriverConnectionWrapper jmx, final AbstractJPPFJobStream jobProvider, final TimeMarker marker) throws Exception {
    final ResultsMap<String, JPPFNodeState> map = jmx.getForwarder().state(NodeSelector.ALL_NODES);
    double total = 0d;
    double min = Double.MAX_VALUE;
    double max = 0d;
    double mean = 0d;
    final int nbNodes = map.size();
    double meanDev = 0d;
    double minDev = Double.MAX_VALUE;
    double maxDev = 0d;
    final int[] nbTasks = new int[nbNodes];
    int count = 0;
    for (final Map.Entry<String, InvocationResult<JPPFNodeState>> entry: map.entrySet()) {
      final InvocationResult<JPPFNodeState> value = entry.getValue();
      if (value.result() != null) nbTasks[count++] = value.result().getNbTasksExecuted();
      else nbTasks[count++] = 0;
    }
    for (final int nb: nbTasks) {
      if (nb > max) max = nb;
      if (nb < min) min = nb;
      total += nb;
    }
    mean = total / nbNodes;
    for (final int nb: nbTasks) {
      final double dev = Math.abs(nb - mean);
      if (dev > maxDev) maxDev = dev;
      if (dev < minDev) minDev = dev;
      meanDev += dev;
    }
    meanDev /= nbNodes;
    final LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    print("executed a total of %,d jobs and %,d tasks in %s", jobProvider.getJobCount(), jobProvider.getTaskCount(), marker.getLastElapsedAsString());
    print("nodes = %d, tasks = %,.2f; avg = %,.2f; min = %,.2f; max = %,.2f", nbNodes, total, mean, min, max);
    print("deviations: avg = %,.2f; min = %,.2f; max = %,.2f", meanDev, minDev, maxDev);
    final File file = new File("lb.csv");
    final TypedProperties props = lbi.getParameters();
    final boolean computed = props.getBoolean("ga_computed", false);
    props.remove("ga_computed");
    final boolean fileExists = file.exists();
    try (final FileWriter writer = new FileWriter(file, true)) {
      if (!fileExists) writeWithQuotedStrings(writer, "nb nodes", "nb jobs", "time", "avg dev", "algorithm", "params", "ga-computed").write("\n");
      writeWithQuotedStrings(
        writer, nbNodes, jobProvider.getJobCount(), marker.getLastElapsed() / 1_000_000L, meanDev, lbi.getAlgorithm(), new TreeMap<>(props).toString(), Boolean.toString(computed));
      writer.write("\n");
    }
    print("client classpath cache stats: %s", ClasspathCache.getInstance());
  }

  /**
   * Write a csv line where string elements are quoted.
   * @param writer the writer which writes to the file.
   * @param params the elements to write into the file.
   * @return the writer, for method call chaining.
   * @throws Exception if any error occurs.
   */
  private static Writer writeWithQuotedStrings(final Writer writer, final Object...params) throws Exception {
    if ((params != null) && (params.length > 0)) {
      for (int i=0; i<params.length; i++) {
        if (i > 0) writer.write(", ");
        final Object o = params[i];
        if (o == null) writer.write("null");
        else if (o instanceof String) writer.write("\"" + o.toString() + "\"");
        else writer.write(o.toString());
      }
    }
    return writer;
  }

  /**
   * Print and log the specified formatted message.
   * @param format the message format.
   * @param params the parameters of the message.
   */
  static void print(final String format, final Object...params) {
    final String msg = String.format(format, params);
    System.out.println(msg);
    log.info(msg);
  }

  /**
   * 
   * @param client the JPPFClient.
   * @throws Exception if any error occurs.
   */
  static void printDebugInfo(final JPPFClient client) throws Exception {
    printDebugInfo(client.awaitWorkingConnectionPool().awaitWorkingJMXConnection());
  }

  /**
   * 
   * @param jmx .
   * @throws Exception if any error occurs.
   */
  static void printDebugInfo(final JMXDriverConnectionWrapper jmx) throws Exception {
    ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> {
      @SuppressWarnings("unchecked")
      final List<String> entries = (List<String>) jmx.invoke("org.jppf:name=debug,type=driver", "allClientJobEntries");
      if ((entries ==null) || entries.isEmpty()) return false;
      final String s = entries.get(entries.size() - 1);
      print(s);
      return "total: 0 entries".equals(s);
    }, 10_000L, 1000L, false);
    @SuppressWarnings("unchecked")
    final List<String> entries = (List<String>) jmx.invoke("org.jppf:name=debug,type=driver", "allClientJobEntries");
    final String jobEntryStats = (String) jmx.getAttribute("org.jppf:name=debug,type=driver", "ClientJobEntryStats");
    print("Remaining client job entries");
    entries.forEach(DeadlockRunner::print);
    print("client job entries stats: %s", jobEntryStats);
  }
}
