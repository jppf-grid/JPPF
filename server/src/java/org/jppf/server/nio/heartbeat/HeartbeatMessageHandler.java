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

package org.jppf.server.nio.heartbeat;

import java.util.*;
import java.util.concurrent.ExecutorService;

import org.jppf.comm.recovery.HeartbeatMessage;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
class HeartbeatMessageHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HeartbeatMessageHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of heartbeat requests tot heir message id.
   */
  private final Map<Long, HeartbeatMessage> map = new HashMap<>();
  /**
   * Maximum time to wait for a response.
   */
  private final long timeout;
  /**
   * Maximum number of attempts.
   */
  private final int maxTries;
  /**
   * The server that handles the connections.
   */
  private final HeartbeatNioServer server;
  /**
   * The executor service that handles the channels heartbeats.
   */
  private final ExecutorService executor;
  /**
   * Performs periodic checks of the channels by sending heartbeat messages.
   */
  private final Timer timer;
  /**
   * The channels handled by this object.
   */
  private final List<HeartbeatContext> channels = new LinkedList<>();

  /**
   * @param server the server that handles theconnections.
   */
  HeartbeatMessageHandler(final HeartbeatNioServer server) {
    this.server = server;
    final TypedProperties config = JPPFConfiguration.getProperties();
    timeout = config.get(JPPFProperties.RECOVERY_READ_TIMEOUT);
    maxTries = config.get(JPPFProperties.RECOVERY_MAX_RETRIES);
    if (debugEnabled) log.debug("configuring HeartbeatMessageHandler with timeout = {}, maxTries = {}", timeout, maxTries);
    executor = initExecutor();
    timer = new Timer("HeartbeatTimer", true);
    timer.schedule(new ReaperTask(), timeout, timeout);
  }

  /**
   * Add a channel to the list of handled channels.
   * @param context the channel to add.
   */
  void addChannel(final HeartbeatContext context) {
    if (debugEnabled) log.debug("add new channel {}", context);
    synchronized(channels) {
      channels.add(context);
    }
  }

  /**
   * Rmove a channel from the list of handled channels.
   * @param context the channel to remove.
   */
  void removeChannel(final HeartbeatContext context) {
    if (debugEnabled) log.debug("removing channel {}", context);
    synchronized(channels) {
      channels.remove(context);
    }
  }

  /**
   * Send a heartbeat message to the specified node.
   * @param context the context describing the node connection.
   * @throws Exception if any error occurs.
   */
  private void sendMessage(final HeartbeatContext context) throws Exception {
    try {
      if (context.getState() != HeartbeatState.IDLE) return;
      int nbTries = 0;
      boolean done = false;
      while ((nbTries < maxTries) && !done) {
        nbTries++;
        final HeartbeatMessage data = context.newHeartbeatMessage();
        context.createMessage(data);
        synchronized(map) {
          map.put(data.getMessageID(), data);
        }
        if (debugEnabled) log.debug("about to send {} to {}", data, context);
        server.getTransitionManager().transitionChannel(context.getChannel(), HeartbeatTransition.TO_SEND_MESSAGE);
        synchronized(data) {
          data.wait(timeout);
          done = data.getResponse() != null;
        }
        if (!done) {
          if (debugEnabled) log.debug("node {} hasn't responded to {}/{} successive heartbeat messages at {} ms intervals", context, nbTries, maxTries, timeout);
          synchronized(map) {
            map.remove(data.getMessageID());
          }
        }
      }
      if (!done) {
        log.error("node {} failed to respond to {} successive heartbeat messages at {} ms intervals", context, maxTries, timeout);
        context.heartbeatFailed();
      }
    } finally {
      context.getSubmitted().set(false);
    }
  }

  /**
   * Called when a response is received from the remote peer.
   * @param response the response received from the remote peer.
   */
  void responseReceived(final HeartbeatMessage response) {
    if (debugEnabled) log.debug("received response {}", response);
    final HeartbeatMessage request;
    synchronized(map) {
      request = map.remove(response.getMessageID());
    }
    if (request != null) {
      synchronized(request) {
        request.setResponse(response);
        request.notifyAll();
      }
    } else if (debugEnabled) log.debug("no entry found in the map for received messageID = {}", response.getMessageID());
  }

  /**
   * 
   * @return the executor service that handles the channels heartbeats.
   */
  private ExecutorService initExecutor() {
    final TypedProperties config = JPPFConfiguration.getProperties();
    final int core = config.get(JPPFProperties.RECOVERY_REAPER_POOL_SIZE);
    return ConcurrentUtils.newFixedExecutor(core, timeout, "HeartbeatServer");
  }

  /**
   * 
   */
  class ReaperTask extends TimerTask {
    @Override
    public void run() {
      final HeartbeatContext[] channelsToCheck;
      synchronized(channels) {
        if (channels.isEmpty()) return;
        channelsToCheck = channels.toArray(new HeartbeatContext[channels.size()]);
      }
      for (final HeartbeatContext channel: channelsToCheck) {
        if (channel.getSubmitted().compareAndSet(false, true)) {
          if (debugEnabled) log.debug("submitting heartbeat for {}", channel);
          executor.execute(new Runnable() {
            @Override
            public void run() {
              try {
                sendMessage(channel);
              } catch (final Exception e) {
                log.error(e.getMessage(), e);
              }
            }
          });
        }
      }
    }
  }
}
