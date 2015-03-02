/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
  protected final ExecutorService executor;
  /**
   * The server for which this transition manager is intended.
   */
  private final NioServer<S, T> server;
  /**
   * The state and transition factory associated with the server.
   */
  private final NioServerFactory<S, T> factory;
  /**
   * Determines whether the channels handled by the server are node job channels.
   */
  private final boolean isNodeServer;
  /**
   * Global thread pool used by all NIO servers.
   * @since 5.0
   */
  private static ExecutorService globalExecutor;
  /**
   * The server lock.
   * @since 5.0
   */
  private final Lock lock;

  /**
   * Initialize this transition manager with the specified server and sequential flag.
   * @param server the server for which this transition manager is intended.
   * performed sequentially or through the executor thread pool.
   */
  public StateTransitionManager(final NioServer<S, T> server) {
    this.server = server;
    this.factory = server.getFactory();
    this.lock = server.getLock();
    executor = initExecutor();
    isNodeServer = server.getIdentifier() == JPPFIdentifiers.NODE_JOB_DATA_CHANNEL;
  }

  /**
   * Submit the sdpecified runnable to the executor.
   * @param runnable the runnable to submit.
   */
  public void submit(final Runnable runnable) {
    executor.submit(runnable);
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
    executor.submit(new StateTransitionTask<>(channel, factory));
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
      server.getSelector().wakeup();
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
    lock.lock();
    try {
      server.getSelector().wakeup();
      synchronized(channel) {
        channel.setInterestOps(0);
        NioContext<S> context = (NioContext<S>) channel.getContext();
        S s1 = context.getState();
        NioTransition<S> t = factory.getTransition(transition);
        S s2 = t.getState();
        if (s1 != null) {
          ///if (!factory.isTransitionAllowed(s1, s2)) log.warn("unauthorized transition" + getTransitionMessage(s1, s2, t, channel));
          if (debugEnabled && (s1 != s2)) log.debug("transition" + getTransitionMessage(s1, s2, t, channel, submit));
          //if (debugEnabled && isNodeServer) log.debug("transition" + getTransitionMessage(s1, s2, t, channel, submit));
          else if (traceEnabled) log.trace(getTransitionMessage(s1, s2, t, channel, submit));
        }
        if (context.setState(s2)) {
          if (!submit) {
            channel.setInterestOps(t.getInterestOps());
            if (traceEnabled) log.trace("set interestOps={} for channel id={}", t.getInterestOps(), channel.getId());
          } else {
            submitTransition(channel);
            if (traceEnabled) log.trace("submitted transition={} for channel id={}", t, channel.getId());
          }
        }
      }
    } catch (RuntimeException e) {
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
      lock.lock();
      try {
        if (channel.isBlocking()) channel.configureBlocking(false);
        SelectionKey key = channel.register(server.getSelector().wakeup(), interestOps, context);
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
    int interestOps = channel.getInterestOps();
    boolean b = (interestOps != channel.getReadyOps()) && (interestOps != 0) && !server.isIdle(channel) &&
        ((sslHandler.getApplicationReceiveBuffer().position() > 0) || (sslHandler.getChannelReceiveBuffer().position() > 0));
    return b;
  }

  /**
   * @param channel the channel to check.
   * @param transition the transition about to be performed on the channel.
   * @return true or false.
   */
  public boolean checkSubmitTransition2(final ChannelWrapper<?> channel, final T transition) {
    SSLHandler sslHandler = channel.getContext().getSSLHandler();
    if (channel.isLocal() || (sslHandler == null)) return false;
    int interestOps = factory.getTransition(transition).getInterestOps();
    boolean b = (interestOps != channel.getReadyOps()) && (interestOps != 0) && !server.isIdle(channel) &&
        ((sslHandler.getApplicationReceiveBuffer().position() > 0) || (sslHandler.getChannelReceiveBuffer().position() > 0));
    return b;
  }

  /**
   * @param channel the channel to check.
   * @param transition the transition about to be performed on the channel.
   * @return true or false.
   */
  public boolean checkSubmitTransition(final ChannelWrapper<?> channel, final T transition) {
    if (channel.isLocal()) return false;
    int interestOps = factory.getTransition(transition).getInterestOps();
    int readyOps = channel.getReadyOps();
    // TODO: investigate why this is necessary, why when stress-testing offline nodes, one of the node channels
    // gets stuck with readyOps=4 and interestOps=5 (for state SENDING_BUNDLE) but the selector doesn't select the corresponding key.
    if (isNodeServer && ((interestOps & OP_WRITE) != 0) && ((readyOps & OP_WRITE) != 0)) return true;
    SSLHandler sslHandler = channel.getContext().getSSLHandler();
    if (sslHandler == null) return false;
    boolean b = (interestOps != readyOps) && (interestOps != 0) && !server.isIdle(channel) &&
        ((sslHandler.getApplicationReceiveBuffer().position() > 0) || (sslHandler.getChannelReceiveBuffer().position() > 0));
    return b;
  }

  /**
   * Initialize the executor for this transition manager.
   * @return an {@link ExecutorService} object.
   * @since 5.0
   */
  private static synchronized ExecutorService initExecutor() {
    if (globalExecutor == null) {
      int n = NioConstants.THREAD_POOL_SIZE;
      globalExecutor = Executors.newFixedThreadPool(n, new JPPFThreadFactory("JPPF NIO"));
      log.info("globalExecutor={}, maxSize={}", globalExecutor, ((ThreadPoolExecutor) globalExecutor).getMaximumPoolSize());
    }
    return globalExecutor;
  }
}
