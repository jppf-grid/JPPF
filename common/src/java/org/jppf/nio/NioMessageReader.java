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

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off to a global thread pool for deserialization and processing.
 * @param <C> the type of connection context.
 * @author Laurent Cohen
 */
public abstract class NioMessageReader<C extends AbstractNioContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NioMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The server handling the connections.d
   */
  protected final StatelessNioServer<C> server;

  /**
   * Initialize this message reader.
   * @param server the nio server.
   */
  public NioMessageReader(final StatelessNioServer<C> server) {
    this.server = server;
  }

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  protected void read(final C context) throws Exception {
    if (context.isSsl()) {
      synchronized(context.getSocketChannel()) {
        doRead(context);
      }
    } else doRead(context);
  }

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  protected void doRead(final C context) throws Exception {
    while (true) {
      final boolean b = context.readMessage();
      if (b) {
        final NioMessage message = context.getReadMessage();
        if (debugEnabled) log.debug("read message {} from {}", message, context);
        context.setReadMessage(null);
        NioHelper.getGlobalexecutor().execute(new HandlingTask<>(context, message, createMessageHandler()));
      } else if (context.readByteCount <= 0L) break;
    }
  }

  /**
   * Create a handling task with the specified context and message.
   * @return the created task.
   */
  protected abstract MessageHandler<C> createMessageHandler();

  /**
   * @param <C> the type of connection context.
   */
  @FunctionalInterface
  protected static interface MessageHandler<C extends AbstractNioContext> {
    /**
     * Execute this task.
     * @param context the context associated with the channel.
     * @param message the message to handle.
     * @throws Exception iuf any error occurs.
     */
    void execute(final C context, final NioMessage message) throws Exception;
  }

  /**
   * Instances of this task deserialize and process a NioMessage that was read from the network channel.
   * @param <C> the type of connection context.
   */
  private static final class HandlingTask<C extends AbstractNioContext> implements Runnable {
    /**
     * The context associated with the channel.
     */
    final C context;
    /**
     * The message to handle.
     */
    final NioMessage message;
    /**
     * 
     */
    final MessageHandler<C> messageHandler;

    /**
     * Initialize with the specified context and message.
     * @param context the context associated with the channel.
     * @param message the message to handle.
     * @param messageHandler the message handler.
     */
    public HandlingTask(final C context, final NioMessage message, final MessageHandler<C> messageHandler) {
      this.context = context;
      this.message = message;
      this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
      try {
        messageHandler.execute(context, message);
      } catch(final Exception|Error e) {
        try {
          if (debugEnabled) log.debug("error on channel {} :\n{}", context, ExceptionUtils.getStackTrace(e));
          else log.warn("error on channel {} : {}", context, ExceptionUtils.getMessage(e));
        } catch (final Exception e2) {
          if (debugEnabled) log.debug("error on channel: {}", ExceptionUtils.getStackTrace(e2));
          else log.warn("error on channel: {}", ExceptionUtils.getMessage(e2));
        }
        if (e instanceof Exception) context.handleException((Exception) e);
        else throw (Error) e;
      }
    }
  }
}
