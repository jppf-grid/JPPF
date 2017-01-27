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

package org.jppf.management;

import java.util.EventListener;

/**
 * 
 * @author Laurent Cohen
 */
public interface JMXWrapperListener extends EventListener {
  /**
   * Notify listeners that a JMX connection wrapper successfully connected to the remote JMX server.
   * @param event the event encapsulating the connection.
   */
  void jmxWrapperConnected(JMXWrapperEvent event);
  /**
   * Notify listeners that a JMX connection wrapper failed to connect before the timeout specified in the configuration.
   * @param event the event encapsulating the connection.
   */
  void jmxWrapperTimeout(JMXWrapperEvent event);
}
