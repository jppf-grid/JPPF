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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFNodeState;
import org.jppf.management.NodeSelector;
import org.jppf.node.NodeRunner;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.ResultsMap;
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
   * The embedded JPPF driver.
   */
  private JPPFDriver driver;
  /**
   * Number of nodes in the embedded grid.
   */
  private int nbNodes;
  /**
   * The actual embedded nodes.
   */
  private NodeRunner[] nodeRunners;

  /**
   * Entry point for this demo.
   * @param args arguments are not used.
   */
  public static void main(final String[] args) throws Throwable {
    final EmbeddedGrid grid = new EmbeddedGrid();
    try {
      final int nbNodes;
      if ((args == null) || (args.length < 1)) {
        nbNodes = 2;
      } else {
        nbNodes = Integer.valueOf(args[0]);
      }
      grid.start(nbNodes);

      // create the client configuration
      final String driverName = "driver1";
      final TypedProperties clientConfig = new TypedProperties()
        .set(JPPFProperties.DISCOVERY_ENABLED, false)
        .set(JPPFProperties.DRIVERS, new String[] { driverName })
        .set(JPPFProperties.PARAM_SERVER_HOST, "localhost", driverName)
        .set(JPPFProperties.PARAM_SERVER_PORT, 11111, driverName);
  
      print("starting the JPPF client");
      try (final JPPFClient client = new JPPFClient(clientConfig)) {
        // wait for the connection to the driver to be established
        client.awaitWorkingConnectionPool();
        print("client connected, now submitting a job");
        final JPPFJob job = new JPPFJob().setName("embedded grid");
        // number of tasks in the job
        final int nbTasks = 10;
        for (int i=0; i<nbTasks; i++) {
          job.add(new HelloTask()).setId("task-" + i);
        }
        // submit the job and get the results
        final String result = (String) client.submit(job).get(0).getResult();
        print("execution result for job '%s': %s", job.getName(), result);
      }
  
      Thread.sleep(500L);
      print("connecting to local driver JMX server");
      // no-arg contructor ==> no network connection: accessing the MBean server directly
      try (final JMXDriverConnectionWrapper driverJmx = new JMXDriverConnectionWrapper()) {
        driverJmx.connect();
        // check the number of connected nodes
        print("number of nodes in the grid: %d", driverJmx.nbNodes());
  
        // get a snapshot of the state of each node
        final ResultsMap<String, JPPFNodeState> nodeStates = driverJmx.getForwarder().state(NodeSelector.ALL_NODES);
        nodeStates.forEach((uuid, result) -> 
          print("state of node %s : %s", uuid, result.isException() ? getStackTrace(result.exception()) : result.result()));
      }
    } finally {
      grid.shutdown();
    }
    System.exit(0);
  }

  /**
   * Start the embedded grid.
   * @param nbNodes the number of nodes to start.
   * @throws Exception if any error occurs.
   */
  public void start(final int nbNodes) throws Exception {
    // create the driver configuration
    final TypedProperties driverConfig = new TypedProperties().set(JPPFProperties.SERVER_PORT, 11111);
    // configure and start the JPPF driver
    print("starting the JPPF driver");
    driver = new JPPFDriver(driverConfig).start();

    // create the node configuration
    final TypedProperties nodeConfigTemplate = new TypedProperties().set(JPPFProperties.SERVER_HOST, "localhost").set(JPPFProperties.SERVER_PORT, 11111);
    // configure and start the nodes
    this.nbNodes = nbNodes;
    nodeRunners = new NodeRunner[nbNodes];
    for (int i=0; i<nbNodes; i++) {
      print("starting the JPPF node %d", i + 1);
      // we assign a readable name to the node in its configuration
      final TypedProperties nodeConfig = new TypedProperties(nodeConfigTemplate).setString("node.name", "Node-" + (i + 1));
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
      for (int i=0; i<nbNodes; i++) {
        // shutdown each node via a direct API
        if (nodeRunners[i] != null) {
          print("shutting down node %d", i + 1);
          nodeRunners[i].shutdown();
        }
      }
    }

    if (driver != null) {
      // shut down the driver
      print("shutting down driver");
      driver.shutdown();
    }

    // wait a little bit so the driver and nodes can complete their shutdown
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
   * Get a stack trace as a string.
   * @param throwable the throwable to get the stack trace from.
   * @return a String representing the stack trace.
   * @throws IOException if any I/O error occurs while retrieving the stack trace.
   */
  private static String getStackTrace(final Throwable throwable) {
    try (final StringWriter sw = new StringWriter(); final PrintWriter pw = new PrintWriter(sw)) {
      throwable.printStackTrace(pw);
      return sw.toString();
    } catch(final IOException e) {
      return e.toString();
    }
  }

  /**
   * A simple task to demonstrate that a job can be submitted to an embedded JPPF grid.
   */
  public static class HelloTask extends AbstractTask<String> {
    @Override
    public void run() {
      final String msg = "Hello!";
      System.out.printf(">>> %s from %s <<<\n", msg, getNode().getConfiguration().getString("node.name", "[unkown node name]"));
      setResult(msg);
    }
  }
}
