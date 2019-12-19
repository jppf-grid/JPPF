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

/**
 * Writes all messages in the channel's context queue, if any, or keeps writing the current message until the socket send buffer is full.
 * @param <C> the type of connection context.
 * @author Laurent Cohen
 */
public abstract class NioMessageWriter<C extends AbstractNioContext> {
  /**
   * The server handling the connections.
   */
  protected final StatelessNioServer<C> server;

  /**
   * Initialize this message writer.
   * @param server the nio server.
   */
  public NioMessageWriter(final StatelessNioServer<C> server) {
    this.server = server;
  }

  /**
   * Write to the specified channel.
   * @param context the context to write to.
   * @return {@code true} if there is more to write, but the socket send buffer is full, {@code false} otherwise.
   * @throws Exception if any errort occurs.
   */
  protected boolean write(final C context) throws Exception {
    if (context.isSsl()) {
      synchronized(context.getSocketChannel()) {
        return doWrite(context);
      }
    }
    return doWrite(context);
  }

  /**
   * Write to the specified channel.
   * @param context the context to write to.
   * @return {@code true} if there is more to write, but the socket send buffer is full, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  protected  boolean doWrite(final C context) throws Exception {
    while (true) {
      NioMessage data = context.getWriteMessage();
      if (data == null) {
        data = context.nextMessageToSend();
        if (data == null) return false;
        context.setWriteMessage(data);
        preWrite(context, data);
      }
      if (context.writeMessage()) {
        context.setWriteMessage(null);
        postWrite(context, data);
      } else if (context.writeByteCount <= 0L) return true;
    }
  }

  /**
   * Method called just before a message is sent to the remote peer.
   * @param context the context that sent the message.
   * @param data the message sent.
   * @throws Exception if any error occurs.
   */
  protected void preWrite(final C context, final NioMessage data) throws Exception {
  }

  /**
   * Method called after a message is sent to the remote peer.
   * @param context the context that sent the message.
   * @param data the message sent.
   * @throws Exception if any error occurs.
   */
  protected abstract void postWrite(final C context, final NioMessage data) throws Exception;
}
