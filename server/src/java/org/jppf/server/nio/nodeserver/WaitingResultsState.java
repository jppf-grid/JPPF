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
  public NodeTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    if (context.getMessage() == null) context.setMessage(context.newMessage());
    if (context.readMessage(channel))
    {
      Exception exception = null;
      ServerTaskBundle bundleWrapper = context.getBundle();
      boolean requeue = false;
      try {
        JPPFTaskBundle bundle = bundleWrapper;// (JPPFTaskBundle) bundleWrapper.getJob();
        ServerTaskBundle newBundleWrapper = context.deserializeBundle(server.getJobManager());
        JPPFTaskBundle newBundle = newBundleWrapper.getJob();
        if (debugEnabled) log.debug("read bundle" + newBundle + " from node " + channel + " done");
        // if an exception prevented the node from executing the tasks
        Throwable t = (Throwable) newBundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM);
        if (t != null)
        {
          if (debugEnabled) log.debug("node " + channel + " returned exception parameter in the header for bundle " + newBundle + " : " + t);
          exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
          bundleWrapper.resultsReceived(t);
        }
        else
        {
          bundleWrapper.resultsReceived(newBundleWrapper.getTasksL());
          long elapsed = System.nanoTime() - bundleWrapper.getExecutionStartTime();
          server.getStatsManager().taskExecuted(newBundle.getTaskCount(), elapsed / 1000000L, newBundle.getNodeExecutionTime(), ((AbstractTaskBundleMessage) context.getMessage()).getLength());
          context.getBundler().feedback(newBundle.getTaskCount(), elapsed);
        }
        jobManager.jobReturned(bundleWrapper.getClientJob(), channel);
        requeue = (Boolean) newBundle.getParameter(BundleParameter.JOB_REQUEUE, false);
        JPPFSystemInformation systemInfo = (JPPFSystemInformation) newBundle.getParameter(BundleParameter.SYSTEM_INFO_PARAM);
        if (systemInfo != null)
        {
          context.setNodeInfo(systemInfo, true);
          Bundler bundler = context.getBundler();
          if (bundler instanceof NodeAwareness) ((NodeAwareness) bundler).setNodeConfiguration(systemInfo);
        }
      }
      catch (Throwable t)
      {
        log.error(t.getMessage(), t);
        exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        bundleWrapper.resultsReceived(t);
      }
      finally 
      {
        bundleWrapper.taskCompleted(exception);
        context.setBundle(null);
      }
      if (requeue)
      {
//        bundle.setParameter(BundleParameter.JOB_REQUEUE, true);
//        // why should it be suspended ?
//        bundle.getSLA().setSuspended(newBundle.getSLA().isSuspended());
//        context.resubmitBundle(bundleWrapper);
        bundleWrapper.resubmit();
      }
      // there is nothing left to do, so this instance will wait for a task bundle
      // make sure the context is reset so as not to resubmit the last bundle executed by the node.
      context.setMessage(null);
      return TO_IDLE;
    }
    return TO_WAITING;
  }
}
