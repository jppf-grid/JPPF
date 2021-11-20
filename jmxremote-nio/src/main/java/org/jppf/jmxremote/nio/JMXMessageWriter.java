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

package org.jppf.jmxremote.nio;

import org.jppf.io.IOHelper;
import org.jppf.jmx.JMXHelper;
import org.jppf.jmxremote.message.JMXRequest;
import org.jppf.nio.SimpleNioMessage;
import org.slf4j.*;

/**
 * Writes all messages in the channel's context queue, if any, or keeps writing the current message until the socket send buffer is full.
 * @author Laurent Cohen
 */
public class JMXMessageWriter {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JMXMessageWriter.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Write to the specified channel.
   * @param context the context to write to.
   * @return {@code true} if there is something left to write, {@code false} otherwise. 
   * @throws Exception if any errort occurs.
   */
  static boolean write(final JMXContext context) throws Exception {
    if (context.isSsl()) {
      synchronized(context.getSocketChannel()) {
        return doWrite(context);
      }
    }
    return doWrite(context);
  }

  /**
   * Write to the specified channel.
   * @param context the context to write to.
   * @return {@code true} if there is something left to write, {@code false} otherwise. 
   * @throws Exception if any errort occurs.
   */
  private static boolean doWrite(final JMXContext context) throws Exception {
    while (true) {
      if (context.getChannels().isClosed()) return false;
      if (context.getCurrentMessageWrapper() == null) {
        final MessageWrapper msg = context.pollJmxMessage();
        if (msg == null) return false;
        final SimpleNioMessage nioMessage =  (SimpleNioMessage) msg.nioMessage;
        if (nioMessage.getCurrentDataLocation() == null) nioMessage.setCurrentDataLocation(IOHelper.serializeData(msg.jmxMessage));
        if (debugEnabled) log.debug("about to send message {} from context {}", msg, context);
        context.setCurrentMessageWrapper(msg);
        context.setReadMessage(msg.nioMessage);
      }
      final MessageWrapper msg = context.getCurrentMessageWrapper();
      try {
        if (context.writeMessage()) {
          if (debugEnabled) log.debug("fully sent message {} from context {}", msg, context);
          context.setReadMessage(null);
          context.setCurrentMessageWrapper(null);
          if (msg.jmxMessage.getMessageType() == JMXHelper.CLOSE) {
            if (debugEnabled) log.debug("handling CLOSE for context {}", context);
            context.getMessageHandler().messageSent(msg.jmxMessage);
            return false;
          }
        } else if (context.readByteCount <= 0L) {
          return true;
        }
      } catch (final Exception e) {
        if (msg.jmxMessage instanceof JMXRequest) {
          context.getMessageHandler().messageSent(msg.jmxMessage);
        }
        throw e;
      }
    }
  }
}
