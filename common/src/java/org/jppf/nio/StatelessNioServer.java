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

import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.GlobalExecutor;
import org.slf4j.*;

/**
 * 
 * @param <C> the type of connection context, a subclass of {@link AbstractNioContext}.
 * @author Laurent Cohen
 */
public abstract class StatelessNioServer<C extends AbstractNioContext> extends NioServer {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(StatelessNioServer.class);
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
   * 
   */
  protected KeysetHandler<C> acceptHandler, readHandler, writeHandler;

  /**
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @param configuration the JPPF configuration to use.
   * @throws Exception if any error occurs.
   */
  public StatelessNioServer(final int identifier, final boolean useSSL, final TypedProperties configuration) throws Exception {
    super(identifier, useSSL, null, configuration);
  }

  /**
   * Initialize this server with a specified port number and name.
   * @param name the name of this thread.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @param configuration the JPPF configuration to use.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected StatelessNioServer(final String name, final int identifier, final boolean useSSL, final TypedProperties configuration) throws Exception {
    super(name, identifier, useSSL, null, configuration);
  }

  @Override
  protected void init() throws Exception {
    initNioHandlers();
    initReaderAndWriter();
  }

  /**
   * 
   */
  protected void initNioHandlers() {
    acceptHandler = (server, key) -> {
      if (key.isAcceptable()) server.doAccept(key);
    };
    readHandler = (server, key) -> {
      if (key.isReadable()) server.handleRead(key);
    };
    writeHandler = (server, key) -> {
      if (key.isWritable()) server.handleWrite(key);
    };
  }

