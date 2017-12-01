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
import org.jppf.utils.*;
import org.slf4j.Logger;

/**
 * Convenience class to group a pair of channels respectively reading from and writing to the same socket channel.
 * @author Laurent Cohen
 */
public class ChannelsPair extends Pair<JMXChannelWrapper, JMXChannelWrapper> {
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
  private final JMXTransitionTask readingTask, writingTask;
  /**
   * 
   */
  private final JMXNioServer server;

  /**
   * @param first the reading channel.
   * @param second the writing channel.
   * @param server the JMX nio server.
   */
  public ChannelsPair(final JMXChannelWrapper first, final JMXChannelWrapper second, final JMXNioServer server) {
    super(first, second);
    this.server = server;
    readingTask = new JMXTransitionTask(first, JMXState.RECEIVING_MESSAGE, server.getFactory());
    writingTask = new JMXTransitionTask(second, JMXState.SENDING_MESSAGE, server.getFactory());
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
    if (closed.compareAndSet(false, true)) {
      try {
        first().close();
      } finally {
        second().close();
      }
    }
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
   * @return this {@code ChannelsPair}, for method call chaining.
   */
  public ChannelsPair setServerSide(final boolean serverSide) {
    this.serverSide = serverSide;
    return this;
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
   * @return this channels pair, for method call chaining.
   */
  public ChannelsPair setServerPort(final int serverPort) {
    this.serverPort = serverPort;
    return this;
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
   * @return this channels pair, for method call chaining.
   */
  public ChannelsPair setMbeanServer(final MBeanServer mbeanServer) {
    this.mbeanServer = mbeanServer;
    return this;
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
   * @return this channels pair, for method call chaining.
   */
  public ChannelsPair setMbeanServerConnection(final JPPFMBeanServerConnection mbeanServerConnection) {
    this.mbeanServerConnection = mbeanServerConnection;
    return this;
  }

  /**
   * Disable reading fron the channel.
   * @throws Exception if any error occurs.
   */
  public void disableRead() throws Exception {
    if (closed.get()) return;
    if (debugEnabled) log.debug("disabling read on {}", this);
    JMXChannelWrapper readingChannel = readingChannel();
    server.getTransitionManager().updateInterestOps(readingChannel.getContext().getSelectionKey(), SelectionKey.OP_READ, false);
    synchronized(readingChannel) {
      readingChannel.context.setState(null);
    }
  }

  /**
   * Disable writing to the channel.
   * @throws Exception if any error occurs.
   */
  public void disableWrite() throws Exception {
    if (closed.get()) return;
    if (debugEnabled) log.debug("disabling write on {}", this);
    JMXChannelWrapper writingChannel = writingChannel();
    server.getTransitionManager().updateInterestOps(writingChannel.getContext().getSelectionKey(), SelectionKey.OP_WRITE, false);
    synchronized(writingChannel) {
      writingChannel.context.setState(null);
    }
  }

  /**
   * Disable reading on the channel.
   * @throws Exception if any error occurs.
   */
  public void disableReadWrite() throws Exception {
    if (closed.get()) return;
    if (debugEnabled) log.debug("disabling read and write on {}", this);
    server.getTransitionManager().updateInterestOps(writingChannel().getContext().getSelectionKey(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, false);
    synchronized(writingChannel()) {
      writingChannel().context.setState(null);
    }
    synchronized(readingChannel()) {
      readingChannel().context.setState(null);
    }
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
   * @return the reading task. 
   */
  JMXTransitionTask getReadingTask() {
    return readingTask;
  }

  /**
   * @return the writing task. 
   */
  public JMXTransitionTask getWritingTask() {
    return writingTask;
  }
}
