/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.awt.Rectangle;

import javax.swing.JViewport;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;

/**
 * A customization of the tree UI which applies the cell background color up to its rightmost edge, instead of only the width of the cell renderer.
 * @author Laurent Cohen
 */
class CustomTreeUI extends BasicTreeUI {
  /**
   * The tree table holding the {@code JTree} to customize.
   */
  private final JTreeTable treeTable;

  /**
   * Initialize this component UI with the specified tree table.
   * @param treeTable the tree table holding the {@code JTree} to customize.
   */
  public CustomTreeUI(final JTreeTable treeTable) {
    this.treeTable = treeTable;
  }
  
  @Override
  protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
    return new NodeDimensionsHandler() {
      @Override
      public Rectangle getNodeDimensions(final Object value, final int row, final int depth, final boolean expanded, final Rectangle size) {
        final JViewport port = (JViewport) treeTable.getParent();
        final Rectangle dimensions = super.getNodeDimensions(value, row, depth, expanded, size);
        if (port != null) dimensions.width = port.getWidth();
        return dimensions;
      }
    };
  }
}
