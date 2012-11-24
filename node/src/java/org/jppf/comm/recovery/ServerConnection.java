/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.net.Socket;

import org.jppf.comm.socket.*;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Instances of this class represent connections to a server from a remote peer.
 * <p>They are used to send, at regular intervals, a request to the distant peer and check
 * if the expected response is received. If the response is incorrect or not received at all,
 * then the connection is considered broken and closed on the server side.
 * <p>The main goal is to detect network connections broken due to hardware failures on
 * the remote peer side, which cannot be detected otherwise.
 * @author Laurent Cohen
 */
public class ServerConnection extends AbstractRecoveryConnection
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ServerConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this connection with the specified socket.
   * @param socket the socket connected to a client.
   * @param maxRetries the maximum number of failed write/read attempts on a connection before the remote peer is considered dead.
   * @param socketReadTimeout the maximum wait time on a response from the remote peer.
   * @throws Exception if any error occurs while initializing the socket connection.
   */
  public ServerConnection(final Socket socket, final int maxRetries, final int socketReadTimeout) throws Exception
  {
    this.ok = true;
    this.initialized = false;
    this.maxRetries = maxRetries;
    this.socketReadTimeout = socketReadTimeout;
    socketWrapper = new BootstrapSocketClient(socket);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void run()
  {
    if (!isOk()) return;
    if (!initialized) performHandshake();
    else performCheck();
  }

  /**
   * Perform the initial handshake with the remote peer.
   */
  private void performHandshake()
  {
    String response = doSendReceive("handshake");
    if (!isOk()) return;
    int idx = response.indexOf(';');
    if (idx < 0)
    {
      setOk(false);
      return;
    }
    uuid = response.substring(idx + 1);
  }

  /**
   * Perform the initial handshake with the remote peer.
   */
  private void performCheck()
  {
    doSendReceive("check");
  }

  /**
   * Send a string to the remote peer and receive a string back.
   * If any exception occurs while sending or receiving, the connection is considered broken.
   * @param message the string message to send to the remote peer.
   * @return the response as a string.
   */
  private String doSendReceive(final String message)
  {
    String response = null;
    try
    {
      if (socketWrapper == null) return null;
      sendMessage(message);
      response = receiveMessage();
      //sendMessage("final");
    }
    catch (Exception e)
    {
      close();
      if (debugEnabled) log.debug("error checking " + this, e);
    }
    return response;
  }

  /**
   * Close this server connection and release the resources it is using.
   */
  @Override
  public synchronized void close()
  {
    try
    {
      if (socketWrapper != null)
      {
        SocketWrapper tmp = socketWrapper;
        socketWrapper = null;
        setOk(false);
        tmp.close();
      }
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug("error closing " + this, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return StringUtils.build("ServerConnection[socketWrapper=", socketWrapper, ", ok=", ok, ", initialized=", initialized, ", uuid=", uuid, "]");
  }
}
