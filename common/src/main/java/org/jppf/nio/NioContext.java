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

import java.nio.channels.SocketChannel;

/**
 * Context associated with an open communication channel.
 * @author Laurent Cohen
 */
public interface NioContext extends NioChannelHandler, CloseableContext {
  /**
   * Read data from a channel.
   * @return true if all the data has been read, false otherwise.
   * @throws Exception if an error occurs while reading the data.
   */
  boolean readMessage() throws Exception;

  /**
   * Write data to a channel.
   * @return true if all the data has been written, false otherwise.
   * @throws Exception if an error occurs while writing the data.
   */
  boolean writeMessage() throws Exception;

  /**
   * Get the uuid of the node or client for this context.
   * @return the uuid as a string.
   */
  String getUuid();

  /**
   * Set the uuid of the node or client for this context.
   * @param uuid the uuid as a string.
   */
  void setUuid(String uuid);

  /**
   * Handle the cleanup when an exception occurs on the channel.
   * @param e exception.
   */
  void handleException(final Exception e);

  /**
   * Get the SSL engine manager associated with the channel.
   * @return an instance of {@link SSLHandlerImpl}.
   */
  SSLHandler getSSLHandler();

  /**
   * Get the SSL engine associated with the channel.
   * @param sslHandler an instance of {@link SSLHandlerImpl}.
   */
  void setSSLHandler(SSLHandler sslHandler);

  /**
   * Determines whether the connection was opened on an SSL port.
   * @return <code>true</code> for an SSL connection, <code>false</code> otherwise.
   */
  boolean isSsl();

  /**
   * Specifies whether the connection was opened on an SSL port.
   * @param ssl <code>true</code> for an SSL connection, <code>false</code> otherwise.
   */
  void setSsl(final boolean ssl);

  /**
   * Determine whether the associated channel is connected to a peer server.
   * @return <code>true</code> if the channel is connected to a peer server, <code>false</code> otherwise.
   */
  boolean isPeer();

  /**
   * Specify whether the associated channel is connected to a peer server.
   * @param peer <code>true</code> if the channel is connected to a peer server, <code>false</code> otherwise.
   */
  void setPeer(boolean peer);

  /**
   * Whether this context is enabled.
   * @return {@code true} if this context is enabled, {@code false} otherwise.
   */
  boolean isEnabled();

  /**
   * Enable or disable this context.
   * @param enabled {@code true} to enable this context, {@code false} to disable it.
   */
  void setEnabled(boolean enabled);

  /**
   * Get the associated socket chanel, if any.
   * @return a {@link SocketChannel} instance, or null if this wrapper has no associated socket chanel.
   */
  SocketChannel getSocketChannel();
}
