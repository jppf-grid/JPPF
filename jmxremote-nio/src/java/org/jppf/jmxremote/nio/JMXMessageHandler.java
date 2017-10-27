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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.jmxremote.message.*;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXMessageHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXMessageHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Sequence number representing message IDs.
   */
  private final AtomicLong messageSequence = new AtomicLong(0L);
  /**
   * The NIO channels that perform the communication with the server.
   */
  private final ChannelsPair channels;
  /**
   * 
   */
  private final Map<Long, JMXRequest> requestMap = new ConcurrentHashMap<>();
  /**
   * 
   */
  private static StateTransitionManager<JMXState, JMXTransition> transitionManager = JMXNioServer.getInstance().getTransitionManager();

  /**
   * 
   */
  public JMXMessageHandler(final ChannelsPair channels) {
    this.channels = channels;
  }

  /**
   * @return the NIO channels that perform the communication with the server.
   */
  public ChannelsPair getChannels() {
    return channels;
  }

  /**
   * Send a request and return a response when it arrives
   * @param type the type of requets ot send.
   * @param params the request's parameters.
   * @return the result of the requet.
   * @throws Exception if any error occurs.
   */
  public Object sendRequest(final JMXMessageType type, final Object...params) throws Exception {
    JMXRequest request = new JMXRequest(messageSequence.incrementAndGet(), type, params);
    if (debugEnabled) log.debug("sendingRequest {}", request);
    requestMap.put(request.getMessageID(), request);
    ChannelWrapper<?> writingChannel = channels.writingChannel();
    JMXContext context = (JMXContext) writingChannel.getContext();
    context.offerJmxMessage(request);
    synchronized(writingChannel) {
      JMXState state = context.getState();
      if (debugEnabled) log.debug("writing channel state: {}, context = {}", state, context);
      if (state == JMXState.IDLE) {
        transitionManager.transitionChannel(writingChannel, JMXTransition.TO_SENDING_MESSAGE);
        transitionManager.setInterestOps(writingChannel.getSocketChannel(), JMXNioServer.getInstance().getFactory().getTransition(JMXTransition.TO_SENDING_MESSAGE).getInterestOps());
      }
    }
    synchronized(request) {
      request.wait();
    }
    JMXResponse response = request.getResponse();
    if (debugEnabled) log.debug("got response {}", response);
    if (response.getException() != null) throw response.getException();
    return response.getResult();
  }

  /**
   * Called when a response is received.
   * @param response the repsonse to process.
   */
  public void responseReceived(final JMXResponse response) {
    if (debugEnabled) log.debug("received response {}", response);
    JMXRequest request = requestMap.remove(response.getMessageID());
    if (request != null) {
      synchronized(request) {
        request.setResponse(response);
        request.notify();
      }
    } else {
      log.warn("no matching request for {}", response);
    }
  }
}
