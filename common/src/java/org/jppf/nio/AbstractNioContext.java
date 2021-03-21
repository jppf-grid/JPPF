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

package org.jppf.nio;

import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFSnapshot;
import org.slf4j.*;

/**
 * Context associated with an open communication channel.
 * @author Laurent Cohen
 */
public abstract class AbstractNioContext implements NioContext {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNioContext.class);
  /**
   * Uuid of the remote client or node.
   */
  protected String uuid;
  /**
   * Container for the current message data.
   */
  protected NioMessage readMessage;
  /**
   * The message to write, if any.
   */
  protected NioMessage writeMessage;
  /**
   * Unique ID for the corresponding connection on the remote peer.
   */
  protected String connectionUuid;
  /**
   * The SSL engine associated with the channel.
   */
  protected SSLHandler sslHandler;
  /**
   * Determines whether the associated channel is connected to a peer server.
   */
  protected boolean peer;
  /**
   * Determines whether the connection was opened on an SSL port.
   */
  protected boolean ssl;
  /**
   * Whether this context is enabled.
   */
  protected boolean enabled = true;
  /**
   * Whether this context has been closed.
   */
  protected final AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * An optional action to perform upon closing this context or its associated channel.
   */
  protected Runnable onCloseAction;
  /**
   * The number of bytes read by this channel.
   */
  public long readByteCount;
  /**
   * The number of bytes written by this channel.
   */
  public long writeByteCount;
  /**
   * The associated socket channel.
   */
  protected SocketChannel socketChannel;
  /**
   * Inbound traffic statistics snapshot.
   */
  protected JPPFSnapshot inSnapshot;
  /**
   * Outbound traffic statistics snapshot.
   */
  protected JPPFSnapshot outSnapshot;
  /**
   * The socket channel's interest ops.
   */
  private int interestOps;
  /**
   * Selection key for the associated socket channel and nio server selector.
   */
  private SelectionKey selectionKey;
  /**
   * Whether this is a local channel.
   */
  protected boolean local;

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public void setUuid(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * Get the container for the current message data.
   * @return an <code>NioMessage</code> instance.
   */
  public NioMessage getReadMessage() {
    return readMessage;
  }

  /**
   * Set the container for the current message data.
   * @param message an <code>NioMessage</code> instance.
   */
  public void setReadMessage(final NioMessage message) {
    this.readMessage = message;
  }

  /**
   * @return the message to write, if any.
   */
  public NioMessage getWriteMessage() {
    return writeMessage;
  }

  /**
   * 
   * @param writeMessage the message to write, if any.
   */
  public void setWriteMessage(final NioMessage writeMessage) {
    this.writeMessage = writeMessage;
  }

  /**
   * Get the next messge to send, if any.
   * @return the next message in the send queue, or {@code null} if the queue is empty.
   */
  protected NioMessage nextMessageToSend() {
    return null;
  }

  /**
   * Get the unique ID for the corresponding connection on the remote peer.
   * @return the id as a string.
   */
  public String getConnectionUuid() {
    return connectionUuid;
  }

  /**
   * Set the unique ID for the corresponding connection on the remote peer.
   * @param connectionUuid the id as a string.
   */
  public void setConnectionUuid(final String connectionUuid) {
    this.connectionUuid = connectionUuid;
  }

  @Override
  public SSLHandler getSSLHandler() {
    return sslHandler;
  }

  @Override
  public void setSSLHandler(final SSLHandler sslHandler) {
    this.sslHandler = sslHandler;
  }

  @Override
  public boolean isPeer() {
    return peer;
  }

  @Override
  public void setPeer(final boolean peer) {
    this.peer = peer;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", connectionUuid=").append(connectionUuid);
    sb.append(", peer=").append(peer);
    sb.append(", ssl=").append(ssl);
    sb.append(", socketChannel=").append(socketChannel);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Determine whether the associated channel is secured via SSL/TLS.
   * @return <code>true</code> if the channel is secure, <code>false</code> otherwise.
   */
  public boolean isSecure() {
    return sslHandler != null;
  }

  /**
   * Determines whether the connection was opened on an SSL port.
   * @return <code>true</code> for an SSL connection, <code>false</code> otherwise.
   */
  @Override
  public boolean isSsl() {
    return ssl;
  }

  /**
   * Specifies whether the connection was opened on an SSL port.
   * @param ssl <code>true</code> for an SSL connection, <code>false</code> otherwise.
   */
  @Override
  public void setSsl(final boolean ssl) {
    this.ssl = ssl;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public void setClosed(final boolean closed) {
    this.closed.set(closed);
  }

  /**
   * Set an optional action to perform upon closing this context or its associated channel.
   * @param onCloseAction a {@link Runnable} instance.
   */
  public void setOnCloseAction(final Runnable onCloseAction) {
    this.onCloseAction = onCloseAction;
  }

  /**
   * Callback that can be invoked upon closing this context or its associated channel.
   */
  protected void onClose() {
    if (onCloseAction != null) {
      try {
        onCloseAction.run();
      } catch (final Exception e) {
        log.error("error in onClose action for {}\n{}", this, ExceptionUtils.getStackTrace(e));
      }
    }
  }

  /**
   * @return the associated socket channel.
   */
  @Override
  public SocketChannel getSocketChannel() {
    return socketChannel;
  }

  @Override
  public int getInterestOps() {
    return interestOps;
  }

  @Override
  public void setInterestOps(final int interestOps) {
    this.interestOps = interestOps;
  }

  @Override
  public SelectionKey getSelectionKey() {
    return selectionKey;
  }

  @Override
  public void setSelectionKey(final SelectionKey selectionKey) {
    this.selectionKey = selectionKey;
  }

  /**
   * @return whether this is a local channel.
   */
  public boolean isLocal() {
    return local;
  }

  /**
   * 
   * @param local whether this is a local channel.
   */
  public void setLocal(final boolean local) {
    this.local = local;
  }
}
