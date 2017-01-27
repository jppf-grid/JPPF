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
 * @(#)file      ServerSynchroMessageConnectionImpl.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.5
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
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

package com.sun.jmx.remote.generic;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.*;
import javax.security.auth.Subject;

import com.sun.jmx.remote.opt.util.*;

/**
 * 
 */
public class ServerSynchroMessageConnectionImpl implements ServerSynchroMessageConnection {
  /**
   * This lock used to ensures no concurrent writes
   */
  /**
   * 
   */
  private transient int[] connectionLock = new int[0];
  /**
   * 
   */
  private transient MessageConnection connection;
  /**
   * 
   */
  private transient ServerAdmin serverAdmin = null;
  /**
   * 
   */
  private Map<String, ?> env;
  /**
   * 
   */
  private transient SynchroCallback callback;
  /**
   * 
   */
  private transient MessageReader reader;
  // state issues
  /**
   * 
   */
  private static final int UNCONNECTED = 1;
  /**
   * 
   */
  private static final int CONNECTING = 2;
  /**
   * 
   */
  private static final int CONNECTED = 3;
  /**
   * 
   */
  private static final int FAILED = 4;
  /**
   * 
   */
  private static final int TERMINATED = 5;
  /**
   * 
   */
  private int state = UNCONNECTED;
  /**
   * Used to control access to the state variable, including ensuring that only one thread manages state transitions.
   */
  private int[] stateLock = new int[0];
  /**
   * 
   */
  private long waitConnectedState;
  /**
   * 
   */
  private final ClassLogger logger = new ClassLogger("com.sun.jmx.remote.generic", "ServerSynchroMessageConnectionImpl");

  /**
   * 
   * @param mc .
   * @param env .
   * @throws IOException .
   */
  public ServerSynchroMessageConnectionImpl(final MessageConnection mc, final Map<String, ?> env) throws IOException {
    if (mc == null) throw new IllegalArgumentException("Null message connection.");
    this.env = env;
    waitConnectedState = DefaultConfig.getTimeoutForWaitConnectedState(env);
    this.serverAdmin = DefaultConfig.getServerAdmin(this.env);
    connection = mc;
  }

  @Override
  public void connect(final Map<String, ?> env) throws IOException {
    synchronized (stateLock) {
      if (state != UNCONNECTED) {
        waitConnected();
        return;
      } else state = CONNECTING;
    }
    connection.connect(env);
    connection = serverAdmin.connectionOpen(connection);
    synchronized (stateLock) {
      if (state != CONNECTING) {
        // closed by another thread
        try {
          connection.close();
        } catch (@SuppressWarnings("unused") Exception e) {
          // OK Already closed.
        }
        throw new IOException("The connecting is stooped by another thread.");
      }
      state = CONNECTED;
      stateLock.notifyAll();
    }
  }

  @Override
  public void sendOneWay(final Message msg) throws IOException {
    if (logger.traceOn()) logger.trace("sendOneWay", "Send a message without response.");
    waitConnected();
    synchronized (connectionLock) {
      connection.writeMessage(msg);
    }
  }

  @Override
  public void setCallback(final SynchroCallback cb) {
    if (logger.traceOn()) logger.trace("setCallback", "" + cb + " be called.");
    if (callback != null) throw new IllegalArgumentException("The callback has been assigned.");
    if (cb == null) throw new IllegalArgumentException("Null callback.");
    callback = cb;
    reader = new MessageReader();
    ThreadService.getShared().handoff(reader);
  }

  @Override
  public String getConnectionId() {
    return connection.getConnectionId();
  }

  @Override
  public void close() throws IOException {
    if (logger.traceOn()) logger.trace("close", "Closing this SynchroMessageConnection.");
    synchronized (stateLock) {
      if (state == TERMINATED) return;
      state = TERMINATED;
      if (logger.traceOn()) logger.trace("close", "Close the callback reader.");
      if (reader != null) reader.stop();
      if (logger.traceOn()) logger.trace("close", "Closing the underlying connection.");
      if (connection != null) connection.close();
      serverAdmin.connectionClosed(connection);
      // clean
      if (logger.traceOn()) logger.trace("close", "Clean all threads waiting theire responses.");
      stateLock.notify();
    }
  }

  /**
   * Returns the underlying asynchronous trasport.
   * @return .
   */
  public MessageConnection getAsynchroConnection() {
    return connection;
  }

  //----------------------------------------------
  // private classes
  //----------------------------------------------

  /**
   * 
   */
  private class MessageReader implements Runnable {
    /**
     * 
     */
    private Thread executingThread;
    /**
     * This flag is used to ensure that we interrupt the executingThread only when it is running in this MessageReader object.
     */
    private boolean executingThreadInterrupted = false;

    /**
     * 
     */
    public MessageReader() {
    }

    @Override
    public void run() {
      try {
        executingThread = Thread.currentThread();
        Message msg;
        while (!stopped()) {
          if (logger.traceOn()) logger.trace("MessageReader.run", "Waiting a coming message...");
          msg = null;
          try {
            msg = connection.readMessage();
          } catch (Exception e) {
            if (stopped()) break;
            callback.connectionException(e);
            // if reconnected, a new reader should be created.
            break;
          }
          if (stopped()) break;
          if (logger.traceOn()) logger.trace("MessageReader.run", "handing message off...");
          ThreadService.getShared().handoff(new RemoteJob(msg));
          if (msg instanceof CloseMessage) break;
        }
      } catch (@SuppressWarnings("unused") Exception eee) {
        // need to stop
        if (logger.traceOn()) logger.trace("MessageReader.run", "stops.");
      }
      synchronized (stateLock) {
        executingThreadInterrupted = true;
      }
      if (logger.traceOn()) logger.trace("MessageReader.run", "ended.");
    }

    /**
     * 
     */
    public void stop() {
      if (logger.traceOn()) logger.trace("MessageReader.stop", "be called.");
      synchronized (stateLock) {
        if (Thread.currentThread() != executingThread && executingThread != null && !executingThreadInterrupted) {
          executingThreadInterrupted = true;
          executingThread.interrupt();
        }
      }
      if (logger.traceOn()) logger.trace("MessageReader.stop", "done.");
    }

    /**
     * 
     * @return .
     */
    private boolean stopped() {
      synchronized (stateLock) {
        return (state != CONNECTED || executingThreadInterrupted);
      }
    }
  }

  /**
   * 
   */
  private class RemoteJob implements Runnable {
    /**
     * 
     */
    private Message msg;

    /**
     * 
     * @param msg .
     */
    public RemoteJob(final Message msg) {
      this.msg = msg;
    }

    @Override
    public void run() {
      if (logger.traceOn()) logger.trace("RemoteJob.run", "Receive a new request.");
      try {
        Message resp = callback.execute(msg);
        if (resp != null) {
          synchronized (connectionLock) {
            connection.writeMessage(resp);
          }
        }
      } catch (Exception ie) {
        synchronized (stateLock) {
          if (state != CONNECTED && callback != null) {
            // inform the callback
            callback.connectionException(ie);
          }
        }
      }
    }
  }

  @Override
  public Subject getSubject() {
    return serverAdmin.getSubject(connection);
  }

  /**
   * 
   * @throws IOException .
   */
  private void waitConnected() throws IOException {
    synchronized (stateLock) {
      if (state == CONNECTED) return;
      else if (state != CONNECTING) throw new IOException("The connection was closed or failed.");
      final long startTime = System.nanoTime();
      long remainingTime = waitConnectedState;
      while (state == CONNECTING && waitConnectedState > 0) {
        try {
          stateLock.wait(remainingTime);
        } catch (@SuppressWarnings("unused") InterruptedException ire) {
          break;
        }
        remainingTime = waitConnectedState - ((System.nanoTime() - startTime) / 1_000_000L);
      }
      if (state != CONNECTED) throw new IOException("The connection is not connected.");
      else return;
    }
  }
}
