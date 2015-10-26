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
package org.jppf.comm.socket;

import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Instances of this class attempt to connect a {@link org.jppf.comm.socket.SocketWrapper SocketWrapper} to a remote server.
 * The connection attempts are performed until a configurable amount of time has passed, and at a configurable time interval.
 * When no attempt succeeded, a <code>JPPFError</code> is thrown, and the application should normally exit.
 * @author Laurent Cohen
 */
public class SocketInitializerImpl extends AbstractSocketInitializer
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SocketInitializerImpl.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean traceEnabled = log.isTraceEnabled();

  /**
   * Instantiate this SocketInitializer with a specified socket wrapper.
   */
  public SocketInitializerImpl() {
  }

  /**
   * Instantiate this SocketInitializer with a specified socket wrapper.
   * @param name the name given to this <code>SocketInitializer</code>, for tracing purposes.
   */
  public SocketInitializerImpl(final String name) {
    this.name = name;
  }

  /**
   * Initialize the underlying socket client, by starting a <code>Timer</code> and a corresponding
   * <code>TimerTask</code> until a specified amount of time has passed.
   * @param socketWrapper the socket wrapper to initialize.
   */
  @Override
  public void initializeSocket(final SocketWrapper socketWrapper) {
    if (closed) return;
    if ("".equals(name)) name = getClass().getSimpleName() + '[' + socketWrapper.getHost() + ':' + socketWrapper.getPort() + ']';
    try {
      if (debugEnabled) log.debug("{} about to close socket wrapper", name);
      socketWrapper.close();
    } catch(Exception e) {
    }
    long delay = 1000L * JPPFConfiguration.get(JPPFProperties.RECONNECT_INITIAL_DELAY);
    if (delay <= 0L) delay = 1L + rand.nextInt(10);
    long maxTime = JPPFConfiguration.get(JPPFProperties.RECONNECT_MAX_TIME);
    long maxDuration = (maxTime <= 0) ? Long.MAX_VALUE : 1000L * maxTime;
    long period = 1000L * JPPFConfiguration.get(JPPFProperties.RECONNECT_INTERVAL);
    goToSleep(delay);
    successful = false;
    long elapsed = 0L;
    long start = System.nanoTime();
    while ((elapsed < maxDuration) && !successful && !closed) {
      try {
        if (traceEnabled) log.trace("{} opening the socket connection", name);
        socketWrapper.open();
        successful = true;
        if (traceEnabled) log.trace("{} socket connection successfully opened", name);
      } catch(Exception e) {
        if (traceEnabled) log.trace("{} socket connection open failed: {}", name, ExceptionUtils.getMessage(e));
      }
      if (!successful && !closed) goToSleep(period);
      elapsed = (System.nanoTime() - start) / 1_000_000L;
    }
  }

  @Override
  public void close() {
    if (!closed) {
      if (debugEnabled) log.debug("{} closing socket initializer", name);
      closed = true;
      wakeUp();
    }
  }
}
