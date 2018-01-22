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
package org.jppf.comm.socket;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class attempt to connect a {@link org.jppf.comm.socket.SocketWrapper SocketWrapper} to a remote server.
 * The connection attempts are performed until a configurable amount of time has passed, and at a configurable time interval.
 * When no attempt succeeded, a <code>JPPFError</code> is thrown, and the application should normally exit.
 * @author Laurent Cohen
 */
public class SocketInitializerImpl extends ThreadSynchronization implements SocketInitializer {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SocketInitializerImpl.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The configuration to use.
   */
  private final TypedProperties config;
  /**
   * The last captured exception.
   */
  private Exception lastException;
  /**
   * Determine whether this socket initializer has been intentionally closed.
   */
  private boolean closed;
  /**
   * Name given to this initializer.
   */
  private String name;

  /**
   * Instantiate this SocketInitializer with a specified socket wrapper.
   */
  public SocketInitializerImpl() {
    this.config = JPPFConfiguration.getProperties();
  }

  /**
   * Instantiate this SocketInitializer with a specified socket wrapper.
   * @param config the configuration to use.
   */
  public SocketInitializerImpl(final TypedProperties config) {
    this.config = config;
  }

  @Override
  public boolean isClosed() {
    synchronized(this) {
      return closed;
    }
  }

  @Override
  public boolean initialize(final SocketWrapper socketWrapper) {
    boolean successful = false;
    if (isClosed()) return false;
    name = getClass().getSimpleName() + '[' + socketWrapper.getHost() + ':' + socketWrapper.getPort() + ']';
    if (socketWrapper.isOpened()) {
      try {
        if (debugEnabled) log.debug("{} about to close socket wrapper", name);
        socketWrapper.close();
      } catch(@SuppressWarnings("unused") final Exception e) {
      }
    }
    final long delay = 1000L * config.get(JPPFProperties.RECONNECT_INITIAL_DELAY);
    final long maxTime = config.get(JPPFProperties.RECONNECT_MAX_TIME);
    final long maxDuration = (maxTime <= 0) ? Long.MAX_VALUE : 1000L * maxTime;
    long period = 1000L * config.get(JPPFProperties.RECONNECT_INTERVAL);
    if (period <= 0L) period = 1000L;
    if (delay > 0L) goToSleep(delay);
    final long start = System.nanoTime();
    while (((System.nanoTime() - start) / 1_000_000L < maxDuration) && !successful && !isClosed()) {
      try {
        if (traceEnabled) log.trace("{} opening the socket connection", name);
        socketWrapper.open();
        successful = true;
        if (traceEnabled) log.trace("{} socket connection successfully opened", name);
      } catch(final Exception e) {
        if (traceEnabled) log.trace("{} socket connection open failed: {}", name, ExceptionUtils.getMessage(e));
        lastException = e;
        if (!isClosed()) goToSleep(period);
      }
    }
    return successful;
  }

  @Override
  public void close() {
    synchronized(this) {
      if (!closed) {
        if (debugEnabled) log.debug("{} closing socket initializer", name);
        closed = true;
        wakeUp();
      }
    }
  }

  @Override
  public Exception getLastException() {
    return lastException;
  }
}
