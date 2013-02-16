/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.management.forwarding;

import java.util.EventListener;

/**
 * Registered instances of this class will be notified of JMX notifications received from the nodes.
 * @author Laurent Cohen
 * @exclude
 */
public interface ForwardingNotificationEventListener extends EventListener
{
  /**
   * Called when a JMX notification is received from any node with a registered notification listener.
   * @param event encapsulates the notification and information on its origin.
   */
  void notificationReceived(ForwardingNotificationEvent event);
}
