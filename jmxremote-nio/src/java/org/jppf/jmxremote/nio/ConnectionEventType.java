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

package org.jppf.jmxremote.nio;

import java.util.List;

import org.jppf.jmxremote.*;

/**
 * Enumeration of the possible types of connection status events on the server side.
 * @author Laurent Cohen
 */
enum ConnectionEventType {
  /**
   * Connection opened notification type.
   */
  OPENED {
    @Override
    void fireNotification(final List<JMXConnectionStatusListener> listeners, final JMXConnectionStatusEvent event) {
      for (JMXConnectionStatusListener listener: listeners) listener.connectionOpened(event);
    }
  },
  /**
   * Connection closed notification type.
   */
  CLOSED {
    @Override
    void fireNotification(final List<JMXConnectionStatusListener> listeners, final JMXConnectionStatusEvent event) {
      for (JMXConnectionStatusListener listener: listeners) listener.connectionClosed(event);
    }
  },
  /**
   * Connection failed notification type.
   */
  FAILED {
    @Override
    void fireNotification(final List<JMXConnectionStatusListener> listeners, final JMXConnectionStatusEvent event) {
      for (JMXConnectionStatusListener listener: listeners) listener.connectionFailed(event);
    }
  };

  /**
   * Notify all listeners of the event.
   * @param listeners the listeners to notify.
   * @param event the event to notify of.
   */
  abstract void fireNotification(List<JMXConnectionStatusListener> listeners, final JMXConnectionStatusEvent event);
}