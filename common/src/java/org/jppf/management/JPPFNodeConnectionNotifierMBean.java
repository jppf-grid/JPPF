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

package org.jppf.management;

import javax.management.NotificationEmitter;

/**
 * This MBean provides notifications of nodes connecting to, or disconnecting from a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFNodeConnectionNotifierMBean extends NotificationEmitter {
  /**
   * The name of this MBean, used when it is registered with an MBean server.
   */
  String MBEAN_NAME = "org.jppf:name=nodeConnectionNotifier,type=driver";
  /**
   * The type of notification which indicates that a node is connected.
   */
  String CONNECTED = "connected";
  /**
   * The type of notification which indicates that a node is disconnected.
   */
  String DISCONNECTED = "disconnected";
}
