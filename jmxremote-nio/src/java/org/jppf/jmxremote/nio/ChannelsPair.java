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
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.*;
import javax.management.remote.*;
import javax.security.auth.Subject;

import org.jppf.jmxremote.*;
import org.jppf.jmxremote.message.JMXMessageHandler;
import org.jppf.utils.Pair;
import org.slf4j.*;

/**
 * Convenience class to group a pair of channels respectively reading from and writing to the same socket channel.
 * @author Laurent Cohen
 */
public class ChannelsPair extends Pair<JMXContext, JMXContext> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ChannelsPair.class);
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
  private JPPFJMXConnector jmxConnector;
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
   * Callbacks invoked upon {@link #close()}.
   */
  private final List<CloseCallback> closeCallbacks = new CopyOnWriteArrayList<>();
  /**
   * The server-side authenticator.
   */
  private final JMXAuthenticator authenticator;
  /**
   * The seriver-side authorization checker.
   */
  private JMXAuthorizationChecker auhtorizationChecker;
  /**
   * The authenticated subject, if any.
   */
  private Subject subject;

  /**
   * @param first the reading channel.
   * @param second the writing channel.
   * @param server the JMX nio server.
   * @param authenticator an optional {@link JMXAuthenticator} (server-side only).
   */
  public ChannelsPair(final JMXContext first, final JMXContext second, final JMXNioServer server, final JMXAuthenticator authenticator) {
    super(first, second);
    this.authenticator = authenticator;
    writingTask = new JMXTransitionTask(second, server, true);
    nonSelectingWritingTask = new JMXTransitionTask(second, server, false);
  }

  /**
   * @return the reading channel.
   */
  public JMXContext readingContext() {
    return first();
  }

  /**
   * @return the writing channel.
   */
  public JMXContext writingContext() {
    return second();
  }

  /**
   * Close the channels and the underlying socket channel.
   * @param exception an optional exception that may have cause the close, may be null.
   * @throws Exception if any error occurs.
   */
  public void close(final Exception exception) throws Exception {
    try {
    if (closed.compareAndSet(false, true)) this.getSelectionKey().channel().close();
    } finally {
      final List<CloseCallback> tmp = new ArrayList<>(closeCallbacks);
      closeCallbacks.clear();
      for (CloseCallback runnable: tmp) runnable.onClose(exception);
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
   * @return the associated JMXConnector (client-side).
   */
  public JPPFJMXConnector getJMXConnector() {
    return jmxConnector;
  }

  /**
   * Set the associated JMXConnector (client-side).
   * @param mbeanServerConnection the {@link JMXConnector} to set.
   */
  public void setJMXConnector(final JPPFJMXConnector mbeanServerConnection) {
    this.jmxConnector = mbeanServerConnection;
  }

  @Override
  public String toString() {
    return  new StringBuilder(getClass().getSimpleName()).append('[')
      .append("readingChannelID=").append(readingContext().getId())
      .append(", writingChannelID=").append(writingContext().getId())
      .append(", connectionID=").append(connectionID)
      .append(", closed=").append(closed.get())
      .append(", closing=").append(closing.get())
      .append(", serverSide=").append(serverSide)
      .append(", socketChannel=").append(selectionKey == null ? "null" : selectionKey.channel())
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

  /**
   * Add a callback to invoke upon {@link #close()}.
   * @param callback the callback to add.
   */
  public void addCloseCallback(final CloseCallback callback) {
    if (callback != null) closeCallbacks.add(callback);
  }

  /**
   * Remove a callback to invoke upon {@link #close()}.
   * @param callback the callback to remove.
   */
  public void removeCloseCallback(final CloseCallback callback) {
    if (callback != null) closeCallbacks.remove(callback);
  }

  /**
   * A callback invoked upon closing the connection.
   */
  public static interface CloseCallback {
    /**
     * Invoked when the {@link ChannelsPair#close(Exception)} is called.
     * @param exception an optional exception that may have cause the close, may be null.
     */
    void onClose(final Exception exception);
  }

  /**
   * @return the server-side authenticator, if any.
   */
  JMXAuthenticator getAuthenticator() {
    return authenticator;
  }

  /**
   * @return the authenticated subject, if any.
   */
  Subject getSubject() {
    return subject;
  }

  /**
   * Set the subjerct.
   * @param subject the authenticated subject.
   */
  void setSubject(final Subject subject) {
    this.subject = subject;
  }

  /**
   * @return the seriver-side authorization checker.
   */
  JMXAuthorizationChecker getAuhtorizationChecker() {
    return auhtorizationChecker;
  }

  /**
   * Set the seriver-side authorization checker.
   * @param checker either the class object or the class name of the autorization checker to set.
   */
  void setAuhtorizationChecker(final Object checker) {
    if (checker == null) return;
    try {
      Class<?> c = null;
      if (checker instanceof Class) c = (Class<?>) checker;
      else if (checker instanceof String) c = Class.forName((String) checker, false, Thread.currentThread().getContextClassLoader());
      else throw new JMException("unable to interpret authorization checker parameter " + checker);
      auhtorizationChecker = (JMXAuthorizationChecker) c.newInstance();
    } catch (final Exception e) {
      log.error("error setting the authorization checker, ACL disabled for this connection", e);
    }
  }
}
