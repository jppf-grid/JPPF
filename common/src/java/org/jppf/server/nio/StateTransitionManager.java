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
public class StateTransitionManager<S extends Enum<S>, T extends Enum<T>>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(StateTransitionManager.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The pool of threads used for submitting channel state transitions.
   */
  protected ExecutorService executor = null;
  /**
   * The server for which this transition manager is intended.
   */
  private NioServer<S, T> server = null;

  /**
   * Initialize this transition manager with the specified server and sequential flag.
   * @param server the server for which this transition manager is intended.
   * performed sequentially or through the executor thread pool.
   */
  public StateTransitionManager(final NioServer<S, T> server)
  {
    this.server = server;
    int size = JPPFConfiguration.getProperties().getInt("transition.thread.pool.size", -1);
    if (size <= 0) size = Runtime.getRuntime().availableProcessors();
    executor = Executors.newFixedThreadPool(size, new JPPFThreadFactory(server.getName()));
  }

  /**
   * Submit the next state transition for a specified channel.
   * @param key the selection key that references the channel.
   */
  public void submitTransition(final ChannelWrapper<?> key)
  {
    if (debugEnabled) log.debug("submitting transition for " + key + ", state=" + key.getContext().getState());
    setKeyOps(key, 0);
    StateTransitionTask<S, T> transition = new StateTransitionTask<S, T>(key, server.getFactory());
    executor.submit(transition);
  }

  /**
   * Set the interest ops of a specified selection key, ensuring no blocking occurs while doing so.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param ops the operations to set on the key.
   */
  private void setKeyOps(final ChannelWrapper<?> key, final int ops)
  {
    Lock lock = server.getLock();
    //server.getSelector().wakeup();
    lock.lock();
    try
    {
      server.getSelector().wakeup();
      key.setKeyOps(ops);
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Transition the specified channel to the specified state.
   * @param channel the key holding the channel and associated context.
   * @param transition holds the new state of the channel and associated key ops.
   */
  @SuppressWarnings("unchecked")
  public void transitionChannel(final ChannelWrapper<?> channel, final T transition)
  {
    Lock lock = server.getLock();
    //server.getSelector().wakeup();
    lock.lock();
    try
    {
      server.getSelector().wakeup();
      NioContext<S> context = (NioContext<S>) channel.getContext();
      S s1 = context.getState();
      NioTransition<S> t = server.getFactory().getTransition(transition);
      S s2 = t.getState();
      context.setState(s2);
      channel.setKeyOps(t.getInterestOps());
      //setKeyOps(channel, t.getInterestOps());
      if (debugEnabled && (s1 != s2)) log.debug("transitioned " + channel + " from " + s1 + " to " + s2 + " with ops=" + t.getInterestOps());
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Transition the specified channel to the specified state.
   * @param channel the key holding the channel and associated context.
   * @param submit specifies whther the transition should be submitted immediately,
   * or if we should wait for the server to submit it.
   * @param transition holds the new state of the channel and associated key ops.
   */
  @SuppressWarnings("unchecked")
  public void transitionChannel(final ChannelWrapper<?> channel, final T transition, final boolean submit)
  {
    Lock lock = server.getLock();
    //server.getSelector().wakeup();
    lock.lock();
    try
    {
      server.getSelector().wakeup();
      NioContext<S> context = (NioContext<S>) channel.getContext();
      S s1 = context.getState();
      NioTransition<S> t = server.getFactory().getTransition(transition);
      S s2 = t.getState();
      context.setState(s2);
      if (!submit) channel.setKeyOps(t.getInterestOps());
      else
      {
        channel.setKeyOps(0);
        submitTransition(channel);
      }
      //setKeyOps(channel, t.getInterestOps());
      if (debugEnabled && (s1 != s2)) log.debug("transitioned " + channel + " from " + s1 + " to " + s2 + " with ops=" + t.getInterestOps());
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Register a channel not opened through this server, with intial interest operation set to 0.
   * @param channel the channel to register.
   * @param context the context attached to the channel.
   * @return a {@link ChannelWrapper} instance.
   */
  @SuppressWarnings("unchecked")
  public ChannelWrapper<?> registerChannel(final SocketChannel channel, final NioContext context)
  {
    return registerChannel(channel, 0, context);
  }

  /**
   * Register a channel not opened through this server.
   * @param channel the channel to register.
   * @param ops the operations the channel is initially interested in.
   * @param context the context attached to the channel.
   * @return a {@link ChannelWrapper} instance.
   */
  @SuppressWarnings("unchecked")
  private ChannelWrapper<?> registerChannel(final SocketChannel channel, final int ops, final NioContext context)
  {
    ChannelWrapper<?> wrapper = null;
    try
    {
      Lock lock = server.getLock();
      lock.lock();
      try
      {
        server.getSelector().wakeup();
        if (channel.isBlocking()) channel.configureBlocking(false);
        SelectionKey key = channel.register(server.getSelector(), ops, context);
        wrapper = new SelectionKeyWrapper(key);
        context.setChannel(wrapper);
      }
      finally
      {
        lock.unlock();
      }
    }
    catch (ClosedChannelException e)
    {
      log.error(e.getMessage(), e);
    }
    catch (IOException e)
    {
      log.error(e.getMessage(), e);
    }
    return wrapper;
  }

  /**
   * @param channel the channel to check.
   * @return true or false.
   */
  public boolean checkSubmitTransition(final ChannelWrapper<?> channel)
  {
    /*
    return checkShouldSubmitTransition(channel, null);
     */
    if (channel.isLocal()) return false;
    SSLEngineManager sslEngineManager = channel.getContext().getSSLEngineManager();
    if (sslEngineManager == null) return false;
    int keyOps = channel.getKeyOps();
    boolean b = (keyOps != channel.getReadyOps()) && (keyOps != 0) && !server.isIdle(channel) &&
        ((sslEngineManager.getAppRecvBuffer().position() > 0) || (sslEngineManager.getNetRecvBuffer().position() > 0));
    return b;
  }

  /**
   * @param channel the channel to check.
   * @param transition the transition about to be performed on the channel.
   * @return true or false.
   */
  public boolean checkSubmitTransition(final ChannelWrapper<?> channel, final T transition)
  {
    /*
    return false;
     */
    if (channel.isLocal()) return false;
    SSLEngineManager sslEngineManager = channel.getContext().getSSLEngineManager();
    if (sslEngineManager == null) return false;
    int keyOps = server.getFactory().getTransition(transition).getInterestOps();
    boolean b = (keyOps != channel.getReadyOps()) && (keyOps != 0) && !server.isIdle(channel) &&
        ((sslEngineManager.getAppRecvBuffer().position() > 0) || (sslEngineManager.getNetRecvBuffer().position() > 0));
    return b;
  }
}
