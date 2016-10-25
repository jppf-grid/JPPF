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

import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractMonitoringListener {
  /**
   * The tree table model.
   */
  protected final AbstractJPPFTreeTableModel treeModel;
  /**
   * Handles the selection of rows in the tree table.
   */
  protected final SelectionHandler selectionHandler;
  /**
   * The table tree to update.
   */
  protected JPPFTableTree tableTree;

  /**
   * Initialize with the specified tree model and selection handler.
   * @param treeModel the tree table model.
   * @param selectionHandler handles the selection of rows in the tree table.
   */
  public AbstractMonitoringListener(final AbstractJPPFTreeTableModel treeModel, final SelectionHandler selectionHandler) {
    this.treeModel = treeModel;
    this.selectionHandler = selectionHandler;
  }

  /**
   * @return the table tree to update.
   */
  public synchronized JPPFTableTree getTableTree() {
    return tableTree;
  }

  /**
   * Set the table tree to update.
   * @param tableTree the table tree to set.
   */
  public synchronized void setTableTree(final JPPFTableTree tableTree) {
    this.tableTree = tableTree;
  }
}
