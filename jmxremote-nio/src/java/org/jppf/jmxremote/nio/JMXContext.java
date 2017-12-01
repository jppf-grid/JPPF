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

import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.io.*;
import org.jppf.jmxremote.message.*;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 * Context associated with a {@link JMXChannelWrapper}.
 * @author Laurent Cohen
 */
public class JMXContext extends SimpleNioContext<JMXState> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXContext.class);
  /**
   * The last read or written non-serialized JMX message.
   */
  private JMXMessage currentJmxMessage;
  /**
   * The queue of pending messages to send.
   */
  private final Queue<JMXMessage> pendingJmxMessages;
  /**
   * The JMX nio server to use.
   */
  private final JMXNioServer server;
  /**
   * The object that handles messages correlations.
   */
  private JMXMessageHandler messageHandler;
  /**
   *
   */
  private AtomicReference<JMXState> stateRef = new AtomicReference<>();
  /**
   * 
   */
  private SelectionKey selectionKey;

  /**
   * Initializewitht he specified server.
   * @param server the JMX nio server to use.
   * @param reading whether the associated channel performs read operations ({@code true}) or write operations ({@code false}).
   */
  public JMXContext(final JMXNioServer server, final boolean reading) {
    this.server = server;
    pendingJmxMessages = reading ? null : new LinkedBlockingQueue<JMXMessage>();
    this.peer = false;
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception exception) {
    try {
      server.closeConnection(getConnectionID(), exception);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * @return the connection id.
   */
  public String getConnectionID() {
    return getMessageHandler().getChannels().getConnectionID();
  }

  /**
   * Deserialize the last read message.
   * @return a deserialzed message.
   * @throws Exception if any error occurs.
   */
  public JMXMessage deserializeMessage() throws Exception {
    currentJmxMessage = null;
    if (message != null) {
      BaseNioMessage msg = (BaseNioMessage) message;
      DataLocation dl = msg.getLocations().get(0);
      currentJmxMessage = (JMXMessage) IOHelper.unwrappedData(dl);
    }
    return currentJmxMessage;
  }

  /**
   * Serialize the current message message.
   * @param channel the channel to which the serialized message is written.
   * @throws Exception if any error occurs.
   */
  public void serializeMessage(final ChannelWrapper<?> channel) throws Exception {
    BaseNioMessage msg = (BaseNioMessage) (message = new BaseNioMessage(channel));
    byteCount = 0;
    DataLocation dl = IOHelper.serializeData(currentJmxMessage);
    msg.addLocation(dl);
  }

  /**
   * @return the last read or written non-serialized JMX message.
   */
  public JMXMessage getCurrentJmxMessage() {
    return currentJmxMessage;
  }

  /**
   * Set the next to read or write non-serialized JMX message.
   * @param jmxMessage the next to read or write non-serialized JMX message.
   */
  public void setCurrentJmxMessage(final JMXMessage jmxMessage) {
    this.currentJmxMessage = jmxMessage;
  }

  /**
   * Add a JMX message to the pending queue.
   * @param msg the JMX message to offer.
   * @throws Exception if any error occurs.
   */
  public void offerJmxMessage(final JMXMessage msg) throws Exception {
    pendingJmxMessages.offer(msg);
  }

  /**
   * Get the next JMX message from the pending queue.
   * @return a {@link JMXMessage} instance.
   */
  public JMXMessage pollJmxMessage() {
    return pendingJmxMessages.poll();
  }

  /**
   * @return whether there is at least one pending message.
   */
  public boolean hasQueuedMessage() {
    return !pendingJmxMessages.isEmpty();
  }

  /**
   * @return the object that handles messages correlations.
   */
  public JMXMessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * @return the ChannelsPair this context's channel is a part of.
   */
  public ChannelsPair getChannels() {
    return messageHandler.getChannels();
  }

  /**
   * Set the object that handles messages correlations.
   * @param messageHandler a {@link JMXMessageHandler} instance.
   * @return this context, for method chaining.
   */
  public JMXContext setMessageHandler(final JMXMessageHandler messageHandler) {
    this.messageHandler = messageHandler;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("channel=").append(channel.getClass().getSimpleName()).append("[id=").append(channel.getId()).append(']');
    sb.append(", state=").append(getState());
    sb.append(", connectionID=").append(messageHandler == null ? "null" : getConnectionID());
    sb.append(", serverSide=").append(messageHandler == null ? "null" : getChannels().isServerSide());
    sb.append(", ssl=").append(ssl);
    if (pendingJmxMessages != null) sb.append(", pendingMessages=").append(pendingJmxMessages.size());
    return sb.append(']').toString();
  }

  @Override
  public JMXState getState() {
    return stateRef.get();
  }

  @Override
  public boolean setState(final JMXState state) {
    stateRef.set(state);
    return true;
  }

  /**
   * Compare the state to the expected one, and set it to the update value only if they are the same.
   * @param expected the state to compare to.
   * @param update the state value to update with.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public boolean compareAndSetState(final JMXState expected, final JMXState update) {
    return stateRef.compareAndSet(expected, update);
  }

  /**
   * Set the spcecified state to the channel and prepare it for selection.
   * @param state the transition to set.
   * @param updateOps the value to AND-wise update the interest ops with.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   * @return {@code null}.
   * @throws Exception if any error occurs.
   */
  JMXTransition transitionChannel(final JMXState state, final int updateOps, final boolean add) throws Exception {
    setState(state);
    server.getTransitionManager().updateInterestOps(getSelectionKey(), updateOps, add);
    return null;
  }

  /**
   * @return the JMX nio server to use.
   */
  public JMXNioServer getServer() {
    return server;
  }

  /**
   * @return the channel's selection key.
   */
  public SelectionKey getSelectionKey() {
    if (selectionKey == null) selectionKey = getChannel().getSocketChannel().keyFor(server.getSelector());
    return selectionKey;
  }
}
