/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.jppf.JPPFException;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.*;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class WaitingResultsState extends NodeServerState
{
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
  public WaitingResultsState(final NodeNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (context.getMessage() == null) context.setMessage(context.newMessage());
    if (context.readMessage(channel)) {
      Exception exception = null;
      ServerTaskBundleNode nodeBundle = context.getBundle();
      boolean requeue = false;
      try {
        ServerTaskBundleClient newBundleWrapper = context.deserializeBundle();
        JPPFTaskBundle newBundle = newBundleWrapper.getJob();
        Bundler bundler = context.getBundler();
        if (debugEnabled) log.debug("*** read bundle " + newBundle + " from node " + channel);
        // if an exception prevented the node from executing the tasks
        Throwable t = (Throwable) newBundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM);
        if (t != null) {
          if (debugEnabled) log.debug("node " + channel + " returned exception parameter in the header for bundle " + newBundle + " : " + t);
          exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
          nodeBundle.resultsReceived(t);
        } else {
          if (debugEnabled) log.debug("*** bundle has " + newBundleWrapper.getTaskList().size() + " tasks, taskCount=" + newBundle.getTaskCount());
          nodeBundle.resultsReceived(newBundleWrapper.getDataLocationList());
          long elapsed = System.nanoTime() - nodeBundle.getExecutionStartTime();
          server.getStatsManager().taskExecuted(newBundle.getTaskCount(), elapsed / 1000000L, newBundle.getNodeExecutionTime() / 1000000, ((AbstractTaskBundleMessage) context.getMessage()).getLength());
          if (bundler instanceof BundlerEx) {
            Long accumulatedTime = (Long) newBundle.getParameter(BundleParameter.NODE_BUNDLE_ELAPSED_PARAM, -1L);
            ((BundlerEx) bundler).feedback(newBundle.getTaskCount(), elapsed, accumulatedTime, elapsed - newBundle.getNodeExecutionTime());
          } else bundler.feedback(newBundle.getTaskCount(), elapsed);
        }
        requeue = (Boolean) newBundle.getParameter(BundleParameter.JOB_REQUEUE, false);
        JPPFSystemInformation systemInfo = (JPPFSystemInformation) newBundle.getParameter(BundleParameter.SYSTEM_INFO_PARAM);
        if (systemInfo != null) {
          context.setNodeInfo(systemInfo, true);
          if (bundler instanceof NodeAwareness) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
        }
      }
      catch (Throwable t) {
        log.error(t.getMessage(), t);
        exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        nodeBundle.resultsReceived(t);
      } finally {
        nodeBundle.taskCompleted(exception);
        context.setBundle(null);
      }
      if (requeue) nodeBundle.resubmit();
      // there is nothing left to do, so this instance will wait for a task bundle
      // make sure the context is reset so as not to resubmit the last bundle executed by the node.
      context.setMessage(null);
      return context.isPeer() ? TO_IDLE_PEER : TO_IDLE;
    }
    return TO_WAITING_RESULTS;
  }
}
