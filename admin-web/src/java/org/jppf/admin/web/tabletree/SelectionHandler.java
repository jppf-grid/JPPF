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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jppf.ui.treetable.TreeNodeFilter;

/**
 * 
 * @author Laurent Cohen
 */
public interface SelectionHandler {
  /**
   * Handle a selection/deselction event for the specified tree node.
   * @param node the node to handle.
   * @param params optional additional parameters.
   * @param target ajax target for the selection request.
   * @return {@code true} if the node is selected, {@code false} otherwise.
   */
  boolean handle(final AjaxRequestTarget target, DefaultMutableTreeNode node, Object... params);

  /**
   * 
   * @return the uuids of the selected tree nodes.
   */
  List<String> getSelected();

  /**
   * Determine whether the specified node is selected.
   * @param uuid uuid of the node to check.
   * @return true if the node with the specified uuid is selected, false otherwise.
   */
  boolean isSelected(String uuid);

  /**
   * Get the filter for this selection handler.
   * @return a {@link TreeNodeFilter} instance.
   */
  TreeNodeFilter getFilter();

  /**
   * Set the filter for this selection handler.
   * @param filter a {@link TreeNodeFilter} instance.
   * @return this selection handler, for method call chaining.
   */
  SelectionHandler setFilter(final TreeNodeFilter filter);

  /**
   * Select the specified uuid.
   * @param uuid uuid of the element to select.
   */
  void select(String uuid);

  /**
   * un-select the specified uuid.
   * @param uuid uuid of the element to unselect.
   */
  void unselect(String uuid);

  /**
   * Clear the selection.
   */
  void clearSelection();

  /**
   * Free resources.
   */
  void cleanup();

  /**
   * Add a selection listenr to this handler.
   * @param listener the listener to add.
   */
  void addSelectionListener(SelectionListener listener);

  /**
   * Remove a selection listenr from this handler.
   * @param listener the listener to remove.
   */
  void removeSelectionListener(SelectionListener listener);
}