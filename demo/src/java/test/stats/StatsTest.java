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

package test.stats;

import java.util.Random;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.stats.*;

/**
 * An attempt at reproducing an issue where statistics on the number of tasks
 * in server queue is != 0  when the client is closed and jobs are still executing.
 * @author Laurent Cohen
 */
public class StatsTest {
  /**
   * @param args not used.
   */
  public static void main(final String[] args) {
    int nbJobs = 1000, tasksPerJob = 1000, nbSlaves = 0;
    long duration = 1L;
    Random rand = new Random(System.nanoTime());
    JMXDriverConnectionWrapper jmx = null;
    JPPFClient client  = null;
    boolean reproduced = false;
    int count = 0;
    try {
      while (!reproduced) {
        count++;
        int stopAfter = 50 + rand.nextInt(50);
        System.out.printf("Iteration %d, stop after %d jobs%n", count, stopAfter);
        client  = new JPPFClient();
        if (jmx == null) {
          JMXDriverConnectionWrapper tmp = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
          jmx = new JMXDriverConnectionWrapper(tmp.getHost(), tmp.getPort());
          jmx.connectAndWait(5000L);
        }
        if (nbSlaves >= 0) doProvisioning(client, nbSlaves);
        for (int i=1; i<= nbJobs; i++) {
          JPPFJob job = new JPPFJob();
          job.setName("test " + i);
          job.setBlocking(false);
          for (int j=0; j<tasksPerJob; j++) job.add(new MyTask(duration));
          client.submitJob(job);
          if (i >= stopAfter) {
            Thread.sleep(50 + rand.nextInt(150));
            client.close();
            client = null;
            Thread.sleep(500L);
            JPPFStatistics stats = jmx.statistics();
            JPPFSnapshot snapshot = stats.getSnapshot(JPPFStatisticsHelper.TASK_QUEUE_COUNT);
            System.out.printf("stats after job %d: %s%n", i, snapshot);
            if (snapshot.getLatest() != 0d) reproduced = true;
            break;
          } else {
            job.awaitResults();
          }
        }
      }
      if (client != null) client.close();
      if (jmx != null) jmx.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Provisiong the specified numbe of slave nodes.
   * @param client th client to use for jmx connections.
   * @param nbSlaves the number of slave to provision.
   * @throws Exception if any error occurs.
   */
  private static void doProvisioning(final JPPFClient client, final int nbSlaves) throws Exception {
    System.out.printf("provisioning %d slaves%n", nbSlaves);
    JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    jmx.getNodeForwarder().provisionSlaveNodes(NodeSelector.ALL_NODES, nbSlaves);
    while (jmx.nbNodes() != nbSlaves + 1) Thread.sleep(10L);
  }

  /** */
  public static class MyTask extends AbstractTask<String> {
    /** */
    private final long duration;

    /**
     * @param duration .
     */
    public MyTask(final long duration) {
      this.duration = duration;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(duration);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
