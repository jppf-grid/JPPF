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

package org.jppf.jmxremote.message;

import java.io.*;

import org.jppf.jmx.JMXHelper;

/**
 * Abstract superclass for all JMX messages.
 * @author Laurent Cohen
 */
abstract class AbstractJMXMessage implements JMXMessage {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The message identifier.
   */
  private long messageID;
  /**
   * The type of request to send.
   */
  private byte messageType;

  /**
   * Intiialize this message with the specified ID.
   * @param messageID the message ID.
   * @param messageType the type of request.
   */
  AbstractJMXMessage(final long messageID, final byte messageType) {
    this.messageID = messageID;
    this.messageType = messageType;
  }

  @Override
  public long getMessageID() {
    return messageID;
  }

  @Override
  public byte getMessageType() {
    return messageType;
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.writeByte(messageType);
    if (messageType != JMXHelper.CONNECT) out.writeLong(messageID);
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    messageType = in.readByte();
    messageID = (messageType == JMXHelper.CONNECT) ? JMXMessageHandler.CONNECTION_MESSAGE_ID : in.readLong();
  }
}
