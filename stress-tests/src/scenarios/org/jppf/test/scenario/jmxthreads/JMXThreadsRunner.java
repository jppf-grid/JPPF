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

package org.jppf.test.scenario.jmxthreads;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.management.*;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXThreadsRunner extends AbstractScenarioRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JMXThreadsRunner.class);
  /**
   * Executor used to restart the nodes as fast as possible.
   */
  private ExecutorService executor = null;

  @Override
  public void run() {
    try {
      final TypedProperties config = getConfiguration().getProperties();
      final int nbNodes = getConfiguration().getNbNodes();
      executor = Executors.newFixedThreadPool(nbNodes, new JPPFThreadFactory("NodeRestart"));
      final int iterations = config.getInt("iterations", 10);
      output("performing test with " + nbNodes + " nodes, for " + iterations + " iterations");
      final JMXDriverConnectionWrapper jmxDriver = getSetup().getClient().getConnectionPool().getJmxConnection();
      for (int i = 1; i <= iterations; i++) {
        final long start = System.nanoTime();
        getSetup().getJmxHandler().checkDriverAndNodesInitialized(1, nbNodes);
        restartNodes(jmxDriver.nodesInformation());
        final long elapsed = System.nanoTime() - start;
        output("iteration " + i + " performed in " + StringUtils.toStringDuration(elapsed / 1000000L));
      }
      Thread.sleep(3000L);
      final DiagnosticsMBean proxy = jmxDriver.getDiagnosticsProxy();
      final String[] threadNames = proxy.threadNames();
      int count = 0;
      for (final String name: threadNames) {
        if (name.startsWith("JMX connection ")) count++;
      }
      output("*** found " + count + " 'JMX connection ...' threads ***");
      //StreamUtils.waitKeyPressed("press [Enter]");
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (executor != null) executor.shutdownNow();
    }
  }

  /**
   * Restart the specified nodes.
   * @param nodesInfo a collection of information used to connect tothe nodes.
   * @throws Exception if any error occurs.
   */
  private void restartNodes(final Collection<JPPFManagementInfo> nodesInfo) throws Exception {
    final List<Future<RestartNode>> futures = new ArrayList<>(nodesInfo.size());
    for (final JPPFManagementInfo info: nodesInfo) {
      final RestartNode task = new RestartNode(info);
      futures.add(executor.submit(task, task));
    }
    for (final Future<RestartNode> f: futures) {
      final RestartNode task = f.get();
      final Exception e = task.getException();
      if (e != null) output("got exception: " + ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }

  /**
   * 
   */
  private class RestartNode implements Runnable {
    /**
     * The node information
     */
    private final JPPFManagementInfo info;
    /**
     * The eventual exception resulting from this task's execution.
     */
    private Exception exception = null;

    /**
     * Initialize this task.
     * @param info the node information.
     */
    public RestartNode(final JPPFManagementInfo info) {
      this.info = info;
    }

    @Override
    public void run() {
      JMXNodeConnectionWrapper node = null;
      try {
        node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), false);
        node.connect();
        while (!node.isConnected())
          Thread.sleep(10L);
        node.restart();
      } catch (final Exception e) {
        exception = e;
      } finally {
        try {
          if (node != null) node.close();
        } catch (final Exception e) {
          if (exception != null) exception = e;
        }
      }
    }

    /**
     * Get the eventual exception resulting from this task's execution.
     * @return an <code>Exception</code> or <code>null</code>.
     */
    public Exception getException() {
      return exception;
    }
  }
}
