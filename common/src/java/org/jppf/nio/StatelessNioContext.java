/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import org.jppf.utils.EmptyEnum;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class StatelessNioContext extends AbstractNioContext<EmptyEnum> {
  /**
   * The computed number of bytes for the last written message.
   */
  public long writeByteCount;
  /**
   * The message to write, if any.
   */
  protected NioMessage writeMessage;
  /**
   * Whether this is a local channel.
   */
  protected boolean local;

  @Override
  public boolean readMessage(final ChannelWrapper<?> channel) throws Exception {
    return readMessage();
  }

  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception {
    return writeMessage();
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
   * @return whether a message was fully read.
   * @throws Exception if any error occurs.
   */
  public abstract boolean readMessage() throws Exception;

  /**
   * @return whether a message was fully written.
   * @throws Exception if any error occurs.
   */
  public abstract boolean writeMessage() throws Exception;

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
