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

package org.jppf.example.embedded;

import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.node.NodeRunner;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.Task;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sample demonstrates how to start a JPPF driver, node and client, all in the same JVM.
 * @author Laurent Cohen
 */
public class EmbeddedGrid {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(EmbeddedGrid.class);
  /**
   * The embedded drivers.
   */
  private JPPFDriver[] drivers;
  /**
   * The actual embedded nodes.
   */
  private NodeRunner[] nodeRunners;

  /**
   * Entry point for this demo.
   * @param args arguments used as follows:
   * <ul>
   *   <li>args[0] specifies the the number of nodes in the embedded grid. The default value is 2</li>
   *   <li>args[1] specifies the the number of drivers in the embedded grid. The default value is 1</li>
   * </ul>
   */
  public static void main(final String[] args) throws Throwable {
    final EmbeddedGrid grid = new EmbeddedGrid();
    try {
      // read the number of nodes as the first argument, if present
      // read the number of drivers as the second argument, if present
      final int nbDrivers;
      if ((args == null) || (args.length < 1)) {
        nbDrivers = 1;
      } else {
        nbDrivers = Integer.valueOf(args[0]);
      }
      final int nbNodes;
      if ((args == null) || (args.length < 2)) {
        nbNodes = 2;
      } else {
        nbNodes = Integer.valueOf(args[1]);
      }
      grid.start(nbDrivers, nbNodes);

      // create the client configuration

      // disable automatic discovery of the drivers
      final TypedProperties clientConfig = new TypedProperties().set(JPPFProperties.DISCOVERY_ENABLED, false);
      final String[] driverNames = new String[nbDrivers];
      // configure the connection to each driver
      for (int i=0; i<nbDrivers; i++) {
        driverNames[i] = "driver-" + (i + 1);
        clientConfig
          .set(JPPFProperties.PARAM_SERVER_HOST, "localhost", driverNames[i])
          .set(JPPFProperties.PARAM_SERVER_PORT, 11111 + i, driverNames[i]);
      }
      clientConfig.set(JPPFProperties.DRIVERS, driverNames);
  
      print("starting the JPPF client");
      try (final JPPFClient client = new JPPFClient(clientConfig)) {
        // wait for all the drivers and nodes to be started
        waitForGridReady(client, nbDrivers, nbNodes);

        final int nbTasks = 10 * nbNodes;
        print("client connected, now submitting a job with %d tasks", nbTasks);
        final JPPFJob job = new JPPFJob().setName("embedded grid");
        // number of tasks in the job
        for (int i=0; i<nbTasks; i++) {
          job.add(new HelloTask()).setId("task-" + i);
        }
        // submit the job and get the results
        final List<Task<?>> results = client.submit(job);
        int nbSuccess = 0;
        int nbErrors = 0;
        for (final Task<?> task: results) {
          if ((task.getResult() == null) || (task.getThrowable() != null)) {
            nbErrors++;
          } else {
            nbSuccess++;
          }
        }
        print("execution result for job '%s': %d successful tasks, %d in error", job.getName(), nbSuccess, nbErrors);
      }
    } finally {
      grid.shutdown();
    }
    System.exit(0);
  }

