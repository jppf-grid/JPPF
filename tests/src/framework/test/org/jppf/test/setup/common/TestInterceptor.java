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

package test.org.jppf.test.setup.common;

import java.io.*;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.jppf.comm.interceptor.AbstractNetworkConnectionInterceptor;

/**
 *
 * @author Laurent Cohen
 */
public class TestInterceptor extends AbstractNetworkConnectionInterceptor {
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
  protected boolean onAccept(final InputStream is, final OutputStream os) {
    DataInputStream dis = new DataInputStream(is);
    try {
      String msg = dis.readUTF();
      clientMessage = msg;
      if (CLIENT_MESSAGE.equals(msg)) {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeUTF(SERVER_MESSAGE);
        dos.flush();
        return true;
      }
    } catch (Exception e) {
    }
    return false;
  }

  @Override
  protected boolean onConnect(final InputStream is, final OutputStream os) {
    DataOutputStream dos = new DataOutputStream(os);
    try {
      dos.writeUTF(CLIENT_MESSAGE);
      dos.flush();
      DataInputStream dis = new DataInputStream(is);
      String msg = dis.readUTF();
      serverMessage = msg;
      if (SERVER_MESSAGE.equals(msg)) return true;
    } catch (Exception e) {
    }
    return false;
  }

  @Override
  public boolean onAccept(final Socket acceptedSocket) {
    return !active || super.onAccept(acceptedSocket);
  }

  @Override
  public boolean onAccept(final SocketChannel acceptedChannel) {
    return !active || super.onAccept(acceptedChannel);
  }

  @Override
  public boolean onConnect(final Socket connectedSocket) {
    return !active || super.onConnect(connectedSocket);
  }

  @Override
  public boolean onConnect(final SocketChannel connectedChannel) {
    return !active || super.onConnect(connectedChannel);
  }

  /**
   * Reset the messages to {@code null}.
   */
  public static void resetMessages() {
    clientMessage = null;
    serverMessage = null;
  }
}
