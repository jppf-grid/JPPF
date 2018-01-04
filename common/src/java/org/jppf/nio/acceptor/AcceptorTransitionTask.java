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

package org.jppf.nio.acceptor;

import java.nio.channels.SelectionKey;

import org.jppf.nio.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class perform the transition of a channel from one state to another.
 * They extend the Runnable interface so they can be executed concurrently by a pool of threads.
 * @author Laurent Cohen
 */
public class AcceptorTransitionTask implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AcceptorTransitionTask.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The channel whose state is changing.
   */
  private final ChannelWrapper<?> channel;
  /**
   * 
   */
  private final AcceptorState state;
  /**
   * 
   */
  private final NioState<AcceptorTransition> nioState;
  /**
   * The associated JMX context.
   */
  private final AcceptorContext ctx;
  /**
   * 
   */
  private final AcceptorNioServer server;

  /**
   * Initialize this task with the specified key and factory.
   * @param channel the channel whose state is changing.
   * @param state the operation to perform.
   * @param server the factory for the server that runs this task.
   */
  @SuppressWarnings("unchecked")
  public AcceptorTransitionTask(final ChannelWrapper<?> channel, final AcceptorState state, final AcceptorNioServer server) {
    this.channel = channel;
    this.state = state;
    this.server = server;
    this.nioState = server.getFactory().getState(state);
    this.ctx = (AcceptorContext) channel.getContext();
  }

  @Override
  public void run() {
    try {
      try {
        if (traceEnabled) log.trace("performing transition to state {} for {}", state, channel);
        synchronized(channel) {
          nioState.performTransition(channel);
        }
        final SelectionKey key = (SelectionKey) channel.getChannel();
        if (key.isValid()) server.updateInterestOps(key, SelectionKey.OP_READ, true);
      } catch (Exception|Error  e) {
        ctx.setEnabled(false);
        throw e;
      }
    } catch(Exception|Error e) {
      try {
        if (debugEnabled) log.debug("error on channel {} :\n{}", channel, ExceptionUtils.getStackTrace(e));
        else log.warn("error on channel {} : {}", channel, ExceptionUtils.getMessage(e));
      } catch (final Exception e2) {
        if (debugEnabled) log.debug("error on channel: {}", ExceptionUtils.getStackTrace(e2));
        else log.warn("error on channel: {}", ExceptionUtils.getMessage(e2));
      }
      if (e instanceof Exception) ctx.handleException(channel, (Exception) e);
      else throw (Error) e;
    }
  }
}
