/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package org.jppf.ui.utils;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.treetable.*;
import org.slf4j.*;


/**
 * Collection utility methods for manipulating a JTreeTable and its model.
 * @author Laurent Cohen
 */
public final class TreeTableUtils {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TreeTableUtils.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Find the position at which to insert a driver, using the sorted lexical order of driver names.
   * @param parent the parent tree node for the driver to insert.
   * @param comp the driver to insert.
   * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
   */
  public static int insertIndex(final DefaultMutableTreeNode parent, final AbstractComponent<?> comp) {
    final Enumeration<?> children = parent.children();
    int count = 0;
    while (children.hasMoreElements()) {
      final DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
      final AbstractComponent<?> childData = (AbstractComponent<?>) child.getUserObject();
      if (childData == null) return -1;
      if (childData.getUuid().equals(comp.getUuid())) return -1;
      else if (comp.getDisplayName().compareTo(childData.getDisplayName()) < 0) return count;
      count++;
    }
    return parent.getChildCount();
  }

  /**
   * Find the tree node with the specified component uuid in the children of the specified parent.
   * @param parent the parent tree node for the component to find.
   * @param uuid uuid of the component to find.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  public static DefaultMutableTreeNode findComponent(final DefaultMutableTreeNode parent, final String uuid) {
    if (uuid == null) return null;
    final Enumeration<?> children = parent.children();
    while (children.hasMoreElements()) {
      final DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
      final AbstractComponent<?> data = (AbstractComponent<?>) child.getUserObject();
      if (data == null) continue;
      if (data.getUuid().equals(uuid)) return child;
    }
    return null;
  }

  /**
   * Get the path to an icon for the node given its state.
   * @param info represents the the node.
   * @return the path to an icon.
   */
  @SuppressWarnings("deprecation")
  public static String getNodeIconPath(final JPPFManagementInfo info) {
    if (info.isMasterNode()) return AbstractTreeCellRenderer.NODE_MASTER_ICON;
    return AbstractTreeCellRenderer.NODE_ICON;
  }

  /**
   * Find the tree node with the specified component uuid in the children of the specified parent.
   * @param root the parent tree node for the component to find.
   * @param uuid uuid of the component to find.
   * @param filter a filter that may reject a certain type of nodes, may be {@code null}.
   * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
   */
  public static DefaultMutableTreeNode findTreeNode(final DefaultMutableTreeNode root, final String uuid, final TreeNodeFilter filter) {
    if (uuid == null) return null;
    for (int i=0; i<root.getChildCount(); i++) {
      final DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
      final AbstractComponent<?> data = (AbstractComponent<?>) child.getUserObject();
      if (data == null) continue;
      if (data.getUuid().equals(uuid) && ((filter == null) || filter.accepts(child))) return child;
      final DefaultMutableTreeNode result = findTreeNode(child, uuid, filter);
      if (result != null) return result;
    }
    return null;
  }
}
