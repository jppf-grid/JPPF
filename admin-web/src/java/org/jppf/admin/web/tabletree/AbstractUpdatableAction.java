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

package org.jppf.admin.web.tabletree;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.jppf.client.monitoring.topology.AbstractTopologyComponent;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractUpdatableAction implements UpdatableAction {
  /**
   *
   */
  protected boolean enabled = true;
  /**
   *
   */
  protected boolean authorized = true;

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(final List<DefaultMutableTreeNode> selected) {
  }

  @Override
  public boolean isAuthorized() {
    return authorized;
  }

  @Override
  public void setAuthorized(final Roles roles) {
  }

  /**
   * Determine whether at least one of the selected elements is a node.
   * @param selected the selected elements.
   * @return {@code true} if at least one node is selected, {@code false} otherwise.
   */
  protected boolean isNodeSelected(final List<DefaultMutableTreeNode> selected) {
    for (DefaultMutableTreeNode treeNode: selected) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isNode()) return true;
    }
    return false;
  }

  /**
   * Determine whether at least one of the selected elements is a node.
   * @param selected the selected elements.
   * @return {@code true} if at least one node is selected, {@code false} otherwise.
   */
  protected boolean isDriverSelected(final List<DefaultMutableTreeNode> selected) {
    for (DefaultMutableTreeNode treeNode: selected) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isDriver()) return true;
    }
    return false;
  }
}
