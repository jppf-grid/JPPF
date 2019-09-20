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

package test.driver.restart;

import java.util.Map;

import org.jppf.management.*;
import org.jppf.management.JPPFNodeState.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestDriverRestart {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TestDriverRestart.class);
  /** */
  private static final int NB_NODES = 4;

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args) {
    final long  waitTime = 10_000L;
    final int nbIterations = 100;
    for (int i=1; i<=nbIterations; i++) {
      print("***** iteration %,6d *****", i);
      try (JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11111)) {
        if (!jmx.connectAndWait(waitTime)) {
          print("failed to connect to driver");
          System.exit(1);
        }
        ConcurrentUtils.awaitCondition(new NodesCondition(jmx), waitTime, 250L, true);
        jmx.restartShutdown(1L, 1L);
        //Thread.sleep(400L);
      } catch(final Exception  e) {
        e.printStackTrace(System.out);
        System.exit(1);
      }
    }
    System.out.println("done");
  }

  /**
   * @param format .
   * @param params 0
   */
  private static void print(final String format, final Object...params) {
    print(true, true, format, params);
  }

  /**
   * @param toStdout .
   * @param toLog .
   * @param format .
   * @param params .
   */
  private static void print(final boolean toStdout, final boolean toLog, final String format, final Object...params) {
    final String message = (params != null) && (params.length > 0) ? String.format(format, params) : format;
    if (toLog) log.info(message);
    if (toStdout) System.out.println(message);
  }


  /** */
  private static final class NodesCondition implements ConcurrentUtils.ConditionFalseOnException {
    /** */
    private final JMXDriverConnectionWrapper jmx;

    /**
     * @param jmx .
     */
    private NodesCondition(final JMXDriverConnectionWrapper jmx) {
      this.jmx = jmx;
    }

    @Override
    public boolean evaluateWithException() throws Exception {
      if (jmx.nbNodes() != NB_NODES) return false; 
      if (jmx.nbIdleNodes() != NB_NODES) return false; 
      final JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
      final Map<String, Object> results = forwarder.forwardInvoke(NodeSelector.ALL_NODES, JPPFNodeAdminMBean.MBEAN_NAME, "state");
      //print(false, true, "results = %s", results);
      if (results.size() != NB_NODES) return false;
      for (final Map.Entry<String, Object> entry: results.entrySet()) {
        if (!(entry.getValue() instanceof JPPFNodeState)) return false;
        final JPPFNodeState state = (JPPFNodeState) entry.getValue();
        if (state.getConnectionStatus() != ConnectionState.CONNECTED) return false;
        if (state.getExecutionStatus() != ExecutionState.IDLE) return false;
      }
      return true;
    }
  }
}
