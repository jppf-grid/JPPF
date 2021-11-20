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

package org.jppf.management;

import org.jppf.utils.concurrent.ThreadSynchronization;
import org.slf4j.*;

/**
 * This class is intended to be used as a thread that attempts to (re-)connect to
 * the management server.
 */
class JMXConnectionThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JMXConnectionThread.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The connection that holds this thread.
   */
  private final JMXConnectionWrapper connectionWrapper;
  /**
   * The thread running this object.
   */
  private Thread thread;
  /**
   * How long to sleep between 2 connection attemps.
   */
  private final long sleepTime;

  /**
   * Initialize this thread with the specified connection.
   * @param connectionWrapper the connection that holds this thread.
   */
  public JMXConnectionThread(final JMXConnectionWrapper connectionWrapper) {
    this(connectionWrapper, 50L);
  }

  /**
   * Initialize this thread with the specified connection.
   * @param connectionWrapper the connection that holds this thread.
   * @param sleepTime how long to sleep between 2 connection attemps.
   */
  public JMXConnectionThread(final JMXConnectionWrapper connectionWrapper, final long sleepTime) {
    this.connectionWrapper = connectionWrapper;
    this.sleepTime = sleepTime;
  }

  @Override
  public void run() {
    int n = 0;
      try {
        synchronized(this) {
          this.thread = Thread.currentThread();
        }
        while (!isStopped() && !connectionWrapper.closed.get()) {
          try {
            if (debugEnabled) log.debug(connectionWrapper.getId() + " about to perform connection attempt #" + (++n));
            connectionWrapper.performConnection();
            if (debugEnabled) log.debug(connectionWrapper.getId() + " about to suspend connection attempt #" + n);
          } catch(final Exception e) {
            if (debugEnabled) log.debug(connectionWrapper.getId()+ " JMX URL = " + connectionWrapper.getURL(), e);
            connectionWrapper.lastConnectionException = e;
            goToSleep(sleepTime);
          }
        }
      } finally {
        synchronized(this) {
          this.thread = null;
        }
      }
  }

  /**
   * Stop this thread.
   */
  public synchronized void close() {
    setStopped(true);
    synchronized(this) {
      if (this.thread != null) {
        this.thread.interrupt();
      }
    }
  }
}
