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

package org.jppf.jmxremote;

import java.util.EventListener;

/**
 * Listener interface for receiving JMX connection events on the server side.
 * @author Laurent Cohen
 * @exclude
 */
public interface JMXConnectionStatusListener extends EventListener {
  /**
   * Called when a connection is opened.
   * @param event encapsulates the connection ID and Throwable information.
   */
  void connectionOpened(JMXConnectionStatusEvent event);

  /**
   * Called when a connection is closed.
   * @param event encapsulates the connection ID and Throwable information.
   */
  void connectionClosed(JMXConnectionStatusEvent event);

  /**
   * Called when a connection fails.
   * @param event encapsulates the connection ID and Throwable information.
   */
  void connectionFailed(JMXConnectionStatusEvent event);
}
