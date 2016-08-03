/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.server.nio.client;

import java.util.List;

import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Completion listener that is used to notify that results were received from a node,
 * and they should be sent back to the client.
 * @author Laurent Cohen
 */
public class CompletionListener implements ServerTaskBundleClient.CompletionListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CompletionListener.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The transition manager for the server to client channels.
   */
  private final StateTransitionManager<ClientState, ClientTransition> transitionManager;
  /**
   * The client channel.
   */
  private final ChannelWrapper<?> channel;

  /**
   * Initialize this completion listener with the specified channel.
   * @param channel the client channel.
   * @param transitionManager the channel's transition manager.
   */
  public CompletionListener(final ChannelWrapper<?> channel, final StateTransitionManager<ClientState, ClientTransition> transitionManager) {
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (transitionManager == null) throw new IllegalArgumentException("transitionManager is null");

    this.channel = channel;
    this.transitionManager = transitionManager;
  }

  @Override
  public void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results) {
    if (bundle == null) throw new IllegalStateException("bundlerWrapper is null");
    if (!isChannelValid()) {
      if (debugEnabled) log.debug("channel is invalid: {}", channel);
      ClientContext context = (ClientContext) channel.getContext();
      context.setNbTasksToSend(context.getNbTasksToSend() - results.size());
      removeJobFromQueue(bundle);
      return;
    }
    if (results.isEmpty()) {
      if (debugEnabled) log.debug("empty results list for bundle {}", bundle);
      return;
    }
    if (debugEnabled) log.debug("*** returning " + results.size() + " results for client bundle " + bundle + "(cancelled=" + bundle.isCancelled() + ')');
    if (bundle.isCancelled()) bundle.removeCompletionListener(this);
    else {
      ClientContext context = (ClientContext) channel.getContext();
      context.offerCompletedBundle(bundle);
      synchronized(channel) {
        if (debugEnabled) log.debug("*** context state=" + context.getState() + " for " + bundle + ", channel=" + channel);
        if (context.getState() == ClientState.IDLE) {
          try {
            transitionManager.transitionChannel(channel, ClientTransition.TO_SENDING_RESULTS);
          } catch(Exception e) {
            if (debugEnabled) log.debug("error while transitioning {} : {}", channel, ExceptionUtils.getStackTrace(e));
            else log.info("error while transitioning {} : {}", channel, ExceptionUtils.getMessage(e));
          }
        }
      }
    }
  }

  @Override
  public void bundleEnded(final ServerTaskBundleClient bundle) {
    if (debugEnabled) log.debug("bundle ended: {}", bundle);
  }

  /**
   * Determine whether the channel is valid at the time this method is called.
   * @return <code>true</code> if the channel is valid, <code>false</code> otherwise.
   */
  private boolean isChannelValid() {
    if (channel instanceof SelectionKeyWrapper) return ((SelectionKeyWrapper) channel).getChannel().isValid();
    return true;
  }

  /**
   * Remove the specified job from the queue.
   * @param bundle the job to remove.
   */
  private void removeJobFromQueue(final ServerTaskBundleClient bundle) {
    String uuid = bundle.getUuid();
    JPPFPriorityQueue queue = JPPFDriver.getInstance().getQueue();
    ServerJob job = queue.getBundleForJob(uuid);
    if (job != null) {
      if (debugEnabled) log.debug("job {} : status={}, submissionStatus={}", new Object[] {job.getName(), job.getStatus(), job.getSubmissionStatus()});
      ClientContext context = (ClientContext) channel.getContext();
      if (job.isDone() || (context.getNbTasksToSend() <= 0)) queue.removeBundle(job);
    }
  }
}
