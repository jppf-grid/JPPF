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

package org.jppf.jmxremote.nio;

import java.nio.channels.SelectionKey;

import org.jppf.client.Operator;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.SynchronizedInteger;
import org.slf4j.*;

/**
 * Instances of this class perform the transition of a channel from one state to another.
 * They extend the Runnable interface so they can be executed concurrently by a pool of threads.
 * @author Laurent Cohen
 */
public class JMXTransitionTask implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXTransitionTask.class);
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
  private final JMXContext context;
  /**
   * The associated nio srver.
   */
  private final JMXNioServer server;
  /**
   * 
   */
  private final SynchronizedInteger requestCount = new SynchronizedInteger();
  /**
   * 
   */
  private final boolean selecting;

  /**
   * Initialize this task with the specified key and factory.
   * @param channel the channel whose state is changing.
   * @param server the NIO server.
   * @param selecting whether the selector is selecting.
   */
  public JMXTransitionTask(final JMXContext channel, final JMXNioServer server, final boolean selecting) {
    this.context = channel;
    this.server = server;
    this.selecting = selecting;
  }

  @Override
  public void run() {
    if (!requestCount.compareAndSet(0, 1)) return;
    try {
      try {
        if (traceEnabled) log.trace("writing to channel {}", context);
        boolean result = false;
        synchronized(context) {
          while (requestCount.get() > 0) {
            try {
              result = JMXMessageWriter.write(context);
            } finally {
              requestCount.decrementAndGet();
            }
          }
        }
        if (result) {
          if (selecting) server.updateInterestOps(context.getSelectionKey(), SelectionKey.OP_WRITE, true);
          else JMXNioServer.updateInterestOpsNoWakeup(context.getSelectionKey(), SelectionKey.OP_WRITE, true);
        }
      } catch (Exception|Error  e) {
        context.setEnabled(false);
        throw e;
      }
    } catch(Exception|Error e) {
      try {
        if (debugEnabled) log.debug("error on channel {} :\n{}", context, ExceptionUtils.getStackTrace(e));
        else log.warn("error on channel {} : {}", context, ExceptionUtils.getMessage(e));
      } catch (final Exception e2) {
        if (debugEnabled) log.debug("error on channel: {}", ExceptionUtils.getStackTrace(e2));
        else log.warn("error on channel: {}", ExceptionUtils.getMessage(e2));
      }
      if (e instanceof Exception) context.handleException(null, (Exception) e);
      else throw (Error) e;
    }
  }

  /**
   * 
   * @return .
   */
  public boolean incrementCountIfNeeded() {
    return requestCount.compareAndIncrement(Operator.MORE_THAN, 0);
  }
}
