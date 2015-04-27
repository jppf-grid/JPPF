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
/*
 * @(#)file      ClientSynchroMessageConnectionImpl.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.29
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

import java.io.*;
import java.util.*;

import javax.management.remote.generic.*;
import javax.management.remote.message.*;
import javax.security.auth.Subject;

import com.sun.jmx.remote.opt.util.*;

/** */
public class ClientSynchroMessageConnectionImpl implements ClientSynchroMessageConnection {
  /** */
  private static final int UNCONNECTED = 1;
  /** */
  private static final int CONNECTING = 2;
  /** */
  private static final int CONNECTED = 3;
  /** */
  private static final int FAILED = 4;
  /** */
  private static final int TERMINATED = 5;
  /** */
  private int state = UNCONNECTED;
  /**
   * This lock used to ensures no concurrent writes
   */
  private transient int[] connectionLock = new int[0];
  /** */
  private transient MessageConnection connection;
  /** */
  private transient SynchroCallback callback;
  /** */
  private transient ClientAdmin clientAdmin = null;
  /** */
  private transient ServerAdmin serverAdmin = null;
  /** */
  private transient Subject subject = null;
  /** */
  private Map<String, ?> env;
  /** */
  private transient MessageReader reader;
  /** */
  private transient long wtimeout;
  /**
   * Maps message id to ResponseMsgWrapper, locked at itself when the map is updated. A ResponseMsgWrapper is used to wait for response for given request. Sychronizing on it to do wait/notify
   */
  private transient HashMap<Long, ResponseMsgWrapper> waitingList = new HashMap<>();
  /**
   * notif stuff.
   */
  private transient Message notifResp = null;
  /**
   * Controls access to notifResp field; used in wait/notify so waiting thread can be informed when a notifResp is set.
   */
  private transient final int[] notifLock = new int[0];
  /**
   * Used to control access to the state variable, including ensuring that only one thread manages state transitions.
   */
  private int[] stateLock = new int[0];
  /** */
  private long waitConnectedState;
  /** */
  private final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "SynchroMessageConnectionImpl");

  /**
   * 
   * @param mc .
   * @param cb .
   * @param env .
   */
  public ClientSynchroMessageConnectionImpl(final MessageConnection mc, final SynchroCallback cb, final Map<String, ?> env) {
    if (mc == null) throw new IllegalArgumentException("Null message connection.");
    if (cb == null) throw new IllegalArgumentException("Null SynchroCallback object.");
    connection = mc;
    callback = cb;
    this.env = env;
  }

  @Override
  public void connect(final Map<String, ?> env) throws IOException {
    synchronized (stateLock) {
      if (state == UNCONNECTED) {
        if (logger.traceOn()) logger.trace("connect", "Establishing the connection.");
        // first time to connect, need to merge env parameters with the one passed to the constructor.
        Map<String, Object> newEnv = new HashMap<>();
        if (this.env != null) newEnv.putAll(this.env);
        if (env != null) newEnv.putAll(env);
        wtimeout = DefaultConfig.getRequestTimeout(newEnv);
        waitConnectedState = DefaultConfig.getTimeoutForWaitConnectedState(newEnv);
        clientAdmin = DefaultConfig.getClientAdmin(newEnv);
        state = CONNECTING;
        stateLock.notifyAll();
        connection.connect(newEnv);
        connection = clientAdmin.connectionOpen(connection);
        this.env = newEnv;
        reader = new MessageReader();
        ThreadService.getShared().handoff(reader);
        state = CONNECTED;
        stateLock.notifyAll();
      } else if (state == FAILED || state == CONNECTED) { // reconnect
        if (logger.traceOn()) logger.trace("connect", "Re-establishing the connection...");
        if (state == CONNECTED) {
          state = FAILED;
          stateLock.notifyAll();
        }
        state = CONNECTING;
        stateLock.notifyAll();
        // should stop the old reader for cleaning
        if (reader != null) reader.stop();
        // Attention: lock order: stateLock before connectionLock reconnect. forbid all other requests
        synchronized (connectionLock) {
          connection.connect(this.env);
          connection = clientAdmin.connectionOpen(connection);
        }
        // wakeup all waiting threads
        if (logger.traceOn()) {
          String s = "Wakeup the threads which are waiting a response " + "frome the server to inform them of the connection failure.";
          logger.trace("connect", s);
        }
        final ConnectionClosedException ce = new ConnectionClosedException("The connection has been closed by the server.");
        // Attention: lock order: stateLock before waitingList before ResponseMsgWrapper
        synchronized (waitingList) {
          for (Long id: waitingList.keySet()) {
            ResponseMsgWrapper rm = waitingList.get(id);
            synchronized (rm) {
              if (!rm.got) { // see whether the response has arrived.
                rm.got = true;
                rm.msg = ce;
              }
              rm.notify();
            }
          }
          waitingList.clear();
        }
        state = CONNECTED;
        reader = new MessageReader();
        //threads.handoff(reader);
        ThreadService.getShared().handoff(reader);
        stateLock.notifyAll();
      } else checkState(); // is someone else calling connect()?
    }
    if (logger.traceOn()) logger.trace("connect", "Done");
  }

  @Override
  public void sendOneWay(final Message msg) throws IOException {
    if (logger.traceOn()) logger.trace("sendOneWay", "Send a message without response.");
    checkState();
    synchronized (connectionLock) {
      connection.writeMessage(msg);
    }
  }

  @Override
  public Message sendWithReturn(final Message msg) throws IOException {
    if (logger.traceOn()) logger.trace("sendWithReturn", "Send a message with response.");
    checkState();
    Message ret = null;
    if (msg instanceof NotificationRequestMessage) {
      if (logger.traceOn()) logger.trace("sendWithReturn", "Send a NotificationRequestMessage.");
      notifResp = null;
      synchronized (connectionLock) {
        connection.writeMessage(msg);
      }
      synchronized (notifLock) {
        while (notifResp == null) {
          checkState();
          try {
            notifLock.wait();
          } catch (InterruptedException ire) {
            InterruptedIOException iioe = new InterruptedIOException(ire.toString());
            EnvHelp.initCause(iioe, ire);
            throw iioe;
          }
        }
        ret = notifResp;
        notifResp = null;
      }
    } else if (msg instanceof MBeanServerRequestMessage) {
      if (logger.traceOn()) logger.trace("sendWithReturn", "Send a MBeanServerRequestMessage.");
      final Long id = new Long(((MBeanServerRequestMessage) msg).getMessageId());
      // When receiving CloseMessage, it is possible that the server closes itself by timeout, so we will do reconnection and then wakeup all
      // threads which are waiting a response by a ConnectionClosedException , to ask them to try once time again, the flag "retried" is specified here to tell whether the retried has done.
      // Note: if a ConnectionClosedException is thrown by the server, that exception will be received by ClientIntermediary and it will inform the ClientCommunicationAdmin before doing retry.
      boolean retried = false;
      while (true) {
        ResponseMsgWrapper mwrapper = new ResponseMsgWrapper();
        synchronized (waitingList) {
          waitingList.put(id, mwrapper);
        }
        synchronized (connectionLock) { // send out the msg
          connection.writeMessage(msg);
        }
        final long startTime = System.currentTimeMillis();
        boolean got = false;
        while (!got && (wtimeout > System.currentTimeMillis() - startTime)) {
          synchronized(mwrapper) { got = mwrapper.got; }
          if (!got) {
            try {
              checkState();
              synchronized(mwrapper) { mwrapper.wait(1000L); }
            } catch (InterruptedException ie) {
              break; // OK. This is a user thread, so it is possible that the user wants to stop waiting.
            }
          }
        }
        synchronized (waitingList) {
          waitingList.remove(id);
        }
        // at this point mwrapper has been already removed from the waitinglist
        // and it will not be modified any more. Synchronizing on mwrapper is no longer needed.
        if (!mwrapper.got) {
          if (!isTerminated()) {
            throw new InterruptedIOException("Waiting response timeout: " + wtimeout);
          } else {
            throw new IOException("The connection has been closed or broken.");
          }
        }
        if (mwrapper.msg instanceof MBeanServerResponseMessage) {
          ret = (MBeanServerResponseMessage) mwrapper.msg;
          break;
        } else if (mwrapper.msg instanceof ConnectionClosedException) {
          if (isTerminated() || retried) throw (ConnectionClosedException) mwrapper.msg;
          if (logger.traceOn()) logger.trace("sendWithReturn", "Got a local ConnectionClosedException, retry.");
          retried = true;
          continue;
        } else throw new IOException("Got wrong response: " + mwrapper.msg);
      }
    } else throw new IOException("Unknow message type: " + msg);
    return ret;
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
      clientAdmin.connectionClosed(connection);
      // clean
      if (logger.traceOn()) logger.trace("close", "Clean all threads waiting theire responses.");
      synchronized (waitingList) {
        for (ResponseMsgWrapper rm: waitingList.values()) {
          final ConnectionClosedException ce = new ConnectionClosedException("The connection has been closed by the server.");
          synchronized (rm) {
            if (!rm.got) { // see whether the response has arrived.
              rm.got = true;
              rm.msg = ce;
            }
            rm.notify();
          }
        }
        waitingList.clear();
      }
      // wakeup notif thread
      synchronized (notifLock) {
        notifLock.notify();
      }
      stateLock.notify();
    }
  }

  @Override
  public String getConnectionId() {
    // at client side, only clientAdmin can know connectionId when it receives HandshakeEndMessage from its server.
    return clientAdmin.getConnectionId();
  }

  /**
   * Returns the underlying asynchronous trasport.
   * @return .
   */
  public MessageConnection getAsynchroConnection() {
    return connection;
  }

  /** */
  private class MessageReader implements Runnable {
    /** */
    private Thread executingThread;
    /**
     * This flag is used to ensure that we interrupt the executingThread only when it is running in this MessageReader object.
     */
    private boolean executingThreadInterrupted = false;

    /** */
    public MessageReader() {
    }

    @Override
    public void run() {
      try {
        executingThread = Thread.currentThread();
        Message msg;
        while (!stopped()) {
          if (logger.traceOn()) logger.trace("MessageReader-run", "Waiting a coming message...");
          msg = null;
          try {
            msg = connection.readMessage();
          } catch (Exception e) {
            if (stopped()) break;
            try {
              callback.connectionException(e);
            } catch (Exception ee) { // OK. We have already informed the admin.
            }
            // if reconnected, a new reader should be created.
            break;
          }
          if (stopped()) break;
          if (msg instanceof NotificationResponseMessage) {
            synchronized (notifLock) {
              notifResp = msg;
              notifLock.notify();
            }
          } else if (msg instanceof MBeanServerResponseMessage) {
            ResponseMsgWrapper mwrapper;
            synchronized (waitingList) {
              mwrapper = (ResponseMsgWrapper) waitingList.get(new Long(((MBeanServerResponseMessage) msg).getMessageId()));
            }
            if (mwrapper == null) {
              checkState();
              // waiting thread is timeout
              if (logger.traceOn()) logger.trace("MessageReader-run", "Receive a MBeanServerResponseMessage but no one is waiting it.");
            } else {
              synchronized (mwrapper) {
                mwrapper.setMsg(msg);
                mwrapper.notify();
              }
            }
          } else ThreadService.getShared().handoff(new RemoteJob(msg)); // unknown message, protocol error
          if (msg instanceof CloseMessage) break;
        }
      } catch (Exception eee) {
        // need to stop
        if (logger.traceOn()) logger.trace("MessageReader-run", "stops.");
      }
      synchronized (stateLock) {
        executingThreadInterrupted = true;
      }
      if (logger.traceOn()) logger.trace("MessageReader-run", "ended.");
    }

    /** */
    public void stop() {
      if (logger.traceOn()) logger.trace("MessageReader-terminated", "be called.");
      synchronized (stateLock) {
        if (Thread.currentThread() != executingThread && executingThread != null && !executingThreadInterrupted) {
          executingThreadInterrupted = true;
          executingThread.interrupt();
        }
      }
      if (logger.traceOn()) logger.trace("MessageReader-terminated", "done.");
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

  /** */
  private static class ResponseMsgWrapper {
    /** */
    public boolean got = false;
    /** */
    public Object msg = null;

    /** */
    public ResponseMsgWrapper() {
    }

    /**
     * 
     * @param msg .
     */
    public void setMsg(final Message msg) {
      got = true;
      this.msg = msg;
    }
  }

  /** */
  private class RemoteJob implements Runnable {
    /** */
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
      if (logger.traceOn()) logger.trace("RemoteJob-run", "Receive a new request.");
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

  /**
   * 
   * @throws IOException .
   */
  private void checkState() throws IOException {
    synchronized (stateLock) {
      if (state == CONNECTED) return;
      else if (state == TERMINATED) throw new IOException("The connection has been closed.");
      // waiting
      long remainingTime = waitConnectedState;
      final long startTime = System.currentTimeMillis();
      while (state != CONNECTED && state != TERMINATED && remainingTime > 0) {
        try {
          stateLock.wait(remainingTime);
        } catch (InterruptedException ire) {
          break;
        }
        remainingTime = waitConnectedState - (System.currentTimeMillis() - startTime);
      }
      if (state == CONNECTED) return;
      else throw new IOException("The connection is not currently established.");
    }
  }

  /**
   * 
   * @return .
   */
  private boolean isTerminated() {
    synchronized (stateLock) {
      return (state == TERMINATED);
    }
  }
}
