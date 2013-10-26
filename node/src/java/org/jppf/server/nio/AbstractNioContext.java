/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.nio;

import org.slf4j.*;

/**
 * Context associated with an open communication channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public abstract class AbstractNioContext<S extends Enum<S>> implements NioContext<S>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNioContext.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The current state of the channel this context is associated with.
   */
  protected S state = null;
  /**
   * Uuid of the remote client or node.
   */
  protected String uuid = null;
  /**
   * Container for the current message data.
   */
  protected NioMessage message = null;
  /**
   * The associated channel.
   */
  protected ChannelWrapper<?> channel = null;
  /**
   * Unique ID for the corresponding connection on the remote peer.
   */
  protected String connectionUuid = null;
  /**
   * The SSL engine associated with the channel.
   */
  protected SSLHandler sslHandler = null;
  /**
   * Determines whether the associated channel is connected to a peer server.
   */
  protected boolean peer = false;
  /**
   * Determines whether the connection was opened on an SSL port.
   */
  protected boolean ssl = false;

  @Override
  public S getState()
  {
    return state;
  }

  @Override
  public boolean setState(final S state)
  {
    this.state = state;
    return true;
  }

  @Override
  public String getUuid()
  {
    return uuid;
  }

  @Override
  public void setUuid(final String uuid)
  {
    this.uuid = uuid;
  }

  /**
   * Give the non qualified name of the class of this instance.
   * @return a class name as a string.
   */
  protected String getShortClassName()
  {
    String fqn = getClass().getName();
    int idx = fqn.lastIndexOf('.');
    return fqn.substring(idx + 1);
  }

  /**
   * Get the container for the current message data.
   * @return an <code>NioMessage</code> instance.
   */
  public NioMessage getMessage()
  {
    return message;
  }

  /**
   * Set the container for the current message data.
   * @param message an <code>NioMessage</code> instance.
   */
  public void setMessage(final NioMessage message)
  {
    this.message = message;
  }

  @Override
  public ChannelWrapper<?> getChannel()
  {
    return channel;
  }

  @Override
  public void setChannel(final ChannelWrapper<?> channel)
  {
    this.channel = channel;
  }

  /**
   * Get the unique ID for the corresponding connection on the remote peer.
   * @return the id as a string.
   */
  public String getConnectionUuid()
  {
    return connectionUuid;
  }

  /**
   * Set the unique ID for the corresponding connection on the remote peer.
   * @param connectionUuid the id as a string.
   */
  public void setConnectionUuid(final String connectionUuid)
  {
    this.connectionUuid = connectionUuid;
  }

  @Override
  public SSLHandler getSSLHandler()
  {
    return sslHandler;
  }

  @Override
  public void setSSLHandler(final SSLHandler sslHandler)
  {
    this.sslHandler = sslHandler;
  }

  /**
   * Determine whether the associated channel is connected to a peer server.
   * @return <code>true</code> if the channel is connected to a peer server, <code>false</code> otherwise.
   */
  public boolean isPeer()
  {
    return peer;
  }

  /**
   * Specify whether the associated channel is connected to a peer server.
   * @param peer <code>true</code> if the channel is connected to a peer server, <code>false</code> otherwise.
   */
  public void setPeer(final boolean peer)
  {
    this.peer = peer;
  }

  @Override
  public String toString()
  {
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
  public boolean isSecure()
  {
    return sslHandler != null;
  }

  /**
   * Determines whether the connection was opened on an SSL port.
   * @return <code>true</code> for an SSL connection, <code>false</code> otherwise.
   */
  public boolean isSsl()
  {
    return ssl;
  }

  /**
   * Specifies whether the connection was opened on an SSL port.
   * @param ssl <code>true</code> for an SSL connection, <code>false</code> otherwise.
   */
  public void setSsl(final boolean ssl)
  {
    this.ssl = ssl;
  }
}
