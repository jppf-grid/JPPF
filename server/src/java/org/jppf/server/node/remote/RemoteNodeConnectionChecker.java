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

package org.jppf.server.node.remote;

import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.server.node.AbstractNodeConnectionChecker;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Connection checker implementation for a remote node.
 * @author Laurent Cohen
 * @exclude
 */
public class RemoteNodeConnectionChecker extends AbstractNodeConnectionChecker {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(RemoteNodeConnectionChecker.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The node for which to check the connection.
   */
  private final AbstractRemoteNode node;
  /**
   * The socket connection.
   */
  private final SocketWrapper socketWrapper;
  /**
   * The thread which performs the checks.
   */
  private CheckerThread checkerThread = null;
  /**
   * 
   */
  private final ThreadSynchronization suspendedLock = new ThreadSynchronization() {};

  /**
   * Initialize this checker <ith the specified node.
   * @param node the node for which to check the connection.
   */
  public RemoteNodeConnectionChecker(final AbstractRemoteNode node) {
    this.node = node;
    this.socketWrapper = ((RemoteNodeConnection) node.getNodeConnection()).getChannel();
  }

  @Override
  public void start() {
    stopped.set(false);
    suspended.set(true);
    checkerThread = new CheckerThread();
    new Thread(checkerThread, "NodeConnectionChecker").start();
  }

  @Override
  public void stop() {
    stopped.set(true);
    checkerThread.setStopped(true);
    checkerThread.wakeUp();
  }

  @Override
  public void resume() {
    suspended.set(false);
    checkerThread.suspended.set(false);
    checkerThread.wakeUp();
  }

  @Override
  public void suspend() {
    suspended.set(true);
    checkerThread.wakeUp();
    waitSuspended();
  }

  /**
   * Wait until the checks are effectively suspended.
   */
  private void waitSuspended() {
    while (!checkerThread.suspended.get()) {
      long start = System.nanoTime();
      suspendedLock.goToSleep();
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
      if (debugEnabled) log.debug("suspended time: " + elapsed);
    }
  }

  /**
   * 
   */
  private class CheckerThread extends ThreadSynchronization implements Runnable {
    /**
     * 
     */
    private final AtomicBoolean suspended = new AtomicBoolean(true);

    @Override
    public void run() {
      while (!isStopped()) {
        if (isSuspended()) {
          if (this.suspended.compareAndSet(false, true)) suspendedLock.wakeUp();
          goToSleep();
        }
        if (isStopped()) return;
        if (isSuspended()) continue;
        long start = System.nanoTime();
        try {
          socketWrapper.receiveBytes(1);
        } catch (SocketTimeoutException ignore) {
          double elapsed = (System.nanoTime() - start) / 1e6d;
          if (debugEnabled) log.debug("receive time: " + elapsed);
        } catch (Exception e) {
          exception = e;
          node.getExecutionManager().cancelAllTasks(false, false);
        }
      }
    }
  }
}
