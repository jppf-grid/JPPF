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

package test.org.jppf.test.setup.common;

import java.io.*;
import java.net.Socket;

import org.jppf.comm.interceptor.AbstractNetworkConnectionInterceptor;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class TestInterceptor extends AbstractNetworkConnectionInterceptor {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TestInterceptor.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The text of the message sent by the client.
   */
  public static final String CLIENT_MESSAGE = "message sent by the client";
  /**
   * The text of the message sent by the server.
   */
  public static final String SERVER_MESSAGE = "message sent by the server";
  /**
   * If {@code true} then invoke the intercpetor's methods, otherwise just return {@code true}.
   */
  public static boolean active = false;
  /**
   * The message sent by the client and set here by the server.
   */
  public static String clientMessage;
  /**
   * The message sent by the server and set here by the client.
   */
  public static String serverMessage;

  @Override
  public boolean onAccept(final Socket acceptedSocket) {
    if (!active) return true;
    if (debugEnabled) log.debug("start");
    try {
      final InputStream is = acceptedSocket.getInputStream();
      final OutputStream os = acceptedSocket.getOutputStream();
      final DataInputStream dis = new DataInputStream(is);
      final String msg = dis.readUTF();
      if (debugEnabled) log.debug("read '{}'", msg);
      clientMessage = msg;
      if (CLIENT_MESSAGE.equals(msg)) {
        final DataOutputStream dos = new DataOutputStream(os);
        dos.writeUTF(SERVER_MESSAGE);
        dos.flush();
        if (debugEnabled) log.debug("wrote '{}'", SERVER_MESSAGE);
        return true;
      }
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      //e.printStackTrace();
    }
    if (debugEnabled) log.debug("failed");
    return false;
  }

  @Override
  public boolean onConnect(final Socket connectedSocket) {
    if (!active) return true;
    if (debugEnabled) log.debug("start");
    try {
      final InputStream is = connectedSocket.getInputStream();
      final OutputStream os = connectedSocket.getOutputStream();
      final DataOutputStream dos = new DataOutputStream(os);
      dos.writeUTF(CLIENT_MESSAGE);
      dos.flush();
      if (debugEnabled) log.debug("wrote '{}'", CLIENT_MESSAGE);
      final DataInputStream dis = new DataInputStream(is);
      final String msg = dis.readUTF();
      if (debugEnabled) log.debug("read '{}'", msg);
      serverMessage = msg;
      if (SERVER_MESSAGE.equals(msg)) return true;
    } catch (final Exception e) {
      e.printStackTrace();
    }
    if (debugEnabled) log.debug("failed");
    return false;
  }

  /**
   * Reset the messages to {@code null}.
   */
  public static void resetMessages() {
    clientMessage = null;
    serverMessage = null;
  }
}
