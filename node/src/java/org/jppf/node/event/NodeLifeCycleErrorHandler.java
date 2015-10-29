/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.node.event;

/**
 * Interface for handling uncaught throwables raised when executing one of the methods of {@link NodeLifeCycleListener}.
 * @author Laurent Cohen
 */
public interface NodeLifeCycleErrorHandler
{
  /**
   * Handle the throwable raised for the specified event.
   * @param listener the listener whose method was being executed when the thowable was raised.
   * @param event the event notification for which an error was raised.
   * @param t the uncaught throwable that was raised during the notification. 
   */
  void handleError(NodeLifeCycleListener listener, NodeLifeCycleEvent event, Throwable t);
}
