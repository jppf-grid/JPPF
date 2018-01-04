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

package org.jppf.jmxremote.nio;

import java.io.IOException;

import org.jppf.nio.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
class JMXMessageReader {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JMXMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   *
   * @param context the JMX context that reads the data.
   * @throws Exception if any exception occurs.
   */
  static void read(final JMXContext context) throws Exception {
    final StateTransitionManager<JMXState, JMXTransition> mgr = context.getServer().getTransitionManager();
    while (true) {
      boolean b = false;
      try {
        b = context.readMessage(context.getChannel());
      } catch (final IOException e) {
        final ChannelsPair pair = context.getChannels();
        if (pair.isClosed() || pair.isClosing()) return;
        else throw e;
      }
      if (b) {
        final SimpleNioMessage message = (SimpleNioMessage) context.getMessage();
        if (debugEnabled) log.debug("read message from {}", context);
        context.setMessage(null);
        mgr.submit(new HandlingTask(context, message));
      } else if (context.byteCount <= 0L) break;
    }
  }

  /**
   *
   */
  private final static class HandlingTask implements Runnable {
    /**
     *
     */
    private final JMXContext context;
    /**
     *
     */
    private final SimpleNioMessage message;

    /**
     *
     * @param context the context.
     * @param message the message to handle.
     */
    private HandlingTask(final JMXContext context, final SimpleNioMessage message) {
      this.context = context;
      this.message = message;
    }

    @Override
    public void run() {
      try {
        ReceivingMessageState.handleMessage(context, message);
      } catch(Exception|Error e) {
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
