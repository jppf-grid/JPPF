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

package org.jppf.ui.plugin;

import org.jppf.ui.options.OptionElement;

/**
 *
 * @author Laurent Cohen
 * @exclude
 */
public class PluggableViewDescriptor {
  /**
   * The name of the view.
   */
  private final String name;
  /**
   * The view's component tree.
   */
  private final OptionElement option;
  /**
   * The container of the view.
   */
  private final String containerName;
  /**
   * The position of the view within the container.
   */
  private final int position;

  /**
   * Initialize this descriptor with the specified parameters.
   * @param name the name of the view.
   * @param option the view's component tree.
   * @param containerName the container of the view.
   * @param position the position of the view within the container.
   */
  public PluggableViewDescriptor(final String name, final OptionElement option, final String containerName, final int position) {
    this.name = name;
    this.option = option;
    this.containerName = containerName;
    this.position = position;
  }

  /**
   * Get the name of the view.
   * @return the name as a string.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the view's component tree.
   * @return an {@link OptionElement} object.
   */
  public OptionElement getOption() {
    return option;
  }

  /**
   * Get the name of the view's container.
   * @return the container name as a string.
   */
  public String getContainerName() {
    return containerName;
  }

  /**
   * Get the position of the view within the container.
   * @return the position as an int.
   */
  public int getPosition() {
    return position;
  }
}
