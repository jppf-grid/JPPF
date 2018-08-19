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
 * @author Laurent Cohen
 */
public abstract class StatelessNioServer extends NioServer<EmptyEnum, EmptyEnum> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(StatelessNioServer.class);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();

  /**
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if any error occurs.
   */
  public StatelessNioServer(final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL);
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
  }

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
      if (!key.isValid()) continue;
      final CloseableContext context = (CloseableContext) key.attachment();
      try {
        if (context.isClosed()) continue;
        if (key.isReadable()) handleRead(key);
        if (key.isWritable()) handleWrite(key);
      } catch (final Exception e) {
        handleSelectionException(key, e);
      }
    }
  }

  /**
   * Called when a selection key is selected and {@link SelectionKey#isReadable() readable}.
   * @param key the key to handle.
   * @throws Exception if any error occurs.
   */
  protected abstract void handleRead(final SelectionKey key) throws Exception;

  /**
   * Called when a selection key is selected and {@link SelectionKey#isWritable() writable}.
   * @param key the key to handle.
   * @throws Exception if any error occurs.
   */
  protected abstract void handleWrite(final SelectionKey key) throws Exception;

  /**
   * Called when a selection key is {@link SelectionKey#isWritable() writable}.
   * @param key the key to handle.
   * @param e the exception to handle.
   * @throws Exception if any error occurs.
   */
  protected abstract void handleSelectionException(final SelectionKey key, final Exception e) throws Exception;

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
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key));
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
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key));
      key.interestOps(newOps);
      channelHandler.setInterestOps(newOps);
    }
  }
}
