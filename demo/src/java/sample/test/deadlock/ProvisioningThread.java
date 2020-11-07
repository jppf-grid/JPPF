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

import java.util.Map;

import org.jppf.client.JPPFClient;
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.node.policy.IsMasterNode;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ProvisioningThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProvisioningThread.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /** */
  private static final NodeSelector masterSelector = new ExecutionPolicySelector(new IsMasterNode());
  /** */
  private final long waitTime;
  /** */
  private final NodeForwardingMBean forwarder;
  /** */
  private final JMXDriverConnectionWrapper jmx;
  /** */
  private final int initialNbSlaves;
  /** */
  private final int nbMasterNodes;
  /** */
  private final long jmxTimeout;

  /**
   * 
   * @param client the JPPF client.
   * @param waitTime interval in millis between iterations of this thread.
   * @throws Exception if any error occurs.
   */
  public ProvisioningThread(final JPPFClient client, final long waitTime) throws Exception {
    this.waitTime = waitTime;
    jmx = DeadlockRunner.getJmxConnection(client);
    log.info("getting forwarder");
    forwarder = jmx.getForwarder();
    log.info("got forwarder");
    final ResultsMap<String, Integer> map = forwarder.getNbSlaves(masterSelector);
    nbMasterNodes = map.size();
    int n = -1;
    for (Map.Entry<String, InvocationResult<Integer>> entry: map.entrySet()) {
      if (entry.getValue().isException()) throw entry.getValue().exception();
      n = entry.getValue().result();
      break;
    }
    if (n < 0) throw new IllegalStateException("no slaves were found");
    initialNbSlaves = n;
    jmxTimeout = client.getConfig().get(JPPFProperties.JMX_REMOTE_REQUEST_TIMEOUT);
    DeadlockRunner.print("nb masters: %d, initial nb slaves: %d", nbMasterNodes, initialNbSlaves);
  }

  @Override
  public void run() {
    log.info("starting {}, waitTime={}", getClass().getSimpleName(), waitTime);
    try {
      checkStoppedAndGoToSleep(waitTime);
      while (!isStopped()) {
        if (!provision(0)) break;
        if (!checkStoppedAndGoToSleep(waitTime)) break;
        if (!provision(initialNbSlaves)) break;
        if (!checkStoppedAndGoToSleep(waitTime)) break;
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * 
   * @param nbSlaves the number of slaves to provision.
   * @return {@code false} if this thread was stopped, {@code true} otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean provision(final int nbSlaves)  throws Exception {
    if (debugEnabled) log.debug("provisioning {} slaves", nbSlaves);
    final ResultsMap<String, Void> map = forwarder.provisionSlaveNodes(masterSelector, nbSlaves);
    for (Map.Entry<String, InvocationResult<Void>> entry: map.entrySet()) {
      if (entry.getValue().isException()) {
        final Exception e = entry.getValue().exception();
        log.error("error provisioning {} slaves on node {}", nbSlaves, entry.getKey(), e);
      }
    }
    ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> 
      forwarder.getNbSlaves(masterSelector).values().stream().allMatch(o -> (o.result() != null) && (o.result() == nbSlaves)), jmxTimeout, 500L, true);
    ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmx.nbNodes() == nbMasterNodes * (1 + nbSlaves), jmxTimeout, 500L, true);
    if (debugEnabled) log.debug("got all {} slaves", nbSlaves);
    return !isStopped();
  }

  @Override
  public synchronized void setStopped(final boolean stopped) {
    super.setStopped(stopped);
    wakeUp();
  }

  /**
   * 
   * @param duration the time to wait in millis.
   * @return {@code false} if this thread was stopped, {@code true} otherwise.
   */
  private boolean checkStoppedAndGoToSleep(final long duration) {
    if (isStopped()) return false;
    goToSleep(duration);
    return !isStopped();
  }
}
