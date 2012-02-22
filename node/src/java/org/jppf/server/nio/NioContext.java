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


/**
 * Context associated with an open communication channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public interface NioContext<S extends Enum<S>>
{

  /**
   * Get the current state of the channel this context is associated with.
   * @return a state enum value.
   */
  S getState();

  /**
   * Set the current state of the channel this context is associated with.
   * @param state a state enum value.
   */
  void setState(S state);

  /**
   * Read data from a channel.
   * @param wrapper the channel to read the data from.
   * @return true if all the data has been read, false otherwise.
   * @throws Exception if an error occurs while reading the data.
   */
  boolean readMessage(ChannelWrapper<?> wrapper) throws Exception;

  /**
   * Write data to a channel.
   * @param wrapper the channel to write the data to.
   * @return true if all the data has been written, false otherwise.
   * @throws Exception if an error occurs while writing the data.
   */
  boolean writeMessage(ChannelWrapper<?> wrapper) throws Exception;

  /**
   * Get the uuid for this node context.
   * @return the uuid as a string.
   */
  String getUuid();

  /**
   * Set the uuid for this node context.
   * @param uuid the uuid as a string.
   */
  void setUuid(String uuid);

  /**
   * Handle the cleanup when an exception occurs on the channel.
   * @param channel the channel that threw the exception.
   */
  void handleException(ChannelWrapper<?> channel);

  /**
   * Get the associated channel.
   * @return a {@link ChannelWrapper} instance.
   */
  ChannelWrapper<?> getChannel();

  /**
   * Set the associated channel.
   * @param channel a {@link ChannelWrapper} instance.
   */
  void setChannel(ChannelWrapper<?> channel);

  /**
   * Get the SSL engine manager associated with the channel.
   * @return an instance of {@link SSLEngineManager}.
   */
  SSLEngineManager getSSLEngineManager();

  /**
   * Get the SSL engine associated with the channel.
   * @param sslEngineManager an instance of {@link SSLEngineManager}.
   */
  void setSSLEngineManager(SSLEngineManager sslEngineManager);
}
