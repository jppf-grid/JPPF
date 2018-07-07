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

import org.jppf.comm.recovery.HeartbeatMessage;
import org.jppf.nio.StateTransitionManager;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off
 * to a global thread pool for deserialization and processing.
 * @author Laurent Cohen
 */
class HeartbeatMessageReader {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HeartbeatMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  static void read(final HeartbeatContext context) throws Exception {
    if (context.isSsl()) {
      synchronized(context.getSocketChannel()) {
        doRead(context);
      }
    } else doRead(context);
  }

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  private static void doRead(final HeartbeatContext context) throws Exception {
    final StateTransitionManager<EmptyEnum, EmptyEnum> mgr = context.server.getTransitionManager();
    while (true) {
      final boolean b = context.readMessage(context.getChannel());
      if (b) {
        final HeartbeatMessage message = context.getHeartbeatMessage();
        if (debugEnabled) log.debug("read message {} from {}", message, context);
        if (context.getUuid() == null) {
          final String uuid = message.getUuid();
          context.setUuid(uuid);
        }
        context.setHeartbeatMessage(null);
        context.setMessage(null);
        mgr.execute(new HandlingTask(context, message));
      } else if (context.byteCount <= 0L) break;
    }
  }

  /**
   * Deserialize the specified message and route it to the specialized handling method.
   * @param context the context associated with the channel.
   * @param message the message to handle.
   * @throws Exception if any error occurs.
   */
  private static void handleMessage(final HeartbeatContext context, final HeartbeatMessage message) throws Exception {
    if (debugEnabled) log.debug("read message = {} from context = {}", message, context);
    context.server.getMessageHandler().responseReceived(message);
  }

  /**
   * Instances of this task deserialize and porcess a NioMessage that was read from the network channel.
   */
  private final static class HandlingTask implements Runnable {
    /**
     * The context associated with the channel.
     */
    private final HeartbeatContext context;
    /**
     * The message to handle.
     */
    private final HeartbeatMessage message;

    /**
     * Initialize with the specified context and message.
     * @param context the context associated with the channel.
     * @param message the message to handle.
     */
    private HandlingTask(final HeartbeatContext context, final HeartbeatMessage message) {
      this.context = context;
      this.message = message;
    }

    @Override
    public void run() {
      try {
        HeartbeatMessageReader.handleMessage(context, message);
      } catch(final Exception|Error e) {
        try {
          if (debugEnabled) log.debug("error on channel {} :\n{}", context, ExceptionUtils.getStackTrace(e));
          else log.warn("error on channel {} : {}", context, ExceptionUtils.getMessage(e));
        } catch (final Exception e2) {
          if (debugEnabled) log.debug("error on channel: {}", ExceptionUtils.getStackTrace(e2));
          else log.warn("error on channel: {}", ExceptionUtils.getMessage(e2));
        }
        if (e instanceof Exception) context.handleException(context.getChannel(), (Exception) e);
        else throw (Error) e;
      }
    }
  }
}
