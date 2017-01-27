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

package org.jppf.test.addons.mbeans;

import java.io.Serializable;

import javax.management.NotificationEmitter;

/**
 * An MBean used for unit testing of receiving notifications from the nodes.
 * @author Laurent Cohen
 */
public interface NodeTestMBean extends Serializable, NotificationEmitter
{
  /**
   * The name of this mbean in a node.
   */
  String MBEAN_NAME = "org.jppf:name=test,type=node";

  /**
   * Send an object as a notification.
   * @param userObject the object to send as part of the notification.
   * @throws Exception if any error occurs.
   */
  void sendUserObject(Object userObject) throws Exception;

  /**
   * Get the total number of notifications emitted by this MBean.
   * @return the number of notifications as a long value.
   * @throws Exception if any error occurs.
   */
  Long getTotalNotifications() throws Exception;
}
