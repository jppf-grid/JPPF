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

import java.nio.channels.*;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @param <C> the type of connection context.
 * @author Laurent Cohen
 */
public abstract class StatelessNioServer<C extends StatelessNioContext> extends NioServer<EmptyEnum, EmptyEnum> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(StatelessNioServer.class);
  /**
   * Determines whether debug logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace logging level is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The nio message reader.
   */
  protected NioMessageReader<C> messageReader;
  /**
   * The nio message writer.
   */
  protected NioMessageWriter<C> messageWriter;

  /**
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if any error occurs.
   */
  public StatelessNioServer(final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL);
    initReaderAndWriter();
  }

  /**
   * Initialize this server with a specified port number and name.
   * @param name the name of this thread.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected StatelessNioServer(final String name, final int identifier, final boolean useSSL) throws Exception {
    super(name, identifier, useSSL);
    initReaderAndWriter();
  }

  /**
   * Initialize this server with a specified list of port numbers and name.
   * @param ports the list of ports this server accepts connections from.
   * @param sslPorts the list of SSL ports this server accepts connections from.
   * @param identifier the channel identifier for channels handled by this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public StatelessNioServer(final int[] ports, final int[] sslPorts, final int identifier) throws Exception {
    super(ports, sslPorts, identifier);
    initReaderAndWriter();
  }

  /**
   * Initialize the message reader and writer.
   */
  protected abstract void initReaderAndWriter();

  @Override
  protected NioServerFactory<EmptyEnum, EmptyEnum> createFactory() {
    return null;
  }

  @Override
  public void run() {
    try {
      final boolean hasTimeout = selectTimeout > 0L;
      int n = 0;
      while (!isStopped() && !externalStopCondition()) {
        sync.waitForZeroAndSetToMinusOne();
        try {
          n = hasTimeout ? selector.select(selectTimeout) : selector.select();
        } finally {
          sync.setToZeroIfNegative();
        }
        if (n > 0) go(selector.selectedKeys());
      }
    } catch (final Throwable t) {
      log.error("error in selector loop for {} : {}", getClass().getSimpleName(), ExceptionUtils.getStackTrace(t));
    } finally {
      end();
    }
  }

  @Override
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    final Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      final SelectionKey key = it.next();
      it.remove();
      if (!isKeyValid(key)) {
        if (debugEnabled) log.debug("invalid key for {}", key.attachment());
        continue;
      }
      try {
        if (key.isAcceptable()) {
          doAccept(key);
        } else {
          @SuppressWarnings("unchecked")
          final CloseableContext context = (CloseableContext) key.attachment();
          if (context.isClosed()) continue;
          if (key.isReadable()) handleRead(key);
          if (isKeyValid(key) && key.isWritable()) handleWrite(key);
        }
      } catch (final Exception e) {
        key.cancel();
        transitionManager.execute(() -> handleSelectionException(key, e));
      }
    }
  }

  /**
   * Called when a selection key is selected and {@link SelectionKey#isReadable() readable}.
   * @param key the key to handle.
   * @throws Exception if any error occurs.
   */
  protected void handleRead(final SelectionKey key) throws Exception {
    @SuppressWarnings("unchecked")
    final C context = (C) key.attachment();
    messageReader.read(context);
  }

  /**
   * Called when a selection key is selected and {@link SelectionKey#isWritable() writable}.
   * @param key the key to handle.
   * @throws Exception if any error occurs.
   */
  protected void handleWrite(final SelectionKey key) throws Exception {
    updateInterestOpsNoWakeup(key, SelectionKey.OP_WRITE, false);
    @SuppressWarnings("unchecked")
    final C context = (C) key.attachment();
    messageWriter.write(context);
    updateInterestOpsNoWakeup(key, SelectionKey.OP_WRITE, true);
  }

  /**
   * Called when a selection key is {@link SelectionKey#isWritable() writable}.
   * @param key the key to handle.
   * @param e the exception to handle.
   */
  protected abstract void handleSelectionException(final SelectionKey key, final Exception e);

  /**
   * Set the interest ops of a specified selection key, ensuring no blocking occurs while doing so.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param update the operations to update on the key.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   * @throws Exception if any error occurs.
   */
  public void updateInterestOps(final SelectionKey key, final int update, final boolean add) throws Exception {
    final NioChannelHandler context = (NioChannelHandler) key.attachment();
    final int ops = context.getInterestOps();
    final int newOps = add ? ops | update : ops & ~update;
    if (newOps != ops) {
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key.attachment()));
      context.setInterestOps(newOps);
      sync.wakeUpAndSetOrIncrement();
      try {
        key.interestOps(newOps);
      } finally {
        sync.decrement();
      }
    }
  }

  /**
   * Register the specified channel with this server's selectior.
   * @param channelHandler the context associated with the channel.
   * @param channel the channel to register.
   * @throws Exception if any error occurs.
   */
  public void registerChannel(final NioChannelHandler channelHandler, final SocketChannel channel) throws Exception {
    final int ops = SelectionKey.OP_READ;
    channelHandler.setInterestOps(ops);
    sync.wakeUpAndSetOrIncrement();
    try {
      channelHandler.setSelectionKey(channel.register(selector, ops, channelHandler));
    } finally {
      sync.decrement();
    }
  }

  /**
   * Set the interest ops of a specified selection key.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param update the operations to update on the key.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   */
  public static void updateInterestOpsNoWakeup(final SelectionKey key, final int update, final boolean add) {
    final NioChannelHandler channelHandler = (NioChannelHandler) key.attachment();
    final int ops = channelHandler.getInterestOps();
    final int newOps = add ? ops | update : ops & ~update;
    if (newOps != ops) {
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key.attachment()));
      key.interestOps(newOps);
      channelHandler.setInterestOps(newOps);
    }
  }

  /**
   * @param key the key to check.
   * @return whether the key is valid or not.
   */
  public static boolean isKeyValid(final SelectionKey key) {
    return key.isValid() && key.channel().isOpen();
  }
}
