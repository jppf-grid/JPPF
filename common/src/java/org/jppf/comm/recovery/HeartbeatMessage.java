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

package org.jppf.comm.recovery;

import java.io.Serializable;

import org.jppf.utils.TypedProperties;

/**
 * Instances of this class represent heartbeat data exchanged between a driver and a node, client or peer driver.
 * @author Laurent Cohen
 * @exclude
 */
public class HeartbeatMessage implements Serializable {
  /**
   * Uuid property name.
   */
  private static final String UUID_PROP = "uuid";
  /**
   * Message id used to correlate the message send and the response received.
   */
  private final long messageID;
  /**
   * Additional properties sent in the message.
   */
  private final TypedProperties properties = new TypedProperties();
  /**
   * An optional response for request messages.
   */
  private transient HeartbeatMessage response;

  /**
   * Initialize this message with the specified message id.
   * @param messageID the id of this message.
   */
  public HeartbeatMessage(final long messageID) {
    this.messageID = messageID;
  }

  /**
   * @return the remote peer's uuid.
   */
  public String getUuid() {
    return properties.getString(UUID_PROP);
  }

  /**
   * Set the remote peer's uuid.
   * @param uuid the uuid ot set.
   */
  public void setUuid(final String uuid) {
    if (uuid != null) properties.setString(UUID_PROP, uuid);
  }

  /**
   * @return the message id.
   */
  public long getMessageID() {
    return messageID;
  }

  /**
   * @return the response, if any.
   */
  public HeartbeatMessage getResponse() {
    return response;
  }

  /**
   * Set the response for request messages
   * @param response the response to set.
   */
  public void setResponse(final HeartbeatMessage response) {
    this.response = response;
  }

  /**
   * @return the additional properties sent in the message.
   */
  public TypedProperties getProperties() {
    return properties;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(getClass().getSimpleName()).append('[')
      .append("messageID=").append(messageID)
      .append(", properties=").append(properties)
      .append(", response = ").append(response)
      .append(']').toString();
  }
}
