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
package org.jppf.node.screensaver;

import org.jppf.node.NodeInternal;
import org.jppf.node.event.*;
import org.jppf.utils.TypedProperties;

/**
 * This class registers as a NodeLifeCycleListener and instantiates an implementation of {@link NodeIntegration},
 * if any is defined in the configuration. It also registers this implementation as a {@link TaskExecutionListener}
 * with the node, so it can receive a notification for each individual task reaches completion. 
 * @author Laurent Cohen
 * @exclude
 */
public class DelegatingNodeListener extends NodeLifeCycleListenerAdapter {
  /**
   * The node life cycle listner to delegate events to.
   */
  private NodeIntegration delegate;

  /**
   * Initialize this listener and instantiate the delegate if one is configured.
   */
  public DelegatingNodeListener() {
    try {
      ScreenSaverMain ssm = ScreenSaverMain.getInstance();
      if (ssm != null) {
        TypedProperties config = ssm.getConfig();
        String name = config.getString("jppf.screensaver.node.listener");
        if (name != null) {
          Class<?> clazz = Class.forName(name, true, getClass().getClassLoader());
          delegate = (NodeIntegration) clazz.newInstance();
          delegate.setUiComponent(ssm.getScreenSaver());
        }
      }
    } catch (Exception e) {
    }
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    if (delegate != null) {
      ((NodeInternal) event.getNode()).getExecutionManager().getTaskNotificationDispatcher().addTaskExecutionListener(delegate);
      delegate.nodeStarting(event);
    }
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    if (delegate != null) {
      try {
        delegate.nodeEnding(event);
      } finally {
        ((NodeInternal) event.getNode()).getExecutionManager().getTaskNotificationDispatcher().removeTaskExecutionListener(delegate);
      }
    }
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
    if (delegate != null) delegate.jobHeaderLoaded(event);
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    if (delegate != null) delegate.jobStarting(event);
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
    if (delegate != null) delegate.jobEnding(event);
  }
}
