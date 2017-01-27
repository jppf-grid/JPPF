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

package org.jppf.node.event;


/**
 * This interface groups two other interfaces, from which an external can receive notifications from the node.
 * @param <C> the type of UI component to dispatch events to.
 * @author Laurent Cohen
 * @since 5.1
 */
public interface NodeIntegration<C> extends NodeLifeCycleListener, TaskExecutionListener {
  /**
   * Provide a reference to the UI component which uses the event notifications.
   * @param uiComponent the uiComponent to which events are dispatched.
   */
  void setUiComponent(C uiComponent);
}