  /**
   * Initialize the message reader and writer.
   */
  protected abstract void initReaderAndWriter();

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
        try {
          if (n > 0) go(selector.selectedKeys());
        } catch (final RejectedExecutionException t) {
          log.error("error in selector loop for {} : ", getClass().getSimpleName(), t);
        }
      }
    } catch (final Throwable t) {
      log.error("error in selector loop for {} : ", getClass().getSimpleName(), t);
    } finally {
      end();
    }
  }

  /**
   * Process the keys selected by the selector for IO operations.
   * @param selectedKeys the set of keys that were selected by the latest <code>select()</code> invocation.
   * @throws Exception if an error is raised while processing the keys.
   */
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    final List<Future<?>> futures = new ArrayList<>();
    try {
      if (acceptHandler != null) futures.add(doOperation(selectedKeys, acceptHandler));
      if (readHandler   != null) futures.add(doOperation(selectedKeys, readHandler));
      if (writeHandler  != null) futures.add(doOperation(selectedKeys, writeHandler));
    } finally {
      for (final Future<?> f: futures) {
        try {
          f.get();
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
      selectedKeys.clear();
    }
  }

  /**
   * Perform an operation on all eligible channels.
   * @param selectedKeys set of selected keys from which to extract the eligible ones.
   * @param handler .
   * @return a future the result of the channels read performed in a separate thread.
   * @throws Exception if any error occurs.
   */
  protected Future<?> doOperation(final Set<SelectionKey> selectedKeys, final KeysetHandler<C> handler) throws Exception {
    return GlobalExecutor.getGlobalexecutor().submit(() -> handler.handle(this, selectedKeys));
  }

  /**
   * A fuctional interface wrapping an operation on a set of selected channels.
   * @param <C> the type of context associated with the channels.
   */
  @FunctionalInterface
  protected interface KeysetHandler<C extends AbstractNioContext> {
    /**
     * Logger for this class.
     */
    Logger log2 = LoggerFactory.getLogger(KeysetHandler.class);

    /**
     * Handle the operation on a set of channels.
     * @param server the associated nio server.
     * @param selectedKeys the set of channels to handle.
     */
    default void handle(final StatelessNioServer<C> server, final Set<SelectionKey> selectedKeys) {
      for (final SelectionKey key: selectedKeys) {
        try {
          if (key.attachment() instanceof CloseableContext) {
            final CloseableContext context = (CloseableContext) key.attachment();
            if (context.isClosed()) continue;
          }
          if (!isKeyValid(key)) {
            if (log2.isDebugEnabled()) log2.debug("invalid key for {}", key.attachment());
            continue;
          }
          handleKey(server, key);
        } catch (final Exception e) {
          key.cancel();
          if (log2.isDebugEnabled()) log2.debug("error on {}", StatelessNioServer.toString(key), e);
          GlobalExecutor.getGlobalexecutor().execute(() -> server.handleSelectionException(key, e));
        }
      }
    }

    /**
     * Handle the operation on a single channel.
     * @param server the associated nio server.
     * @param key the channel to handle.
     * @throws Exception if any error occurs.
     */
    void handleKey(final StatelessNioServer<C> server, SelectionKey key) throws Exception;
  }

  /**
   * Called when a selection key is selected and {@link SelectionKey#isReadable() readable}.
   * @param key the key to handle.
   * @throws Exception if any error occurs.
   */
  protected void handleRead(final SelectionKey key) throws Exception {
    @SuppressWarnings("unchecked")
    final C context = (C) key.attachment();
    if (!context.isClosed()) messageReader.read(context);
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
    if (messageWriter.write(context)) updateInterestOpsNoWakeup(key, SelectionKey.OP_WRITE, true);
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

  /**
   * Get all connections accepted by this server.
   * This method is a shorthand for {@link #performContextAction(ContextFilter, ContextAction) performContextAction(null, null)}.
   * @return a list of {@link C} instances that passed the filter, possibly empty but never {@code null}.
   */
  public Map<String, C> getAllContexts() {
    return performContextAction(null, null);
  }

  /**
   * Perform the specified action on the contexts that pass the specified filter.
   * @param filter the context filter, may be {@code null}, in which case all contexts are accepted.
   * @param action the action to execute, may be {@code null}, in which case no action is performed.
   * @return a list of {@link C} instances that passed the filter, possibly empty but never {@code null}.
   */
  public Map<String, C> performContextAction(final ContextFilter<C> filter, final ContextAction<C> action) {
    Set<SelectionKey> keys = null;
    sync.wakeUpAndSetOrIncrement();
    try {
      keys  = new HashSet<>(selector.keys());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      sync.decrement();
    }
    if (keys == null) return Collections.emptyMap();
    final Map<String, C> channels = new HashMap<>(keys.size());
    for (final SelectionKey key: keys) {
      @SuppressWarnings("unchecked")
      final C context = (C) key.attachment();
      if ((filter == null) || filter.accepts(context)) {
        try {
          if (action != null) action.execute(context);
        } catch (final Exception e) {
          log.error("error trying to run action {} on {}", action, context, e);
        }
        channels.put(context.getUuid(), context);
      }
    }
    return channels;
  }

  /**
   * Interface for context filtering.
   * @param <C> the type of connection context.
   */
  @FunctionalInterface
  public interface ContextFilter<C extends AbstractNioContext> {
    /**
     * @param context the context to check.
     * @return {@code true} if the context is accepted, {@code false} otherwise.
     */
    boolean accepts(C context);
  }

  /**
   * An action ot execute on a context.
   * @param <C> the type of connection context.
   */
  @FunctionalInterface
  public interface ContextAction<C extends AbstractNioContext> {
    /**
     * @param context the context on which to execute the action.
     */
    void execute(C context);
  }

  /**
   * 
   * @param key the selection key to describe.
   * @return a string description of the selection key.
   */
  public static String toString(final SelectionKey key) {
    if (key == null) return "null";
    return new StringBuilder(SelectionKey.class.getName()).append('[')
      .append("valid=").append(key.isValid())
      .append(", channel=").append(key.channel())
      .append(", attachment=").append(key.attachment())
      .append(']').toString();
  }

  @Override
  public String toString() {
    return super.toString() + " -> " + SystemUtils.getSystemIdentityName(this);
  }
}
