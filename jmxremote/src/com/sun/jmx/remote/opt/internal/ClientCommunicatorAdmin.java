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
/*
 * @(#)ClientCommunicatorAdmin.java	1.3
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

import java.io.*;

import com.sun.jmx.remote.opt.util.*;

/**
 *
 */
public abstract class ClientCommunicatorAdmin {
  /**
   * 
   */
  private final Checker checker;
  /**
   * 
   */
  private long period;
  /**
   * 
   */
  private final static int CONNECTED = 0;
  /**
   * 
   */
  private final static int RE_CONNECTING = 1;
  /**
   * 
   */
  private final static int FAILED = 2;
  /**
   * 
   */
  private final static int TERMINATED = 3;
  /**
   * 
   */
  private int state = CONNECTED;
  /**
   * 
   */
  private final int[] lock = new int[0];
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "ClientCommunicatorAdmin");

  /**
   * Initialize this client admin.
   * @param period .
   */
  public ClientCommunicatorAdmin(final long period) {
    this.period = period;

    if (period > 0) {
      checker = new Checker();

      Thread t = new Thread(checker);
      t.setDaemon(true);
      t.start();
    } else checker = null;
  }

  /**
   * Called by a client to inform of getting an IOException.
   * @param ioe .
   * @throws IOException if any I/O error occurs.
   */
  public void gotIOException(final IOException ioe) throws IOException {
    restart(ioe);
  }

  /**
   * Called by this class to check a client connection.
   * @throws IOException if any I/O error occurs.
   */
  protected abstract void checkConnection() throws IOException;

  /**
   * Tells a client to re-start again.
   * @throws IOException if any I/O error occurs.
   */
  protected abstract void doStart() throws IOException;

  /**
   * Tells a client to stop becaue failing to call checkConnection.
   */
  protected abstract void doStop();

  /**
   * Terminates this object.
   */
  public void terminate() {
    synchronized (lock) {
      if (state == TERMINATED) {
        return;
      }

      state = TERMINATED;

      lock.notifyAll();

      if (checker != null) checker.stop();
    }
  }

  /**
   * 
   * @param ioe .
   * @throws IOException .
   */
  private void restart(final IOException ioe) throws IOException {
    // check state
    synchronized (lock) {
      if (state == TERMINATED) {
        throw new IOException("The client has been closed.");
      } else if (state == FAILED) { // already failed to re-start by another thread
        throw ioe;
      } else if (state == RE_CONNECTING) {
        // restart process has been called by another thread
        // we need to wait
        while (state == RE_CONNECTING) {
          try {
            lock.wait();
          } catch (InterruptedException ire) {
            // be asked to give up
            InterruptedIOException iioe = new InterruptedIOException(ire.toString());
            EnvHelp.initCause(iioe, ire);

            throw iioe;
          }
        }

        if (state == TERMINATED) {
          throw new IOException("The client has been closed.");
        } else if (state != CONNECTED) {
          // restarted is failed by another thread
          throw ioe;
        }
      } else {
        state = RE_CONNECTING;
        lock.notifyAll();
      }
    }

    // re-starting
    try {
      doStart();
      synchronized (lock) {
        if (state == TERMINATED) {
          throw new IOException("The client has been closed.");
        }

        state = CONNECTED;

        lock.notifyAll();
      }

      return;
    } catch (Exception e) {
      logger.warning("restart", "Failed to restart: " + e);
      logger.debug("restart", e);

      synchronized (lock) {
        if (state == TERMINATED) {
          throw new IOException("The client has been closed.");
        }

        state = FAILED;

        lock.notifyAll();
      }

      try {
        doStop();
      } catch (@SuppressWarnings("unused") Exception eee) {
        // OK.
        // We know there is a problem.
      }

      terminate();

      throw ioe;
    }
  }

  /**
   * 
   */
  private class Checker implements Runnable {
    /**
     * 
     */
    private Thread myThread;

    @Override
    public void run() {
      myThread = Thread.currentThread();
      while (state != TERMINATED && !myThread.isInterrupted()) {
        try {
          Thread.sleep(period);
        } catch (@SuppressWarnings("unused") InterruptedException ire) {
          // OK. We will check the state at the following steps
        }
        if (state == TERMINATED || myThread.isInterrupted()) break;
        try {
          checkConnection();
        } catch (Exception e) {
          synchronized (lock) {
            if (state == TERMINATED || myThread.isInterrupted()) break;
          }
          e = (Exception) EnvHelp.getCause(e);
          if (e instanceof IOException && !(e instanceof InterruptedIOException)) {
            try {
              restart((IOException) e);
            } catch (@SuppressWarnings("unused") Exception ee) {
              logger.warning("Checker-run", "Failed to check connection: " + e);
              logger.warning("Checker-run", "stopping");
              logger.debug("Checker-run", e);
              break;
            }
          } else {
            logger.warning("Checker-run", "Failed to check the connection: " + e);
            logger.debug("Checker-run", e);
            // XXX stop checking?
            break;
          }
        }
      }
      if (logger.traceOn()) logger.trace("Checker-run", "Finished.");
    }

    /**
     * 
     */
    private void stop() {
      if (myThread != null && myThread != Thread.currentThread()) myThread.interrupt();
    }
  }
}
