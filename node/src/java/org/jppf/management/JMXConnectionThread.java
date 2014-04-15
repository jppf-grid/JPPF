/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * This class is intended to be used as a thread that attempts to (re-)connect to
 * the management server.
 * @exclude
 */
public class JMXConnectionThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JMXConnectionThread.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines the suspended state of this connection thread.
   */
  private boolean suspended = false;
  /**
   * Determines the connecting state of this connection thread.
   */
  private boolean connecting = true;
  /**
   * The connection that holds this thread.
   */
  private JMXConnectionWrapper connectionWrapper = null;

  /**
   * Initialize this thread with the specified connection.
   * @param connectionWrapper the connection that holds this thread.
   */
  public JMXConnectionThread(final JMXConnectionWrapper connectionWrapper) {
    this.connectionWrapper = connectionWrapper;
  }

  /**
   * 
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    while (!isStopped()) {
      if (isSuspended()) {
        if (debugEnabled) log.debug(connectionWrapper.getId() + " about to go to sleep");
        goToSleep();
        continue;
      }
      if (isConnecting()) {
        try {
          if (debugEnabled) log.debug(connectionWrapper.getId() + " about to perform connection attempts");
          connectionWrapper.performConnection();
          if (debugEnabled) log.debug(connectionWrapper.getId() + " about to suspend connection attempts");
          suspend();
          connectionWrapper.wakeUp();
        } catch(Exception ignored) {
          if (debugEnabled) log.debug(connectionWrapper.getId()+ " JMX URL = " + connectionWrapper.getURL(), ignored);
          try {
            Thread.sleep(100);
          } catch(InterruptedException e) {
            log.error(e.getMessage(), e);
          }
        } finally {
          connectionWrapper.wakeUp();
        }
      }
    }
  }

  /**
   * Suspend the current thread.
   */
  public synchronized void suspend() {
    if (debugEnabled) log.debug(connectionWrapper.getId() + " suspending connection attempts");
    setConnecting(false);
    setSuspended(true);
    wakeUp();
  }

  /**
   * Resume the current thread's execution.
   */
  public synchronized void resume() {
    if (debugEnabled) log.debug(connectionWrapper.getId() + " resuming connection attempts");
    setConnecting(true);
    setSuspended(false);
    wakeUp();
  }

  /**
   * Stop this thread.
   */
  public synchronized void close() {
    setConnecting(false);
    setStopped(true);
    wakeUp();
  }

  /**
   * Get the connecting state of this connection thread.
   * @return true if the connection is established, false otherwise.
   */
  public synchronized boolean isConnecting() {
    return connecting;
  }

  /**
   * Get the connecting state of this connection thread.
   * @param connecting true if the connection is established, false otherwise.
   */
  public synchronized void setConnecting(final boolean connecting) {
    this.connecting = connecting;
  }

  /**
   * Determines the suspended state of this connection thread.
   * @return true if the thread is suspended, false otherwise.
   */
  public synchronized boolean isSuspended() {
    return suspended;
  }

  /**
   * Set the suspended state of this connection thread.
   * @param suspended true if the connection is suspended, false otherwise.
   */
  public synchronized void setSuspended(final boolean suspended) {
    this.suspended = suspended;
  }
}