  /**
   * Start the embedded grid.
   * @param nbDrivers the number of drivers to start. Each drver is conencted to all other drivers.
   * @param nbNodes the number of nodes to start. The nodes are assigned to the drivers in a round-robin fashion.
   * @throws Exception if any error occurs.
   */
  public void start(final int nbDrivers, final int nbNodes) throws Exception {
    print("starting %,d drivers", nbDrivers);
    drivers = new JPPFDriver[nbDrivers];
    // configure and start each JPPF driver
    for (int i=0; i<nbDrivers; i++) {
      final int n = i + 1;
      print("starting driver %d", n);
      // base configuration
      final TypedProperties driverConfig = new TypedProperties()
        .set(JPPFProperties.SERVER_PORT, 11111 + i)
        .setString("jppf.driver.uuid", "d" + n)
        // disable auto discovery of other drivers
        .set(JPPFProperties.PEER_DISCOVERY_ENABLED, false)
        // configure load-balancing algorithm that sends no more than 5 tasks at a time
        .set(JPPFProperties.LOAD_BALANCING_ALGORITHM, "manual")
        .set(JPPFProperties.LOAD_BALANCING_PROFILE, "jppf")
        .setInt("jppf.load.balancing.profile.jppf.size", 5);
      // configure the connection to the other drivers
      final StringBuilder names = new StringBuilder();
      for (int j=0; j<nbDrivers; j++) {
        // configure the connection to peer driver j
        if (j != i) {
          // give the peer a name
          final String peerName = "peer-d" + (j + 1);
          driverConfig
            .set(JPPFProperties.PARAM_PEER_SERVER_HOST, "localhost", peerName)
            .set(JPPFProperties.PARAM_PEER_SERVER_PORT, 11111 + j, peerName);
          // add to the list of driver names to connect to
          names.append(peerName).append(" ");
        }
      }
      driverConfig.set(JPPFProperties.PEERS, names.toString().trim());
      drivers[i] = new JPPFDriver(driverConfig).start();
    }


    // configure and start the nodes
    print("starting %,d nodes", nbNodes);
    nodeRunners = new NodeRunner[nbNodes];
    for (int i=0; i<nbNodes; i++) {
      print("starting the JPPF node %d", i + 1);
      // we assign a readable name to the node in its configuration
      final TypedProperties nodeConfig = new TypedProperties()
        .set(JPPFProperties.SERVER_HOST, "localhost")
        // assign the node to a driver in round-robin, according to the number of drivers
        .set(JPPFProperties.SERVER_PORT, 11111 + i % nbDrivers)
        .set(JPPFProperties.DISCOVERY_ENABLED, false)
        .setString("node.name", "Node-" + (i + 1));
      final NodeRunner runner = nodeRunners[i] = new NodeRunner(nodeConfig);
      // start the node in a separate thread, otherwise the current thread will be stuck
      new Thread(() -> runner.start(), "NodeRunner-" + (i + 1)).start();
    }
  }

  /**
   * Stop the embedded grid.
   * @throws Exception if any error occurs.
   */
  public void shutdown() throws Exception {
    if (nodeRunners != null) {
      for (int i=0; i<nodeRunners.length; i++) {
        // shutdown each node via a direct API
        if (nodeRunners[i] != null) {
          print("shutting down node %d", i + 1);
          nodeRunners[i].shutdown();
        }
      }
    }

    if (drivers != null) {
      for (int i=0; i<drivers.length; i++) {
        // shut down the driver
        print("shutting down driver %d", i + 1);
        if (drivers[i] != null)
          drivers[i].shutdown();
      }
    }

    // wait a little bit so the drivers and nodes can complete their shutdown
    Thread.sleep(1_000L);
    print("done, exiting program");
  }

  /**
   * Print a formatted message to stdout and to the log.
   * @param format the message format.
   * @param params the message parameters, if any.
   */
  private static void print(final String format, final Object...params) {
    // we prefix all our messages with ">>> " so as to distinguish them from the driver, node and client messages
    final String msg = String.format(">>> " + format, params);
    log.info(msg);
    System.out.println(msg);
  }

  /**
   * This method loops until there are expected number of nodes and drivers in the grid.
   * @param client the JPPF client holding the connections to the drivers.
   * @param nbDrivers the expected number of drivers.
   * @param nbNodes the expected number of nodes.
   * @throws Exception if any error occurs.
   */
  private static void waitForGridReady(final JPPFClient client, final int nbDrivers, final int nbNodes) throws Exception {
    final TopologyManager manager = new TopologyManager(client);
    while (true) {
      final int currentDrivers = manager.getDriverCount();
      if (currentDrivers == nbDrivers) {
        final int currentNodes = manager.getNodeCount();
        if (currentNodes == nbNodes) {
          print("There are now %d drivers and %d nodes active in the embedded grid", nbDrivers, nbNodes);
          break;
        }
      }
      Thread.sleep(250L);
    }
  }

  /**
   * A simple task to demonstrate that a job can be submitted to an embedded JPPF grid.
   */
  public static class HelloTask extends AbstractTask<String> {
    @Override
    public void run() {
      final String msg = "Hello!";
      System.out.printf(">>> %s says %s from %s <<<\n", getId(), msg, getNode().getConfiguration().getString("node.name", "[unkown node name]"));
      setResult(msg);
    }
  }
}
