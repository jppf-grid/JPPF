/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
   * Uuid for this node context.
   */
  protected String uuid = null;
  /**
   * Container for the current message data.
   */
  protected NioMessage message = null;
  /**
   * Count of bytes read.
   */
  public int readByteCount = -1;
  /**
   * Count of bytes written.
   */
  public int writeByteCount = -1;
  /**
   * The associated channel.
   */
  private ChannelWrapper<?> channel = null;
  /**
   * Unique ID for the corresponding connection on the remote peer.
   */
  protected String connectionUuid = null;
  /**
   * The SSL engine associated with the channel.
   */
  protected SSLEngineManager sslEngineManager = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public S getState()
  {
    return state;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setState(final S state)
  {
    this.state = state;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUuid()
  {
    return uuid;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ChannelWrapper<?> getChannel()
  {
    return channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setChannel(final ChannelWrapper<?> channel)
  {
    this.channel = channel;
  }

  /**
   * Get the nique ID for the corresponding connection on the remote peer.
   * @return the id as a string.
   */
  public String getConnectionUuid()
  {
    return connectionUuid;
  }

  /**
   * Set the nique ID for the corresponding connection on the remote peer.
   * @param connectionUuid the id as a string.
   */
  public void setConnectionUuid(final String connectionUuid)
  {
    this.connectionUuid = connectionUuid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SSLEngineManager getSSLEngineManager()
  {
    return sslEngineManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSSLEngineManager(final SSLEngineManager sslEngineManager)
  {
    this.sslEngineManager = sslEngineManager;
  }
}
