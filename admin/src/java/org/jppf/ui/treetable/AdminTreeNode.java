/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.ui.treetable;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.AbstractComponent;

/**
 *
 * @author Laurent Cohen
 */
public class AdminTreeNode extends DefaultMutableTreeNode {
  /**
   * Uuid of the underlying component.
   */
  private final String uuid;
  /**
   * Type of view this node is part of.
   */
  private final TreeViewType type;

  /**
   *
   * @param userObject an Object provided by the user that constitutes the node's data.
   * @param type the type of view where the node is displayed.
   */
  public AdminTreeNode(final Object userObject, final TreeViewType type) {
    super(userObject);
    uuid = (userObject instanceof AbstractComponent) ? ((AbstractComponent<?>) userObject).getUuid() : null;
    this.type = type;
  }

  /**
   * @return the uid of the underlying component.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * @return the type of view where the node is displayed.
   */
  public TreeViewType getType() {
    return type;
  }
}
