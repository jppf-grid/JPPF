/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.node.NodeRunner;
import org.jppf.node.protocol.AbstractTask;
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
   * Entry point for this demo.
   * @param args arguments are not used.
   */
  public static void main(final String[] args) {
    try {
      // create the driver configuration
      final TypedProperties driverConfig = new TypedProperties()
        .set(JPPFProperties.SERVER_PORT, 11111)
        // disable SSL/TLS
        .set(JPPFProperties.SERVER_SSL_PORT, -1);

      // start the JPPF driver
      print("starting the JPPF driver");
      final JPPFDriver driver = new JPPFDriver(driverConfig).start();

      // create the node configuration
      final TypedProperties nodeConfig = new TypedProperties()
        .set(JPPFProperties.SERVER_PORT, 11111)
        .set(JPPFProperties.SERVER_HOST, "localhost")
        .set(JPPFProperties.SSL_ENABLED, false)
        .set(JPPFProperties.MANAGEMENT_PORT, 11111);

      print("starting the JPPF node");
      final NodeRunner nodeRunner = new NodeRunner(nodeConfig);
      // start the node in a separate thread, otherwise the current thread will be stuck
      new Thread(() -> nodeRunner.start(), "JPPF Node").start();

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
        job.add(new HelloTask());
        final String result = (String) client.submit(job).get(0).getResult();
        print("execution result for job '%s': %s", job.getName(), result);
      }

      print("connecting to local driver JMX server");
      // no-arg contructor ==> no network connection: accessing the MBean server directly
      try (final JMXDriverConnectionWrapper driverJmx = new JMXDriverConnectionWrapper()) {
        driverJmx.connect();
        // check the number of connected nodes
        print("nb nodes in server: " + driverJmx.nbNodes());

        print("connecting to local node JMX server");
        try (final JMXNodeConnectionWrapper nodeJmx = new JMXNodeConnectionWrapper()) {
          nodeJmx.connect();
          print("node state: %s", nodeJmx.state());
          // the node can be shutdown via JMX
          //print("shutting down node");
          //nodeJmx.shutdown();
        }
        // the node can also be shutdown via a direct API
        print("shutting down node");
        nodeRunner.shutdown();
        // wait a little bit so the driver has time to detect the node disconnection
        Thread.sleep(1_000L);
        // check that the driver knows the node is disconnected
        print("nb nodes in server: " + driverJmx.nbNodes());
      }

      // shut down the driver
      print("shutting down driver");
      driver.shutdown();
      print("done, exiting program");
    } catch (final Throwable t) {
      t.printStackTrace();
    }
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
   * A simple task to demonstrate that a job can be submitted to an embedded JPPF grid.
   */
  public static class HelloTask extends AbstractTask<String> {
    @Override
    public void run() {
      final String msg = "Hello!";
      System.out.println(">>> " + msg + " from the node <<<");
      setResult(msg);
    }
  }
}
