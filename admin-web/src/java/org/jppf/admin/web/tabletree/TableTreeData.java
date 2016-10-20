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

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.model.*;
import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.ui.treetable.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * @author Laurent Cohen
 */
public class TableTreeData implements SelectionListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TableTreeData.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The tree table model.
   */
  private transient AbstractJPPFTreeTableModel model;
  /**
   * Handles the selection of rows in the tree table.
   */
  private transient SelectionHandler selectionHandler;
  /**
   * The action handler.
   */
  private transient ActionHandler actionHandler;
  /**
   * The type of tree view.
   */
  private final TreeViewType viewType;
  /**
   * Keeps tabs on the expanded nodes in the tree.
   */
  private final IModel<Set<DefaultMutableTreeNode>> expansionModel;
  /**
   * Whether this is the first time any node is expanded.
   */
  private boolean firstExpansion = true;

  /**
   * @param viewType the view type.
   */
  public TableTreeData(final TreeViewType viewType) {
    this.viewType = viewType;
    actionHandler = new ActionHandler();
    selectionHandler = new MultipleSelectionHandler();
    selectionHandler.addSelectionListener(this);
    expansionModel = Model.ofSet(new HashSet<DefaultMutableTreeNode>());
  }

  /**
   * @return the tree table model.
   */
  public AbstractJPPFTreeTableModel getModel() {
    return model;
  }

  /**
   * @param model the tree table model.
   */
  public void setModel(final AbstractJPPFTreeTableModel model) {
    this.model = model;
  }

  /**
   * @return the selection handler.
   */
  public SelectionHandler getSelectionHandler() {
    return selectionHandler;
  }

  /**
   * @param selectionHandler the selection handler.
   */
  public void setSelectionHandler(final SelectionHandler selectionHandler) {
    this.selectionHandler = selectionHandler;
  }

  /**
   * @return the action handler.
   */
  public ActionHandler getActionHandler() {
    return actionHandler;
  }

  /**
   * @param actionHandler the action handler.
   */
  public void setActionHandler(final ActionHandler actionHandler) {
    this.actionHandler = actionHandler;
  }

  /**
   * Free resources.
   */
  public void cleanup() {
    model = null;
    selectionHandler = null;
    actionHandler = null;
    expansionModel.getObject().clear();
  }

  @Override
  public void selectionChanged(final SelectionHandler source) {
    if (debugEnabled) log.debug("selection changed: {}", source);
    actionHandler.selectionChanged(getSelectedTreeNodes());
  }

  /**
   * Get the currently selected nodes.
   * @return the selected tree nodes.
   */
  public List<DefaultMutableTreeNode> getSelectedTreeNodes() {
    List<DefaultMutableTreeNode> treeNodes = new ArrayList<>();
    exploreTreeSelection(getSelectionHandler(), (DefaultMutableTreeNode) getModel().getRoot(), treeNodes);
    return treeNodes;
  }

  /**
   * Get the list of selected nodes in depth-first traversal order.
   * @param handler the selection handler.
   * @param currentNode the node current being explored.
   * @param treeNodes the selected tree nodes.
   */
  void exploreTreeSelection(final SelectionHandler handler, final DefaultMutableTreeNode currentNode, final List<DefaultMutableTreeNode> treeNodes) {
    if (!currentNode.isRoot()) {
      AbstractComponent<?> data = (AbstractComponent<?>) currentNode.getUserObject();
      if (handler.isSelected(data.getUuid())) treeNodes.add(currentNode);
    }
    for (int i=0; i<currentNode.getChildCount(); i++) exploreTreeSelection(handler, (DefaultMutableTreeNode) currentNode.getChildAt(i), treeNodes);
  }

  /**
   * @return the view type.
   */
  public TreeViewType getViewType() {
    return viewType;
  }

  /**
   * @return the object that keeps tabs on the expanded nodes in the tree.
   */
  public IModel<Set<DefaultMutableTreeNode>> getExpansionModel() {
    return expansionModel;
  }

  /**
   * @return whether this is the first time any node is expanded.
   */
  public boolean isFirstExpansion() {
    return firstExpansion;
  }

  /**
   * Specify whether this is the first time any node is expanded.
   * @param firstExpansion {@code true} if first-tome expansion, {@code false} otherwise.
   */
  public void setFirstExpansion(final boolean firstExpansion) {
    this.firstExpansion = firstExpansion;
  }
}
