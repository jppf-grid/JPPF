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

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractJMXMessage implements JMXMessage {
  /**
   * The message identifier.
   */
  protected final long messageID;
  /**
   * The type of request to send.
   */
  protected final JMXMessageType messageType;

  /**
   * Intiialize this message with the specified ID.
   * @param messageID the message ID.
   * @param messageType the type of request.
   */
  public AbstractJMXMessage(final long messageID, final JMXMessageType messageType) {
    this.messageID = messageID;
    this.messageType = messageType;
  }

  @Override
  public long getMessageID() {
    return messageID;
  }

  @Override
  public JMXMessageType getMessageType() {
    return messageType;
  }
}
