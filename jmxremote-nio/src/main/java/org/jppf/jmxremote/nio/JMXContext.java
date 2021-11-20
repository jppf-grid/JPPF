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

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.nio.channels.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.io.IOHelper;
import org.jppf.jmx.*;
import org.jppf.jmxremote.message.*;
import org.jppf.nio.*;
import org.jppf.utils.concurrent.QueueHandler;
import org.slf4j.*;

/**
 * Context associated with a {@link JMXChannelWrapper}.
 * @author Laurent Cohen
 */
public class JMXContext extends AbstractNioContext {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The last read or written JMX message.
   */
  private MessageWrapper currentMessageWrapper;
  /**
   * The queue of pending messages to send.
   */
  private final QueueHandler<MessageWrapper> pendingJmxMessages;
  /**
   * The JMX nio server to use.
   */
  private final JMXNioServer server;
  /**
   * The object that handles messages correlations.
   */
  private JMXMessageHandler messageHandler;
  /**
   * The selection key which associates this context with the {@code SocketChannel}.
   */
  private SelectionKey selectionKey;
  /**
   * Sequence number for each instance of this class.
   */
  private static final AtomicInteger idSequence = new AtomicInteger(0);
  /**
   * The unique id for this context.
   */
  private final int id = idSequence.incrementAndGet();

  /**
   * Initialize with the specified server.
   * @param server the JMX nio server to use.
   * @param socketChannel the associated socket channel.
   * @param reading whether the associated channel performs read operations ({@code true}) or write operations ({@code false}).
   * @param env the jmx environment parameters.
   */
  public JMXContext(final JMXNioServer server, final boolean reading, final SocketChannel socketChannel, final Map<String, ?> env) {
    this.server = server;
    if (reading) {
      pendingJmxMessages = null;
    } else {
      int size = JMXEnvHelper.getInt(JPPFJMXProperties.NOTIF_QUEUE_SIZE, env, null);
      if (size <= 0) size = JPPFJMXProperties.NOTIF_QUEUE_SIZE.getDefaultValue();
      pendingJmxMessages = QueueHandler.<MessageWrapper>builder()
        .withCapacity(size)
        .handlingPeakSizeAs(server::updatePeakPendingMessages)
        .build();
    }
    this.peer = false;
    this.socketChannel = socketChannel;
  }

  @Override
  public void handleException(final Exception exception) {
    try {
      server.closeConnection(getChannels(), exception, false);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * @return the connection id.
   */
  public String getConnectionID() {
    return messageHandler.getChannels().getConnectionID();
  }

  /**
   * Deserialize the specified message.
   * @param message the message to deserialize.
   * @return a deserialzed message.
   * @throws Exception if any error occurs.
   */
  public JMXMessage deserializeMessage(final SimpleNioMessage message) throws Exception {
    return (message != null) ? (JMXMessage) IOHelper.unwrappedData(message.getCurrentDataLocation()) : null;
  }

  /**
   * Add a JMX message to the pending queue.
   * @param jmxMessage the JMX message to offer.
   * @throws Exception if any error occurs.
   */
  public void offerJmxMessage(final JMXMessage jmxMessage) throws Exception {
    final SimpleNioMessage msg = new SimpleNioMessage(this);
    msg.setCurrentDataLocation(IOHelper.serializeData(jmxMessage));
    pendingJmxMessages.put(new MessageWrapper(jmxMessage, msg));
  }

  /**
   * Get the next JMX message from the pending queue.
   * @return a {@link JMXMessage} instance.
   */
  public MessageWrapper pollJmxMessage() {
    final MessageWrapper msg = pendingJmxMessages.poll();
    return msg;
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
   */
  public void setMessageHandler(final JMXMessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", connectionID=").append(messageHandler == null ? "null" : getConnectionID());
    sb.append(", serverSide=").append(messageHandler == null ? "null" : getChannels().isServerSide());
    sb.append(", ssl=").append(ssl);
    if (pendingJmxMessages != null) sb.append(", pendingMessages=").append(pendingJmxMessages.size());
    sb.append(", socketChannel=").append(socketChannel);
    return sb.append(']').toString();
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
  @Override
  public SelectionKey getSelectionKey() {
    if (selectionKey == null) selectionKey = socketChannel.keyFor(server.getSelector());
    return selectionKey;
  }

  @Override
  public boolean readMessage() throws Exception {
    if (readMessage == null) readMessage = new SimpleNioMessage(this);
    readByteCount = readMessage.getChannelReadCount();
    boolean b = false;
    try {
      b = readMessage.read();
    } catch (final Exception e) {
      updateTrafficStats();
      throw e;
    }
    readByteCount = readMessage.getChannelReadCount() - readByteCount;
    if (debugEnabled) log.debug("read {} bytes", readByteCount);
    if (b) updateTrafficStats();
    return b;
  }

  @Override
  public boolean writeMessage() throws Exception {
    readByteCount = readMessage.getChannelWriteCount();
    boolean b = false;
    try {
      b = readMessage.write();
    } catch (final Exception e) {
      updateTrafficStats();
      throw e;
    }
    readByteCount = readMessage.getChannelWriteCount() - readByteCount;
    if (debugEnabled) log.debug("wrote {} bytes", readByteCount);
    if (b) updateTrafficStats();
    return b;
  }

  /**
   * Update the inbound and outbound traffic statistics.
   */
  private void updateTrafficStats() {
    if ((readMessage != null) && (server.getStats() != null)) {
      if (inSnapshot == null) inSnapshot = server.getStats().getSnapshot(JMX_IN_TRAFFIC);
      if (outSnapshot == null) outSnapshot = server.getStats().getSnapshot(JMX_OUT_TRAFFIC);
      double value = readMessage.getChannelReadCount();
      if (value > 0d) inSnapshot.addValues(value, 1L);
      value = readMessage.getChannelWriteCount();
      if (value > 0d) outSnapshot.addValues(value, 1L);
    }
  }

  /**
   * @return the last read or written JMX message.
   */
  MessageWrapper getCurrentMessageWrapper() {
    return currentMessageWrapper;
  }

  /**
   * Set the last read or written JMX message.
   * @param currentMessageWrapper the last read or written JMX message to set.
   */
  void setCurrentMessageWrapper(final MessageWrapper currentMessageWrapper) {
    this.currentMessageWrapper = currentMessageWrapper;
  }

  /**
   * @return The unique id for this context.
   */
  public int getId() {
    return id;
  }
}
