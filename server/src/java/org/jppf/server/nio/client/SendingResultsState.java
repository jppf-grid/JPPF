/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import static org.jppf.server.nio.client.ClientTransition.*;

import java.net.ConnectException;

import org.jppf.nio.ChannelWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.debug.DebugHelper;
import org.jppf.server.protocol.ServerTaskBundleClient;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
class SendingResultsState extends ClientServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendingResultsState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public SendingResultsState(final ClientNioServer server) {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public ClientTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    if (channel.isReadable()) throw new ConnectException("client {}" + channel + " has been disconnected");
    ClientContext context = (ClientContext) channel.getContext();
    ServerTaskBundleClient clientBundle = context.getBundle();
    if (clientBundle == null) {
      clientBundle = context.pollCompletedBundle();
      if (clientBundle == null) {
        if (debugEnabled) log.debug("*** clientBundle = null for {}", channel);
        return TO_IDLE;
      }
      context.setBundle(clientBundle);
      context.serializeBundle();
    }
    if (context.writeMessage(channel)) {
      if (debugEnabled) log.debug("*** sent entire bundle {} to {}", clientBundle, context);
      context.setNbTasksToSend(context.getNbTasksToSend() - clientBundle.getTaskCount());
      if (debugEnabled) log.debug("*** NbTasksToSend={}, sent tasks count={}, CompletedBundlesEmpty={}", new Object[] {context.getNbTasksToSend(), clientBundle.getTaskCount(), context.isCompletedBundlesEmpty()});
      context.setBundle(null);
      context.setClientMessage(null);
      if (context.isCompletedBundlesEmpty()) {
        if (context.getNbTasksToSend() <= 0) {
          if (debugEnabled) log.debug("*** client bundle ended " + context.getInitialBundleWrapper());
          context.jobEnded();
          context.setInitialBundleWrapper(null);
          if (JPPFDriver.JPPF_DEBUG) DebugHelper.clearResults(clientBundle.getUuid());
          return TO_WAITING_JOB;
        }
        return TO_IDLE;
      }
    }
    return TO_SENDING_RESULTS;
  }
}
