/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import java.util.Arrays;

import javax.management.Notification;

import org.jppf.jmx.JMXHelper;

/**
 * A specialized message that represents a JMX notification to dispatch on the client side.
 * @author Laurent Cohen
 */
public class JMXNotification extends AbstractJMXMessage {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Constant for an empty set of listeners.
   */
  private static final Integer[] NO_LISTENER = new Integer[0];
  /**
   * The notification to dispatch.
   */
  private Notification notification;
  /**
   * The ids of the listeners to dispatch to.
   */
  private Integer[] listenerIDs;

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param notification the notification to dispatch.
   * @param listenerIDs ids of the listeners to dispatch the notification to.
   */
  public JMXNotification(final long messageID, final Notification notification, final Integer[] listenerIDs) {
    super(messageID, JMXHelper.NOTIFICATION);
    this.notification = notification;
    this.listenerIDs = (listenerIDs == null) ? NO_LISTENER : listenerIDs;
  }

  /**
   * @return the actual notification.
   */
  public Notification getNotification() {
    return notification;
  }

  /**
   * @return the ids of the listeners to dispatch to.
   */
  public Integer[] getListenerIDs() {
    return listenerIDs;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("messageID=").append(getMessageID())
      .append(", messageType=").append(JMXHelper.name(getMessageType()))
      .append(", listenerIDs=").append(Arrays.toString(listenerIDs))
      .append(", notification=").append(notification)
      .append(']').toString();
  }

  /**
   * Save the state of the {@code AbstractJPPFJob} instance to a stream (i.e.,serialize it).
   * @param out the output stream to which to write the job. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.writeObject(notification);
    out.writeInt(listenerIDs.length);
    for (final int id: listenerIDs) out.writeInt(id);
  }

  /**
   * Reconstitute the {@code AbstractJPPFJob} instance from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the job. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    notification = (Notification) in.readObject();
    final int n = in.readInt();
    if (n == 0) listenerIDs = NO_LISTENER;
    else {
      listenerIDs = new Integer[n];
      for (int i=0; i<n; i++) listenerIDs[i] = in.readInt();
    }
  }
}
