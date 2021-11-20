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

package org.jppf.jmxremote.nio;

import java.io.Serializable;

/**
 * Interface for an MBean that collects statistics on a {@link JMXNioServer} instance.
 * @author Laurent Cohen
 */
public interface JMXNioServerMBean extends Serializable {
  /**
   * Name of the node's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=JMXNioServer,type=debug,instance=";

  /**
   * Provide some statistics about this sserver, for debug purposes.
   * @return a map of statistics entries.
   */
  String stats();

  /**
   * Get the maximum number of pending notifications in the queue of a JMX connection.
   * @return the peak queue size as an int.
   */
  int getPeakPendingMessages();
}
