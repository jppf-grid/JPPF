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

import static org.jppf.jmxremote.nio.JMXState.*;

import java.nio.channels.SelectionKey;

import org.jppf.jmxremote.message.JMXMessage;
import org.jppf.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * Writes all messages in the channel's context queue, if any, or keep writing the current message.
 * @author Laurent Cohen
 */
public class SendingMessageState extends JMXNioState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendingMessageState.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   *
   * @param server the server which handles the channels states and transitions.
   */
  public SendingMessageState(final JMXNioServer server) {
    super(server);
  }

  @Override
  public JMXTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    JMXContext context = (JMXContext) channel.getContext();
    if (context.getCurrentJmxMessage() == null) {
      JMXMessage msg = context.pollJmxMessage();
      if (msg == null) return transitionChannel(channel, IDLE, SelectionKey.OP_WRITE, false);
      if (debugEnabled) log.debug("about to send message {} from context {}", msg, context);
      context.setCurrentJmxMessage(msg);
      context.serializeMessage(channel);
    }
    if (context.writeMessage(channel)) {
      if (debugEnabled) log.debug("fully sent message {} from context {}", context.getCurrentJmxMessage(), context);
      context.setCurrentJmxMessage(null);
      context.setMessage(null);
    }
    return transitionChannel(channel, SENDING_MESSAGE, SelectionKey.OP_WRITE, true);
  }
}
