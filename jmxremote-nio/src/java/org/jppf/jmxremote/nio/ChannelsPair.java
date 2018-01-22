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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.*;

import org.jppf.jmxremote.*;
import org.jppf.jmxremote.message.JMXMessageHandler;
import org.jppf.utils.*;
import org.slf4j.Logger;

/**
 * Convenience class to group a pair of channels respectively reading from and writing to the same socket channel.
 * @author Laurent Cohen
 */
public class ChannelsPair extends Pair<JMXChannelWrapper, JMXChannelWrapper> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggingUtils.getLogger(ChannelsPair.class, JMXEnvHelper.isAsyncLoggingEnabled());
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Whether this a close of the channels was request.
   */
  private final AtomicBoolean closing = new AtomicBoolean(false);
  /**
   * Whether this channel was closed.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * The connection identifier.
   */
  private String connectionID;
  /**
   * Whether this pair of channels represents a server-side connection.
   */
  private boolean serverSide;
  /**
   * The associated MBeanServer (server-side).
   */
  private MBeanServer mbeanServer;
  /**
   * The associated MBeanServerConnection (clientr-side).
   */
  private JPPFMBeanServerConnection mbeanServerConnection;
  /**
   * Server port on which the connection was established.
   */
  private int serverPort = -1;
  /**
   * The tasks that perform the state transitions for the reading and writing channels.
   */
  private final JMXTransitionTask writingTask;
  /**
   * The tasks that perform the state transitions for the reading and writing channels.
   */
  private final JMXTransitionTask nonSelectingWritingTask;
  /**
   * The associated nio server.
   */
  private final JMXNioServer server;
  /**
   * The socket channel's interest ops.
   */
  private int interestOps;
  /**
   * Selection key for the associated socket channel and nio server selector.
   */
  private SelectionKey selectionKey;
  /**
   * The associated message handler.
   */
  private JMXMessageHandler messageHandler;

  /**
   * @param first the reading channel.
   * @param second the writing channel.
   * @param server the JMX nio server.
   */
  public ChannelsPair(final JMXChannelWrapper first, final JMXChannelWrapper second, final JMXNioServer server) {
    super(first, second);
    this.server = server;
    writingTask = new JMXTransitionTask(second, server);
    nonSelectingWritingTask = new JMXTransitionTask(second, server, false);
  }

  /**
   * @return the reading channel.
   */
  public JMXChannelWrapper readingChannel() {
    return first();
  }

  /**
   * @return the reading channel.
   */
  public JMXChannelWrapper writingChannel() {
    return second();
  }

  /**
   * Close the channels and the underlying socket channel.
   * @throws Exception if any error occurs.
   */
  public void close() throws Exception {
    if (closed.compareAndSet(false, true)) this.getSelectionKey().channel().close();
  }

  /**
   * @return whther the channels are closed.
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Request a close of this channel.
   */
  public void requestClose() {
    closing.set(true);
  }

  /**
   * @return whether the channels are closing.
   */
  public boolean isClosing() {
    return closing.get();
  }

  /**
   * @return the connection identifier.
   */
  public String getConnectionID() {
    return connectionID;
  }

  /**
   * Set the connection identifier.
   * @param connectionID the connection id to set.
   * @return this {@code ChannelsPair}, for method call chaining.
   */
  public ChannelsPair setConnectionID(final String connectionID) {
    this.connectionID = connectionID;
    return this;
  }

  /**
   * @return whether this pair of channels represents a server-side connection.
   */
  public boolean isServerSide() {
    return serverSide;
  }

  /**
   * Set whether this pair of channels represents a server-side connection.
   * @param serverSide {@code true} for a server-side connection, {@code false} otherwise.
   */
  public void setServerSide(final boolean serverSide) {
    this.serverSide = serverSide;
  }

  /**
   * @return the server port on which the connection was established.
   */
  public int getServerPort() {
    return serverPort;
  }

  /**
   * Set the server port on which the connection was established.
   * @param serverPort the port number.
   */
  public void setServerPort(final int serverPort) {
    this.serverPort = serverPort;
  }

  /**
   * @return the associated MBeanServer (server-side), if any.
   */
  public MBeanServer getMbeanServer() {
    return mbeanServer;
  }

  /**
   * Set the associated MBeanServer (server-side).
   * @param mbeanServer a {@link MBeanServer} instance.
   */
  public void setMbeanServer(final MBeanServer mbeanServer) {
    this.mbeanServer = mbeanServer;
  }

  /**
   * @return the associated MBeanServerConnection (client-side).
   */
  public JPPFMBeanServerConnection getMbeanServerConnection() {
    return mbeanServerConnection;
  }

  /**
   * Set the associated MBeanServerConnection (client-side).
   * @param mbeanServerConnection the {@link MBeanServerConnection} to set.
   */
  public void setMbeanServerConnection(final JPPFMBeanServerConnection mbeanServerConnection) {
    this.mbeanServerConnection = mbeanServerConnection;
  }

  /**
   * Disable reading fron the channel.
   * @throws Exception if any error occurs.
   */
  public void disableRead() throws Exception {
    if (closed.get()) return;
    if (debugEnabled) log.debug("disabling read on {}", this);
    final JMXChannelWrapper readingChannel = readingChannel();
    readingChannel.context.setState(null);
    server.updateInterestOps(readingChannel.getContext().getSelectionKey(), SelectionKey.OP_READ, false);
  }

  /**
   * Disable writing to the channel.
   * @throws Exception if any error occurs.
   */
  public void disableWrite() throws Exception {
    if (closed.get()) return;
    if (debugEnabled) log.debug("disabling write on {}", this);
    final JMXChannelWrapper writingChannel = writingChannel();
    writingChannel.context.setState(null);
    server.updateInterestOps(writingChannel.getContext().getSelectionKey(), SelectionKey.OP_WRITE, false);
  }

  /**
   * Disable reading on the channel.
   * @throws Exception if any error occurs.
   */
  public void disableReadWrite() throws Exception {
    if (closed.get()) return;
    if (debugEnabled) log.debug("disabling read and write on {}", this);
    writingChannel().context.setState(null);
    readingChannel().context.setState(null);
    server.updateInterestOps(writingChannel().getContext().getSelectionKey(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, false);
  }

  @Override
  public String toString() {
    return  new StringBuilder(getClass().getSimpleName()).append('[')
      .append("readingChannelID=").append(readingChannel().getId())
      .append(", writingChannelID=").append(writingChannel().getId())
      .append(", connectionID=").append(connectionID)
      .append(", closed=").append(closed.get())
      .append(", closing=").append(closing.get())
      .append(", serverSide=").append(serverSide)
      .append(']').toString();
  }

  /**
   * @return the writing task.
   */
  public JMXTransitionTask getWritingTask() {
    return writingTask;
  }

  /**
   * @return the writing task while the selector is not selecting.
   */
  public JMXTransitionTask getNonSelectingWritingTask() {
    return nonSelectingWritingTask;
  }

  /**
   * @return the socket channel's interest ops.
   */
  public int getInterestOps() {
    return interestOps;
  }

  /**
   * Set the socket channel's interest ops.
   * @param interestOps the interest ops to set.
   */
  public void setInterestOps(final int interestOps) {
    this.interestOps = interestOps;
  }

  /**
   * @return the associated selection key.
   */
  public SelectionKey getSelectionKey() {
    return selectionKey;
  }

  /**
   * Set the associated selection key.
   * @param selectionKey the ley to set.
   */
  public void setSelectionKey(final SelectionKey selectionKey) {
    this.selectionKey = selectionKey;
  }

  /**
   * @return the associated message handler.
   */
  public JMXMessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * Set the associated message handler.
   * @param messageHandler the message handler to set.
   */
  public void setMessageHandler(final JMXMessageHandler messageHandler) {
    this.messageHandler = messageHandler;
  }
}
