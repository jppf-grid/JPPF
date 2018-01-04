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
package org.jppf.comm.socket;

import java.net.Socket;

import org.jppf.JPPFException;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.JPPFBuffer;
import org.slf4j.*;

/**
 * This class provides a simple API to transfer objects over a TCP socket connection.
 * @author Laurent Cohen
 */
public class SocketClient extends AbstractSocketWrapper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SocketClient.class);

  /**
   * Default constructor is invisible to other classes.
   */
  public SocketClient() {
    super();
  }

  /**
   * Initialize this socket client and connect it to the specified host on the specified port.
   * 
   * @param host the remote host this socket client connects to.
   * @param port the remote port on the host this socket client connects to.
   * @throws Exception if there is an issue with the socket streams.
   */
  public SocketClient(final String host, final int port) throws Exception {
    super(host, port, null);
  }

  /**
   * Initialize this socket client and connect it to the specified host on the specified port.
   * @param host the remote host this socket client connects to.
   * @param port the remote port on the host this socket client connects to.
   * @param serializer the object serializer used by this socket client.
   * @throws Exception if there is an issue with the socket streams.
   */
  public SocketClient(final String host, final int port, final ObjectSerializer serializer) throws Exception {
    super(host, port, serializer);
  }

  /**
   * Initialize this socket client with an already opened and connected socket.
   * @param socket the underlying socket this socket client wraps around.
   * @throws JPPFException if the socket connection fails.
   */
  public SocketClient(final Socket socket) throws JPPFException {
    super(socket);
  }

  /**
   * Send an object over a TCP socket connection.
   * @param o the object to send.
   * @throws Exception if the underlying output stream throws an exception.
   */
  @Override
  public void send(final Object o) throws Exception {
    final JPPFBuffer buf = getSerializer().serialize(o);
    sendBytes(buf);
  }

  /**
   * Read an object from a TCP socket connection. This method blocks until an object is received
   * or the specified timeout has expired, whichever happens first.
   * @param timeout timeout after which the operation is aborted. A timeout of zero is interpreted as an infinite timeout.
   * @return the object that was read from the underlying input stream or null if the operation timed out.
   * @throws Exception if the underlying input stream throws an exception.
   */
  @Override
  public Object receive(final int timeout) throws Exception {
    checkOpened();
    Object o = null;
    try {
      if (timeout > 0) socket.setSoTimeout(timeout);
      final JPPFBuffer buf = receiveBytes(timeout);
      o = getSerializer().deserialize(buf);
    } finally {
      // disable the timeout on subsequent read operations.
      if (timeout > 0) socket.setSoTimeout(0);
    }
    return o;
  }

  /**
   * Get an object serializer / deserializer to convert an object to or from an array of bytes.
   * @return an <code>ObjectSerializer</code> instance.
   */
  @Override
  public ObjectSerializer getSerializer() {
    if (serializer == null) {
      // serializer = new ObjectSerializerImpl();
      final String name = "org.jppf.utils.ObjectSerializerImpl";
      try {
        serializer = (ObjectSerializer) Class.forName(name).newInstance();
      } catch (InstantiationException|IllegalAccessException|ClassNotFoundException e) {
        log.error(e.getMessage(), e);
      }
    }
    return serializer;
  }

  /**
   * Set the object serializer / deserializer to convert an object to or from an array of bytes.
   * @param serializer an <code>ObjectSerializer</code> instance.
   */
  @Override
  public void setSerializer(final ObjectSerializer serializer) {
    this.serializer = serializer;
  }

  /**
   * Generate a string representation of this socket client.
   * @return as string describing this object.
   */
  @Override
  public String toString() {
    return "SocketClient[" + this.host + ':' + this.port + ", open=" + this.opened + ']';
  }
}
