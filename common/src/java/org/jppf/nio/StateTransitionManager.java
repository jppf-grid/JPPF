/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import static java.nio.channels.SelectionKey.OP_WRITE;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class manage the state transitions of channels opened via a {@link NioServer}.
 * @param <S> type safe enum of the possible states for a channel.
 * @param <T> type safe enum of the possible state transitions for a channel.
 * @author Laurent Cohen
 */
public class StateTransitionManager<S extends Enum<S>, T extends Enum<T>> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(StateTransitionManager.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The server for which this transition manager is intended.
   */
  private final NioServer<S, T> server;
  /**
   * The state and transition factory associated with the server.
   */
  private NioServerFactory<S, T> factory;
  /**
   * Determines whether the channels handled by the server are node job channels.
   */
  private final boolean isNodeServer;
  /**
   * Global thread pool used by all NIO servers.
   */
  private final ExecutorService executor;
  /**
   * The server lock.
   */
  private final Lock lock;

  /**
   * Initialize this transition manager with the specified server and sequential flag.
   * @param server the server for which this transition manager is intended.
   * performed sequentially or through the executor thread pool.
   */
  public StateTransitionManager(final NioServer<S, T> server) {
    this(server, NioHelper.getGlobalexecutor());
  }

  /**
   * Initialize this transition manager with the specified server and sequential flag.
   * @param server the server for which this transition manager is intended.
   * performed sequentially or through the executor thread pool.
   * @param executor the executor service to use.
   */
  public StateTransitionManager(final NioServer<S, T> server, final ExecutorService executor) {
    this.server = server;
    this.executor = executor;
    this.factory = server.getFactory();
    this.lock = server.getLock();
    isNodeServer = server.getIdentifier() == JPPFIdentifiers.NODE_JOB_DATA_CHANNEL;
  }

  /**
   * Submit the specified runnable to the executor.
   * @param runnable the runnable to submit.
   */
  public void execute(final Runnable runnable) {
    executor.execute(runnable);
  }

  /**
   * Submit the specified runnable to the executor.
   * @param runnable the runnable to submit.
   * @return a future.
   */
  public Future<?> submit(final Runnable runnable) {
    return executor.submit(runnable);
  }

  /**
   * Submit the next state transition for a specified channel.
   * @param channel the selection key that references the channel.
   */
  public void submitTransition(final ChannelWrapper<?> channel) {
    if (debugEnabled && "NodeNioServer".equals(server.getClass().getSimpleName())) log.debug("submitting transition for channel id={}", channel.getId());
    synchronized(channel) {
      setInterestOps(channel, 0);
    }
    executor.execute(new StateTransitionTask<>(channel, factory));
  }

  /**
   * Set the interest ops of a specified selection key, ensuring no blocking occurs while doing so.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param channel the key on which to set the interest operations.
   * @param interestOps the operations to set on the key.
   */
  private void setInterestOps(final ChannelWrapper<?> channel, final int interestOps) {
    lock.lock();
    try {
      server.wakeUpSelectorIfNeeded();
      channel.setInterestOps(interestOps);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Transition the specified channel to the specified state.
   * @param channel the key holding the channel and associated context.
   * @param transition holds the new state of the channel and associated key ops.
   * @throws Exception if any error occurs.
   */
  public void transitionChannel(final ChannelWrapper<?> channel, final T transition) throws Exception {
    transitionChannel(channel, transition, false);
  }

  /**
   * Transition the specified channel to the specified state.
   * @param channel the key holding the channel and associated context.
   * @param transition holds the new state of the channel and associated key ops.
   * @param submit specifies whether the transition should be submitted immediately.
   * or if we should wait for the server to submit it.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  public void transitionChannel(final ChannelWrapper<?> channel, final T transition, final boolean submit) throws Exception {
    if (traceEnabled) log.trace("transition {} for channel {}", transition, channel);
    lock.lock();
    try {
      server.wakeUpSelectorIfNeeded();
      synchronized(channel) {
        channel.setInterestOps(0);
        final NioContext<S> context = (NioContext<S>) channel.getContext();
        final S s1 = context.getState();
        final NioTransition<S> t = factory.getTransition(transition);
        final S s2 = t.getState();
        if (s1 != null) if (debugEnabled && (s1 != s2)) log.debug("transition" + getTransitionMessage(s1, s2, t, channel, submit));
        else if (traceEnabled) log.trace(getTransitionMessage(s1, s2, t, channel, submit));
        if (context.setState(s2)) if (!submit) {
          channel.setInterestOps(t.getInterestOps());
          if (traceEnabled) log.trace("set interestOps={} for channel id={}", t.getInterestOps(), channel.getId());
        } else {
          submitTransition(channel);
          if (traceEnabled) log.trace("submitted transition={} for channel id={}", t, channel.getId());
        }
      }
    } catch (final Exception e) {
      log.info(e.getMessage(), e);
      throw e;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Build a message for the specified parameters.
   * @param s1 old channel state.
   * @param s2 new channel state.
   * @param t transition.
   * @param channel the channel.
   * @param submit specifies whether the transition should be submitted immediately.
   * @return a string message.
   */
  private String getTransitionMessage(final S s1, final S s2, final NioTransition<S> t, final ChannelWrapper<?> channel, final boolean submit) {
    try {
      return StringUtils.build(" from ", s1, " to ", s2, " with ops=", t.getInterestOps(), " (readyOps=", channel.getReadyOps(), ") for channel id=", channel.getId(), ", submit=", submit);
    } catch(final Exception e) {
      return "could not build transition message: " + ExceptionUtils.getMessage(e);
    }
  }

  /**
   * @param channel the channel to check.
   * @return true or false.
   */
  public boolean checkSubmitTransition(final ChannelWrapper<?> channel) {
    if (channel.isLocal()) return false;
    final SSLHandler sslHandler = channel.getContext().getSSLHandler();
    if (sslHandler == null) return false;
    final int interestOps = channel.getInterestOps();
    final boolean b = (interestOps != channel.getReadyOps()) && (interestOps != 0) && !server.isIdle(channel) &&
      ((sslHandler.getAppReceiveBuffer().position() > 0) || (sslHandler.getNetReceiveBuffer().position() > 0));
    return b;
  }

  /**
   * @param channel the channel to check.
   * @param transition the transition about to be performed on the channel.
   * @return true or false.
   */
  public boolean checkSubmitTransition(final ChannelWrapper<?> channel, final T transition) {
    try {
      if (channel.isLocal()) return false;
      final int interestOps = factory.getTransition(transition).getInterestOps();
      final int readyOps = channel.getReadyOps();
      // TODO: investigate why this is necessary, why when stress-testing offline nodes, one of the node channels
      // gets stuck with readyOps=4 and interestOps=5 (for state SENDING_BUNDLE) but the selector doesn't select the corresponding key.
      if (isNodeServer && ((interestOps & OP_WRITE) != 0) && ((readyOps & OP_WRITE) != 0)) return true;
      final SSLHandler sslHandler = channel.getContext().getSSLHandler();
      if (sslHandler == null) return false;
      final boolean b = (interestOps != readyOps) && (interestOps != 0) && !server.isIdle(channel) &&
        ((sslHandler.getAppReceiveBuffer().position() > 0) || (sslHandler.getNetReceiveBuffer().position() > 0));
      return b;
    } catch (final RuntimeException e) {
      log.error(String.format("error for transition=%s, channel=%s, exception=%s", transition, channel, e));
      throw e;
    }
  }

  /**
   * Set the state and transition factory associated with the server.
   * @param factory an instance of {@link NioServerFactory}.
   */
  public void setFactory(final NioServerFactory<S, T> factory) {
    this.factory = factory;
  }

  /**
   * @return an {@link ExecutorService}.
   */
  public ExecutorService getExecutor() {
    return executor;
  }
}
