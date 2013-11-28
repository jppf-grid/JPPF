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

package org.jppf.server.protocol;

import java.util.EventListener;

/**
 * Listener interface for receiving notifications of tasks events.
 * @deprecated sending task notifications via this means has very little, if any, usefulness,
 * as only the latest notification is kept and processed. In a multithreaded/parallel context,
 * this doesn't make any sense. It is much better to use the approach described in the <a href="http://www.jppf.org/samples-pack/TaskNotifications/Readme.php">TaskNotification sample</a>
 * @author Laurent Cohen
 * @exclude
 */
public interface JPPFTaskListener extends EventListener
{
  /**
   * Notify this listener that an event has occurred during a task's life cycle.
   * @param event the event this listener is notified of.
   * @deprecated
   */
  void eventOccurred(JPPFTaskEvent event);
}
