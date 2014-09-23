/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.ui.options.docking;

import java.awt.Component;

import org.jppf.ui.options.*;

/**
 * Describes the state of a component with regard to the view to which it is attached.
 */
public class DetachableComponentDescriptor {
  /**
   * The component to describe.
   */
  private final OptionElement component;
  /**
   * The UI component which has the mouse listener.
   */
  private Component listenerComponent;
  /**
   * The container to which the component was initially attached.
   */
  private final OptionContainer initialContainer;
  /**
   * The container to which the component is currently attached.
   */
  private OptionContainer currentContainer;
  /**
   * Id of the view in which the component is currently displayed.
   */
  private String viewId;

  /**
   * Initialize this descriptor
   * @param component the component to describe.
   * @param listenerComponent the UI component which has the mouse listener.
   */
  public DetachableComponentDescriptor(final OptionElement component, final Component listenerComponent) {
    this.component = component;
    this.listenerComponent = listenerComponent;
    this.initialContainer = (OptionContainer) component.getParent();
    this.currentContainer = this.initialContainer;
    this.viewId = DockingManager.INITIAL_VIEW;
  }

  /**
   * Get the described component.
   * @return the component as an {@link OptionElement} object.
   */
  public OptionElement getComponent() {
    return component;
  }

  /**
   * Get the container to which the component was initially attached.
   * @return the initial container as an {@link OptionContainer} object.
   */
  public OptionContainer getInitialContainer() {
    return initialContainer;
  }

  /**
   * Get the container to which the component is currently attached.
   * @return the current container as an {@link OptionContainer} object.
   */
  public OptionContainer getCurrentContainer() {
    return currentContainer;
  }

  /**
   * Set the container to which the component is currently attached.
   * @param currentContainer the current container as an {@link OptionContainer} object.
   */
  public void setCurrentContainer(final OptionContainer currentContainer) {
    this.currentContainer = currentContainer;
  }

  /**
   * Get the  id of the view in which the component is currently displayed.
   * @return the view id as a string.
   */
  public String getViewId() {
    return viewId;
  }

  /**
   * Get the  id of the view in which the component is currently displayed.
   * @param viewId the view id as a string.
   */
  public void setViewId(final String viewId) {
    this.viewId = viewId;
  }

  /**
   * Get the UI component which has the mouse listener.
   * @return a {@link Component}.
   */
  public Component getListenerComponent() {
    return listenerComponent;
  }

  /**
   * Set the UI component which has the mouse listener.
   * @param listenerComponent a {@link Component}.
   */
  public void setListenerComponent(final Component listenerComponent) {
    this.listenerComponent = listenerComponent;
  }
}
