/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
/*
 * @(#)ServerCommunicatorAdmin.java	1.3
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.internal;

import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * 
 */
public abstract class ServerCommunicatorAdmin {
  /**
   * Time of last activity on this connection.
   */
  private long timestamp;
  /**
   * 
   */
  private final int[] lock = new int[0];
  /**
   * 
   */
  private int currentJobs = 0;
  /**
   * Idle tiemout for the connection.
   */
  private long timeout;
  /**
   * 
   */
  private boolean terminated = false;
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "ServerCommunicatorAdmin");
  /**
   * 
   */
  private static final ClassLogger timelogger = new ClassLogger("javax.management.remote.timeout", "ServerCommunicatorAdmin");

  /**
   * 
   * @param timeout .
   */
  public ServerCommunicatorAdmin(final long timeout) {
    if (logger.traceOn()) logger.trace("Constructor", "Creates a new ServerCommunicatorAdmin object with the timeout " + timeout);
    this.timeout = timeout;
    timestamp = 0;
    if (timeout < Long.MAX_VALUE) {
      Runnable timeoutTask = new Timeout();
      final Thread t = new Thread(timeoutTask);
      t.setName("JMX server connection timeout " + t.getName());
      // If you change this name you will need to change a unit test (NoServerTimeoutTest)
      t.setDaemon(true);
      t.start();
    }
  }

  /**
   * Tells that a new request message is received. A caller of this method should always
   * call the method <code>rspOutgoing</code> to inform that a response is sent out for the received request.
   * @return the value of the termination flag: <code>true</code> if the connection is already being terminated,
   * <code>false</code> otherwise.
   */
  public boolean reqIncoming() {
    if (logger.traceOn())logger.trace("reqIncoming", "Receive a new request.");
    synchronized (lock) {
      if (terminated) logger.warning("reqIncoming", "The server has decided to close " + "this client connection.");
      ++currentJobs;
      return terminated;
    }
  }

  /**
   * Tells that a response is sent out for a received request.
   * @return the value of the termination flag: <code>true</code>
   * if the connection is already being terminated, <code>false</code> otherwise.
   */
  public boolean rspOutgoing() {
    if (logger.traceOn()) logger.trace("reqIncoming", "Finish a request.");
    synchronized (lock) {
      if (--currentJobs == 0) {
        timestamp = System.currentTimeMillis();
        logtime("Admin: Timestamp=", timestamp);
        // tells the adminor to restart waiting with timeout
        lock.notify();
      }
      return terminated;
    }
  }

  /**
   * Called by this class to tell an implementation to do stop.
   */
  protected abstract void doStop();

  /**
   * Terminates this object. Called only by outside, so do not need to call doStop
   */
  public void terminate() {
    if (logger.traceOn()) logger.trace("terminate", "terminate the ServerCommunicatorAdmin object.");
    synchronized (lock) {
      if (terminated) return;
      terminated = true;
      // tell Timeout to terminate
      lock.notify();
    }
  }

  /**
   * This task checks for the idle connection timeout and closes
   * the server-sidde connection when the tiemout is reached.
   */
  private class Timeout implements Runnable {
    @Override
    public void run() {
      boolean stopping = false;
      synchronized (lock) {
        if (timestamp == 0) timestamp = System.currentTimeMillis();
        logtime("Admin: timeout=", timeout);
        logtime("Admin: Timestamp=", timestamp);
        while (!terminated) {
          try {
            // wait until there is no more job
            while (!terminated && currentJobs != 0) {
              if (logger.traceOn()) logger.trace("Timeout-run", "Waiting without timeout.");
              lock.wait();
            }
            if (terminated) return;
            final long remaining = timeout - (System.currentTimeMillis() - timestamp);
            logtime("Admin: remaining timeout=", remaining);
            if (remaining > 0) {
              if (logger.traceOn()) logger.trace("Timeout-run", "Waiting with timeout: " + remaining + " ms remaining");
              lock.wait(remaining);
            }
            if (currentJobs > 0) continue;
            final long elapsed = System.currentTimeMillis() - timestamp;
            logtime("Admin: elapsed=", elapsed);
            if (!terminated && elapsed > timeout) {
              if (logger.traceOn()) logger.trace("Timeout-run", "timeout elapsed");
              logtime("Admin: timeout elapsed! " + elapsed + ">", timeout);
              // stopping
              terminated = true;
              stopping = true;
              break;
            }
          } catch (InterruptedException ire) {
            logger.warning("Timeout-run", "Unexpected Exception: " + ire);
            logger.debug("Timeout-run", ire);
            return;
          }
        }
      }
      if (stopping) {
        if (logger.traceOn()) logger.trace("Timeout-run", "Call the doStop.");
        doStop();
      }
    }
  }

  /**
   * 
   * @param desc .
   * @param time .
   */
  private void logtime(final String desc, final long time) {
    timelogger.trace("synchro", desc + time);
  }
}
