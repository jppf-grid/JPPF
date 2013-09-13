/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.io.IOException;
import java.nio.channels.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class manage the state transitions of channels opened via a <code>NioServer</code>.
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The pool of threads used for submitting channel state transitions.
   */
  protected ExecutorService executor = null;
  /**
   * The server for which this transition manager is intended.
   */
  private final NioServer<S, T> server;
  /**
   * The state and transition factory associated with the server.
   */
  private final NioServerFactory<S, T> factory;

  /**
   * Initialize this transition manager with the specified server and sequential flag.
   * @param server the server for which this transition manager is intended.
   * performed sequentially or through the executor thread pool.
   */
  public StateTransitionManager(final NioServer<S, T> server) {
    this.server = server;
    this.factory = server.getFactory();
    executor = Executors.newFixedThreadPool(NioConstants.THREAD_POOL_SIZE, new JPPFThreadFactory(server.getName()));
  }

  /**
   * Submit the next state transition for a specified channel.
   * @param channel the selection key that references the channel.
   */
  public void submitTransition(final ChannelWrapper<?> channel) {
    setInterestOps(channel, 0);
    executor.submit(new StateTransitionTask<>(channel, factory));
  }

  /**
   * Set the interest ops of a specified selection key, ensuring no blocking occurs while doing so.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param interestOps the operations to set on the key.
   */
  private void setInterestOps(final ChannelWrapper<?> key, final int interestOps) {
    Lock lock = server.getLock();
    lock.lock();
    try {
      server.getSelector().wakeup();
      key.setInterestOps(interestOps);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Transition the specified channel to the specified state.
   * @param channel the key holding the channel and associated context.
   * @param transition holds the new state of the channel and associated key ops.
   */
  public void transitionChannel(final ChannelWrapper<?> channel, final T transition) {
    transitionChannel(channel, transition, false);
  }

  /**
   * Transition the specified channel to the specified state.
   * @param channel the key holding the channel and associated context.
   * @param transition holds the new state of the channel and associated key ops.
   * @param submit specifies whether the transition should be submitted immediately,
   * or if we should wait for the server to submit it.
   */
  @SuppressWarnings("unchecked")
  public void transitionChannel(final ChannelWrapper<?> channel, final T transition, final boolean submit) {
    Lock lock = server.getLock();
    lock.lock();
    try {
      synchronized(channel) {
        server.getSelector().wakeup();
        channel.setInterestOps(0);
        NioContext<S> context = (NioContext<S>) channel.getContext();
        S s1 = context.getState();
        NioTransition<S> t = factory.getTransition(transition);
        S s2 = t.getState();
        if (s1 != null) {
          /*if (!factory.isTransitionAllowed(s1, s2)) log.warn("unauthorized transition" + getTransitionMessage(s1, s2, t, channel));
          else */if (debugEnabled && (s1 != s2)) log.debug("transition" + getTransitionMessage(s1, s2, t, channel));
          else if (traceEnabled) log.trace(getTransitionMessage(s1, s2, t, channel));
        }
        if (context.setState(s2)) {
          if (!submit) channel.setInterestOps(t.getInterestOps());
          else submitTransition(channel);
        }
      }
    } catch (RuntimeException e) {
      //if (debugEnabled) log.debug(e.getMessage(), e);
      log.info(e.getMessage(), e);
      throw e;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Build a message for the specified parameter.
   * @param s1 old channel state.
   * @param s2 new channel state.
   * @param t transition.
   * @param channel the channel.
   * @return a string message.
   */
  private String getTransitionMessage(final S s1, final S s2, final NioTransition<S> t, final ChannelWrapper<?> channel) {
    try {
      return StringUtils.build(" from ", s1, " to ", s2, " with ops=", t.getInterestOps(), " for channel ", channel);
    } catch(Exception e) {
      return "could not build transition message: " + ExceptionUtils.getMessage(e);
    }
  }

  /**
   * Register a channel not opened through this server, with initial interest operation set to 0.
   * @param channel the channel to register.
   * @param context the context attached to the channel.
   * @return a {@link ChannelWrapper} instance.
   */
  public ChannelWrapper<?> registerChannel(final SocketChannel channel, final NioContext context) {
    return registerChannel(channel, 0, context);
  }

  /**
   * Register a channel not opened through this server.
   * @param channel the channel to register.
   * @param interestOps the operations the channel is initially interested in.
   * @param context the context attached to the channel.
   * @return a {@link ChannelWrapper} instance.
   */
  @SuppressWarnings("unchecked")
  private ChannelWrapper<?> registerChannel(final SocketChannel channel, final int interestOps, final NioContext context) {
    ChannelWrapper<?> wrapper = null;
    try {
      Lock lock = server.getLock();
      lock.lock();
      try {
        server.getSelector().wakeup();
        if (channel.isBlocking()) channel.configureBlocking(false);
        SelectionKey key = channel.register(server.getSelector(), interestOps, context);
        wrapper = new SelectionKeyWrapper(key);
        context.setChannel(wrapper);
      } finally {
        lock.unlock();
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return wrapper;
  }

  /**
   * @param channel the channel to check.
   * @return true or false.
   */
  public boolean checkSubmitTransition(final ChannelWrapper<?> channel) {
    if (channel.isLocal()) return false;
    SSLHandler sslHandler = channel.getContext().getSSLHandler();
    if (sslHandler == null) return false;
    int keyOps = channel.getInterestOps();
    boolean b = (keyOps != channel.getReadyOps()) && (keyOps != 0) && !server.isIdle(channel) &&
        ((sslHandler.getApplicationReceiveBuffer().position() > 0) || (sslHandler.getChannelReceiveBuffer().position() > 0));
    return b;
  }

  /**
   * @param channel the channel to check.
   * @param transition the transition about to be performed on the channel.
   * @return true or false.
   */
  public boolean checkSubmitTransition(final ChannelWrapper<?> channel, final T transition) {
    SSLHandler sslHandler = channel.getContext().getSSLHandler();
    if (channel.isLocal() || (sslHandler == null)) return false;
    int interestOps = factory.getTransition(transition).getInterestOps();
    boolean b = (interestOps != channel.getReadyOps()) && (interestOps != 0) && !server.isIdle(channel) &&
        ((sslHandler.getApplicationReceiveBuffer().position() > 0) || (sslHandler.getChannelReceiveBuffer().position() > 0));
    return b;
  }
}
