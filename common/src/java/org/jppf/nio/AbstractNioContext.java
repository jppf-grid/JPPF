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

package org.jppf.nio;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.*;

/**
 * Context associated with an open communication channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public abstract class AbstractNioContext<S extends Enum<S>> implements NioContext<S> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNioContext.class);
  /**
   * The current state of the channel this context is associated with.
   */
  protected S state;
  /**
   * Uuid of the remote client or node.
   */
  protected String uuid;
  /**
   * Container for the current message data.
   */
  protected NioMessage message;
  /**
   * The associated channel.
   */
  protected ChannelWrapper<?> channel;
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

  @Override
  public S getState() {
    return state;
  }

  @Override
  public boolean setState(final S state) {
    this.state = state;
    return true;
  }

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
  public NioMessage getMessage() {
    return message;
  }

  /**
   * Set the container for the current message data.
   * @param message an <code>NioMessage</code> instance.
   */
  public void setMessage(final NioMessage message) {
    this.message = message;
  }

  @Override
  public ChannelWrapper<?> getChannel() {
    return channel;
  }

  @Override
  public void setChannel(final ChannelWrapper<?> channel) {
    this.channel = channel;
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
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("channel=").append(channel.getClass().getSimpleName()).append("[id=").append(channel.getId()).append(']');
    sb.append(", state=").append(getState());
    sb.append(", uuid=").append(uuid);
    sb.append(", connectionUuid=").append(connectionUuid);
    sb.append(", peer=").append(peer);
    sb.append(", ssl=").append(ssl);
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

  /**
   * Determine whether this channel has been closed.
   * @return {@code true} if this channel has been closed, {@code false} otherwise.
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Specify whether this channel has been closed.
   * @param closed {@code true} if this channel has been closed, {@code false} otherwise.
   */
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
      } catch (Exception e) {
        log.error(String.format("error in onClose action for %s", this), e);
      }
    }
  }
}
