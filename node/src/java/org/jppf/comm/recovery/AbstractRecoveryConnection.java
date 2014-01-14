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

package org.jppf.comm.recovery;

import java.net.SocketTimeoutException;

import org.jppf.JPPFException;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Common abstract super class for a connection dedicated to recovery from hardware failure of a remote peer.
 * @author Laurent Cohen
 */
public abstract class AbstractRecoveryConnection extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractRecoveryConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Connection to a client.
   */
  protected SocketWrapper socketWrapper = null;
  /**
   * Maximum number of failed write/read attempts on a connection before the remote peer is considered dead.
   */
  protected int maxRetries;
  /**
   * Maximum wait time on a response from the remote peer.
   */
  protected int socketReadTimeout;
  /**
   * The JPPF node or client uuid.
   */
  protected String uuid = null;
  /**
   * Determines whether this connection is ok after is has been checked.
   */
  protected boolean ok;
  /**
   * Determines whether the initial handshake has been performed.
   */
  protected boolean initialized;
  /**
   * 
   */
  protected Thread runThread = null;

  /**
   * Read a message form the remote peer.
   * While receiving the message, this method also waits for {@link #socketReadTimeout} specified
   * in the configuration. If the timeout expires {@link #maxRetries} times in a row, the connection
   * is also considered broken.
   * @return the message that was received.
   * @throws Exception if any error occurs.
   */
  protected String receiveMessage() throws Exception
  {
    return receiveMessage(this.maxRetries, this.socketReadTimeout);
  }

  /**
   * Read a message from the remote peer.
   * While receiving the message, this method also waits for {@link #socketReadTimeout} specified
   * in the configuration. If the timeout expires {@link #maxRetries} times in a row, the connection
   * is also considered broken.
   * @param maxRetries maximum number of attempts to read a response form the remote peer.
   * @param socketReadTimeout timeout for each attempt.
   * @return the message that was received.
   * @throws Exception if any error occurs.
   */
  protected String receiveMessage(final int maxRetries, final int socketReadTimeout) throws Exception
  {
    String message = null;
    JPPFBuffer buffer = null;
    int retries = 0;
    boolean success = false;
    while ((retries < maxRetries) && !success)
    {
      try
      {
        buffer = socketWrapper.receiveBytes(socketReadTimeout);
        success = true;
        message = buffer.asString();
        if (traceEnabled) log.trace("received '{}' for {}", message, this);
      }
      catch (SocketTimeoutException e)
      {
        retries++;
        if (debugEnabled) log.debug("retry #{} failed for {}", retries, this);
      }
    }
    if (!success) throw new JPPFException("could not get a message from the remote peer");
    return message;
  }

  /**
   * Send a message to the remote peer.
   * @param message the message to send.
   * @throws Exception if any error occurs while sending the message.
   */
  public void sendMessage(final String message) throws Exception
  {
    JPPFBuffer buffer = new JPPFBuffer(message);
    socketWrapper.sendBytes(buffer);
    if (traceEnabled) log.trace("sent '{}' from {}", message, this);
  }

  /**
   * Close this client and release any resources it is using.
   */
  public abstract void close();

  /**
   * Get the uuid of the remote peer.
   * @return the uuid as a string.
   */
  public String getUuid()
  {
    return uuid;
  }

  /**
   * Determine whether this connection is ok after is has been checked.
   * @return true if the connection is ok, false otherwise.
   */
  public synchronized boolean isOk()
  {
    return ok;
  }

  /**
   * Specify whether this connection is ok after is has been checked.
   * @param ok true if the connection is ok, false otherwise.
   */
  public synchronized void setOk(final boolean ok)
  {
    this.ok = ok;
  }

  /**
   * Determine whether the initial handshake has been performed.
   * @return <code>true</code> if the initial handshake was done, <code>false</code> otherwise.
   */
  public synchronized boolean isInitialized()
  {
    return initialized;
  }

  /**
   * Specify whether the initial handshake has been performed.
   * @param initialized <code>true</code> if the initial handshake was done, <code>false</code> otherwise.
   */
  public synchronized void setInitialized(final boolean initialized)
  {
    this.initialized = initialized;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("socketWrapper=").append(socketWrapper);
    sb.append(", maxRetries=").append(maxRetries);
    sb.append(", socketReadTimeout=").append(socketReadTimeout);
    sb.append(", uuid=").append(uuid);
    sb.append(", ok=").append(ok);
    sb.append(", initialized=").append(initialized);
    sb.append(']');
    return sb.toString();
  }
}
