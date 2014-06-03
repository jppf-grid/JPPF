/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.server.protocol.BundleParameter.*;

import java.util.*;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.nio.ChannelWrapper;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class WaitingResultsState extends NodeServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingResultsState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public WaitingResultsState(final NodeNioServer server) {
    super(server);
  }

  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    //if (debugEnabled) log.debug("exec() for " + channel);
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (context.readMessage(channel)) {
      BundleResults received = context.deserializeBundle();
      return process(received, context);
    }
    return TO_WAITING_RESULTS;
  }

  /**
   * Process the bundle that was just read.
   * @param received holds the received bundle along with the tasks.
   * @param context the channel from which the bundle was read.
   * @return the enxt transition to perform.
   * @throws Exception if any error occurs.
   */
  public NodeTransition process(final BundleResults received, final AbstractNodeContext context) throws Exception {
    ServerTaskBundleNode nodeBundle = context.getBundle();
    server.getDispatchExpirationHandler().cancelAction(ServerTaskBundleNode.makeKey(nodeBundle));
    boolean requeue = false;
    try {
      TaskBundle newBundle = received.bundle();
      if (debugEnabled) log.debug("*** read bundle " + newBundle + " from node " + context.getChannel());
      requeue = processResults(context, received);
    }
    catch (Throwable t) {
      log.error(t.getMessage(), t);
      nodeBundle.resultsReceived(t);
    } finally {
      context.setBundle(null);
    }
    if (requeue) nodeBundle.resubmit();
    // there is nothing left to do, so this instance will wait for a task bundle
    // make sure the context is reset so as not to resubmit the last bundle executed by the node.
    context.setMessage(null);
    return context.isPeer() ? TO_IDLE_PEER : TO_IDLE;
  }

  /**
   * Process the results received from the node.
   * @param context the context associated witht he node channel.
   * @param received groups the job header and resuls of the tasks.
   * @return A boolean requeue indicator.
   * @throws Exception if any error occurs.
   */
  private boolean processResults(final AbstractNodeContext context, final BundleResults received) throws Exception {
    TaskBundle newBundle = received.bundle();
    ServerTaskBundleNode nodeBundle = context.getBundle();
    // if an exception prevented the node from executing the tasks or sending back the results
    Throwable t = newBundle.getParameter(NODE_EXCEPTION_PARAM);
    Bundler bundler = context.getBundler();
    if (t != null) {
      if (debugEnabled) log.debug("node " + context.getChannel() + " returned exception parameter in the header for bundle " + newBundle + " : " + t);
      nodeBundle.resultsReceived(t);
    } else {
      if (debugEnabled) log.debug("*** received bundle with " + received.second().size() + " tasks, taskCount=" + newBundle.getTaskCount() + " : " + received.bundle());
      Set<Integer> resubmitSet = null;
      int[] resubmitPositions = newBundle.getParameter(BundleParameter.RESUBMIT_TASK_POSITIONS, null);
      if (debugEnabled) log.debug("*** resubmitPositions = {} for {}", resubmitPositions, newBundle);
      if (resubmitPositions != null) {
        resubmitSet = new HashSet<>();
        for (int n: resubmitPositions) resubmitSet.add(n);
        if (debugEnabled) log.debug("*** resubmitSet = {} for {}", resubmitSet, newBundle);
      }
      boolean anyResubmit = resubmitSet != null;
      int count = 0;
      for (ServerTask task: nodeBundle.getTaskList()) {
        if (anyResubmit && resubmitSet.contains(task.getJobPosition())) {
          int max = nodeBundle.getJob().getSLA().getMaxTaskResubmits();
          if (task.incResubmitCount() <= max) {
            task.resubmit();
            count++;
          }
        }
      }
      if (count > 0) context.updateStatsUponTaskResubmit(count);
      
      nodeBundle.resultsReceived(received.data());
      long elapsed = System.nanoTime() - nodeBundle.getJob().getExecutionStartTime();
      updateStats(newBundle.getTaskCount(), elapsed / 1_000_000L, newBundle.getNodeExecutionTime() / 1_000_000L);
      if (bundler instanceof BundlerEx) {
        long accumulatedTime = newBundle.getParameter(NODE_BUNDLE_ELAPSED_PARAM, -1L);
        ((BundlerEx) bundler).feedback(newBundle.getTaskCount(), elapsed, accumulatedTime, elapsed - newBundle.getNodeExecutionTime());
      } else bundler.feedback(newBundle.getTaskCount(), elapsed);
    }
    boolean requeue = newBundle.isRequeue();
    JPPFSystemInformation systemInfo = newBundle.getParameter(SYSTEM_INFO_PARAM);
    if (systemInfo != null) {
      context.setNodeInfo(systemInfo, true);
      if (bundler instanceof NodeAwareness) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
    }
    return requeue;
  }

  /**
   * Update the statistcis from the received results.
   * @param nbTasks number of tasks received.
   * @param elapsed server/node round trip time.
   * @param elapsedInNode time spent in the node.
   */
  private void updateStats(final int nbTasks, final long elapsed, final long elapsedInNode) {
    JPPFStatistics stats = JPPFDriver.getInstance().getStatistics();
    stats.addValue(JPPFStatisticsHelper.TASK_DISPATCH, nbTasks);
    stats.addValues(JPPFStatisticsHelper.EXECUTION, elapsed, nbTasks);
    stats.addValues(JPPFStatisticsHelper.NODE_EXECUTION, elapsedInNode, nbTasks);
    stats.addValues(JPPFStatisticsHelper.TRANSPORT_TIME, elapsed - elapsedInNode, nbTasks);
  }
}
