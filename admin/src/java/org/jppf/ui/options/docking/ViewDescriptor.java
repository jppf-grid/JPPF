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

package org.jppf.ui.options.docking;

import java.awt.Window;
import java.util.*;

import org.jppf.ui.options.*;

/**
 * Encapsulates a JFrame and main container for a given view id.
 */
public class ViewDescriptor {
  /**
   * The frame for this view.
   */
  private final Window frame;
  /**
   * The main container to which other elements are attached.
   */
  private final OptionContainer container;
  /**
   * The components attached to this view.
   */
  private final Set<DetachableComponentDescriptor> components = new LinkedHashSet<>();

  /**
   * Initialize this view descriptor.
   * @param frame the frame for this view.
   * @param container the main container to which other elements are attached.
   */
  public ViewDescriptor(final Window frame, final OptionContainer container) {
    this.frame = frame;
    this.container = container;
  }

  /**
   * Get the frame for this view.
   * @return a {@link Window} instance.
   */
  public Window getFrame() {
    return frame;
  }

  /**
   * Get the main container to which other elements are attached.
   * @return an {@link OptionElement} instance.
   */
  public OptionContainer getContainer() {
    return container;
  }

  /**
   * Get the components attached to this view.
   * @return a Set of components.
   */
  public Set<DetachableComponentDescriptor> getComponents() {
    return components;
  }

  /**
   * Add a component to this view.
   * @param desc the component to add.
   */
  public void addComponent(final DetachableComponentDescriptor desc) {
    components.add(desc);
  }

  /**
   * Remove a component from this view.
   * @param desc the component to remove.
   */
  public void removeComponent(final DetachableComponentDescriptor desc) {
    components.remove(desc);
  }
}
