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

package org.jppf.jmxremote.message;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFTimeoutException;
import org.jppf.jmxremote.JMXEnvHelper;
import org.jppf.jmxremote.nio.*;
import org.jppf.nio.StateTransitionManager;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.Logger;

/**
 * Handles requests/responses and notifications with a message-based semantics.
 * @author Laurent Cohen
 */
public class JMXMessageHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggingUtils.getLogger(JMXMessageHandler.class, JMXEnvHelper.isAsyncLoggingEnabled());
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Message ID for receiving a connection ID from the server.
   */
  public final static long CONNECTION_MESSAGE_ID = -1;
  /**
   * Sequence number representing message IDs.
   */
  private static final AtomicLong messageSequence = new AtomicLong(0L);
  /**
   * The NIO channels that perform the communication with the server.
   */
  private final ChannelsPair channels;
  /**
   * Mapping of pending requests to their messageID.
   */
  private final HashMap<Long, JMXRequest> requestMap = new HashMap<>();
  /**
   * Request timeout in millis.
   */
  private final long requestTimeout;
  /**
   * Whether this handler was closed.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * The transition manager.
   */
  private final JMXNioServer server;
  /**
   * The transition manager.
   */
  private final StateTransitionManager<JMXState, JMXTransition> mgr;
  /**
   * 
   */
  private final JMXRequest connectionRequest;

  /**
   * Initialize with the specified pair of reading and writing channels.
   * @param channels the associated pair of reading/writing channels.
   * @param env environment parameters to use for connector properties.
   */
  public JMXMessageHandler(final ChannelsPair channels, final Map<String, ?> env) {
    this.channels = channels;
    channels.setMessageHandler(this);
    this.server = channels.readingChannel().getContext().getServer();
    this.mgr = this.server.getTransitionManager();
    this.requestTimeout = JMXEnvHelper.getLong(JPPFProperties.JMX_REMOTE_REQUEST_TIMEOUT, env, JPPFConfiguration.getProperties());
    connectionRequest = channels.isServerSide() ? null : new JMXRequest(CONNECTION_MESSAGE_ID, JMXMessageType.CONNECT);
    if (connectionRequest != null) putRequest(connectionRequest);
  }

  /**
   * @return the NIO channels that perform the communication with the server.
   */
  public ChannelsPair getChannels() {
    return channels;
  }

  /**
   * Send a request and return a response when it arrives.
   * @param type the type of request to send.
   * @param params the request's parameters.
   * @return the result of the request.
   * @throws Exception if any error occurs.
   */
  public Object sendRequestWithResponse(final byte type, final Object...params) throws Exception {
    return receiveResponse(new JMXRequest(messageSequence.incrementAndGet(), type, params), true);
  }

  /**
   * Get the connection ID upon startup of a client connection.
   * @return the connection ID.
   * @throws Exception if any error occurs.
   */
  public String receiveConnectionID() throws Exception {
    synchronized(connectionRequest) {
      if (connectionRequest.getResponse() != null) {
        final JMXResponse response = connectionRequest.getResponse();
        if (response.getException() != null) throw response.getException();
        return (String) response.getResult();
      }
      return (String) receiveResponse(connectionRequest, false);
    }
  }

  /**
   * Wait for a response message form the server.
   * @param request the request to wat a response for.
   * @param doSendMessage whether to actually send the message.
   * @return the result of the request.
   * @throws Exception if any error occurs.
   */
  private Object receiveResponse(final JMXRequest request, final boolean doSendMessage) throws Exception {
    if (closed.get()) return null;
    if (debugEnabled) log.debug("sending request {}, channels={}", request, channels);
    putRequest(request);
    synchronized(request) {
      if (doSendMessage) sendMessage(request);
      waitForMessage(request);
    }
    final JMXResponse response = request.getResponse();
    if (response != null) {
      if (debugEnabled) log.debug("got response {}", response);
      if (response.getException() != null) throw response.getException();
      return response.getResult();
    }
    return null;
  }

  /**
   * Called when a response is received.
   * @param response the repsonse to process.
   */
  public void responseReceived(final JMXResponse response) {
    final JMXRequest request = removeRequest(response.getMessageID());
    if (request != null) {
      if (debugEnabled) log.debug("received response {}, channels={}", response, channels);
      synchronized(request) {
        request.setResponse(response);
        request.notifyAll();
      }
    } else {
      log.warn("no matching request for {}, channels={}", response, channels);
    }
  }

  /**
   * Sends the specified message.
   * @param type the type of request to send.
   * @param params the request's parameters.
   * @throws Exception if any error occurs.
   */
  public void sendRequestNoResponse(final byte type, final Object...params) throws Exception {
    if (closed.get()) return;
    try {
      final JMXRequest request = new JMXRequest(messageSequence.incrementAndGet(), type, params);
      if (debugEnabled) log.debug("sending request {}, channels={}", request, channels);
      putRequest(request);
      synchronized(request) {
        sendMessage(request);
        waitForMessage(request);
      }
    } catch(final JPPFTimeoutException e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Called when a request that doesn't require a response is sent.
   * @param message the request.
   */
  public void messageSent(final JMXMessage message) {
    if (debugEnabled) log.debug("sent request {}, channels={}", message, channels);
    final JMXRequest request = removeRequest(message.getMessageID());
    if (request == null) log.warn("no matching request for {}", message);
    else if (request != message) log.warn("message and request do not match, request = {}, message = {}", request, message);
    synchronized(message) {
      message.notifyAll();
    }
  }

  /**
   * Sends the specified message.
   * @param message the message to send.
   * @throws Exception if any error occurs.
   */
  public void sendMessage(final JMXMessage message) throws Exception {
    if (closed.get()) return;
    if (debugEnabled) log.debug("sending message {}", message);
    channels.writingChannel().getContext().offerJmxMessage(message);
    final JMXTransitionTask task = channels.getWritingTask();
    if (!task.incrementCountIfNeeded()) mgr.submit(task);
    //server.updateInterestOps(channels.getSelectionKey(), SelectionKey.OP_WRITE, true);
  }

  /**
   * Close this message handler.
   */
  public void close() {
    if (closed.compareAndSet(false, true)) {
      synchronized(requestMap) {
        for (Map.Entry<Long, JMXRequest> entry: requestMap.entrySet()) {
          final JMXRequest request = entry.getValue();
          synchronized(request) {
            request.notifyAll();
          }
        }
        requestMap.clear();
      }
    }
  }

  /**
   * Wait for a call to {@code notify()} on the specified message, for at most the request timeout.
   * The calling method is assumed to own the request's monitor.
   * @param request the message to wait on.
   * @throws JPPFTimeoutException if the timeout was reached before a {@code notify()} was performed.
   * @throws Exception if any other error occurs.
   */
  private void waitForMessage(final JMXRequest request) throws JPPFTimeoutException, Exception {
    final long start = System.nanoTime();
    request.wait(requestTimeout);
    if ((System.nanoTime() - start) / 1_000_000L >= requestTimeout) {
      final String text = "exceeded timeout of " + requestTimeout + " ms waiting for " + request + " on " + channels;
      log.warn(text + ", requests map = {}");
      throw new JPPFTimeoutException(text);
    }
  }

  /**
   * Put the specified request in the requests map.
   * @param request the request to add to the map.
   */
  private void putRequest(final JMXRequest request) {
    synchronized(requestMap) {
      requestMap.put(request.getMessageID(), request);
    }
  }

  /**
   * Put the specified request in the requests map.
   * @param requestID the id of the request.
   * @return the request that was removed, or {@link null}.
   */
  private JMXRequest removeRequest(final Long requestID) {
    synchronized(requestMap) {
      return requestMap.remove(requestID);
    }
  }
}
