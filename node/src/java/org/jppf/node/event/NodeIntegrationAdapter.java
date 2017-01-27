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
 * An abstract adapter class for receiving node events. The methods in this class are empty.
 * This class exists as convenience, to be overriden for creating listener objects, instead of
 * implementing the {@link NodeLifeCycleListener} and {@link TaskExecutionListener} interfaces.
 * @param <C> the type of UI component to dispatch events to.
 * @author Laurent Cohen
 * @since 5.1
 */
public abstract class NodeIntegrationAdapter<C> extends NodeLifeCycleListenerAdapter implements NodeIntegration<C> {
  @Override
  public void taskExecuted(final TaskExecutionEvent event) {
  }

  @Override
  public void taskNotification(final TaskExecutionEvent event) {
  }

  @Override
  public void setUiComponent(final C uiComponent) {
  }
}
