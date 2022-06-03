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

package org.jppf.example.jobrecovery;

import java.util.Collection;
import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.persistence.DefaultFilePersistenceManager;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.Operator;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.stats.JPPFStatisticsHelper;

/**
 * Demonstration of the job persistence API to implement jobs failover and recovery
 * in the use case of an application crash before it completes.
 * @author Laurent Cohen
 */
public class Runner {
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    JobPersistence<String> persistenceManager = null;
    try {
      client = new JPPFClient();
      // configure the driver so it behaves suitably for this demo
      final int nbNodes = configureDriver();
      System.out.println("updated load-balancing settings, " + nbNodes + " nodes connected to the driver");
      // Create the persistence manager, the root path of the underlying store
      // is in the folder "job_store" under the current directory
      persistenceManager = new DefaultFilePersistenceManager("./job_store");
      final Collection<String> keys = persistenceManager.allKeys();
      // if there is no job in the persistent store,
      // we submit a job normally and simulate an application crash
      if (keys.isEmpty()) {
        final int nbTasks = 10 * nbNodes;
        System.out.println("no job found in persistence store, creating a new job with " + nbTasks + " tasks");
        final JPPFJob job = new JPPFJob();
        // add 10 tasks per node, each task waiting for 1 second
        for (int i = 0; i < nbTasks; i++)
          job.add(new MyTask(1000L, i + 1));
        // set the persistence manager so the job will be persisted
        // each time completed tasks are received from the driver
        job.setPersistenceManager(persistenceManager);
        // the application will exit after 6 seconds (simulated crash)
        final Runnable quit = new Runnable() {
          @Override
          public void run() {
            try {
              Thread.sleep(6000L);
            } catch (@SuppressWarnings("unused") final Exception e) {
            }
            System.exit(1);
          }
        };
        new Thread(quit).start();
        // meanwhile, start the job execution
        executeJob(job);
      }
      // otherwise, if there are jobs in the persistence store,
      // we load them and execute them on the grid
      else {
        System.out.println("found jobs in persistence store: " + keys);
        for (final String key : keys) {
          // load the job from the persistent store, using its key (= job uuid)
          final JPPFJob job = persistenceManager.loadJob(key);
          System.out.println("loaded job '" + key + "' from persistence store " + persistenceManager);
          // don't forget this! the application may crash again
          job.setPersistenceManager(persistenceManager);
          // start the job execution, only non-completed tasks will be executed
          executeJob(job);
          // delete the persisted job after successful completion
          persistenceManager.deleteJob(key);
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
      if (persistenceManager != null) persistenceManager.close();
    }
  }

  /**
   * Execute the specified job.
   * @param job the job to execute.
   * @throws Exception if any error occurs.
   */
  private static void executeJob(final JPPFJob job) throws Exception {
    final List<Task<?>> results = client.submit(job);
    for (final Task<?> task : results) {
      if (task.getThrowable() != null) System.out.println("task " + task.getId() + " exception occurred: " + ExceptionUtils.getStackTrace(task.getThrowable()));
      else System.out.println("task " + task.getId() + " result: " + task.getResult());
    }
  }

  /**
   * This method updates the load balancer setting of the driver,
   * to configure the &quot;manual&quot; algorithm with a size of 1.
   * This means no more than one task will be sent to each node at any given time.
   * @return the number of nodes connected to the driver.
   * @throws Exception if any error occurs while configuring the driver.
   */
  private static int configureDriver() throws Exception {
    // get a connection to the driver's JMX server
    final JMXDriverConnectionWrapper jmxDriver = client.awaitActiveConnectionPool().awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
    // obtain the current load-balancing settings
    final LoadBalancingInformation lbi = jmxDriver.loadBalancerInformation();
    if (lbi == null) return 1;
    final TypedProperties props = lbi.getParameters();
    props.setProperty("size", "1");
    // set load-balancing algorithm to "manual" with a size of 1
    jmxDriver.changeLoadBalancerSettings("manual", props);
    // return the current number of nodes
    return (int) jmxDriver.statistics().getSnapshot(JPPFStatisticsHelper.NODES).getLatest();
  }
}
