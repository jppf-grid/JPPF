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

package org.jppf.admin.web.utils;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;

/**
 * Interface for actions whose enabled state can be updated based on the elements selected in a tree.
 * @author Laurent Cohen
 */
public interface UpdatableAction {
  /**
   * @return whether this action is enabled.
   */
  boolean isEnabled();

  /**
   * Compute the enabled stated of this action.
   * @param selected a list of the selected elements, in depth-first traversal order of the tree model.
   */
  void setEnabled(List<DefaultMutableTreeNode> selected);

  /**
   * Determine wehtehr this action is autothorized.
   * @return {@code true} if this action is authroized, {@code false} otherwise.
   */
  boolean isAuthorized();

  /**
   * Determine wehtehr this action is autothorized for the specified roles.
   * @param roles the roles to check for authorization.
   */
  void setAuthorized(Roles roles);
}
