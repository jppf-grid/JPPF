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
/*
 * @(#)JTreeTable.java  1.2 98/10/27
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jppf.ui.treetable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;

import org.slf4j.*;

/**
 * This example shows how to create a simple JTreeTable component, by using a JTree as a renderer (and editor) for the
 * cells in a particular column in the JTable.
 *
 * @version 1.2 10/27/98
 *
 * @author Philip Milne
 * @author Scott Violet
 */
public class JTreeTable extends JTable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JTreeTable.class);
  /**
   * A subclass of JTree.
   */
  protected TreeTableCellRenderer tree;

  /**
   * Initialize this tree table with the specified model.
   * @param treeTableModel this tree table's model.
   */
  public JTreeTable(final TreeTableModel treeTableModel) {
    super();
    // Create the tree. It will be used as a renderer and editor.
    tree = new TreeTableCellRenderer(treeTableModel);
    // Install a tableModel representing the visible rows in the tree.
    super.setModel(new TreeTableModelAdapter(treeTableModel, tree));
    // Force the JTable and JTree to share their row selection models.
    final ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
    tree.setSelectionModel(selectionWrapper);
    selectionWrapper.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    setSelectionModel(selectionWrapper.getListSelectionModel());
    selectionWrapper.getListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    // Install the tree editor renderer and editor.
    setDefaultRenderer(TreeTableModel.class, tree);
    setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
    // No grid.
    setShowGrid(false);
    // No intercell spacing
    setIntercellSpacing(new Dimension(0, 0));
    // And update the height of the trees row to match that of the table.
    // Metal looks better like this.
    if (tree.getRowHeight() < 1) setRowHeight(18);
    //setTableHeader(new TreeTableHeader());
  }

  /**
   * Overridden to message super and forward the method to the tree. Since the tree is not actually in the component
   * hierarchy it will never receive this unless we forward it in this manner.
   */
  @Override
  public void updateUI() {
    super.updateUI();
    if (tree != null) tree.updateUI();
    // Use the tree's default foreground and background colors in the table.
    LookAndFeel.installColorsAndFont(this, "Tree.background", "Tree.foreground", "Tree.font");
  }

  /**
   * Workaround for BasicTableUI anomaly. Make sure the UI never tries to paint the editor. The UI currently uses
   * different techniques to paint the renderers and editors and overriding setBounds() below is not the right thing to
   * do for an editor. Returning -1 for the editing row in this case, ensures the editor is never painted.
   * {@inheritDoc}
   */
  @Override
  public int getEditingRow() {
    return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;
  }

  /**
   * Overridden to pass the new rowHeight to the tree.
   * {@inheritDoc}
   */
  @Override
  public void setRowHeight(final int rowHeight) {
    super.setRowHeight(rowHeight);
    if ((tree != null) && (tree.getRowHeight() != rowHeight)) tree.setRowHeight(getRowHeight());
  }

  /**
   * Returns the tree that is being shared between the model.
   * @return a {@link JTree} instance.
   */
  public JTree getTree() {
    return tree;
  }

  /**
   * Set the selection mode of this tree table.
   * @param selectionMode - one of ListSelectionModel.SINGLE_SELECTION, ListSelectionModel.SINGLE_INTERVAL_SELECTION, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION.
   */
  @Override
  public void setSelectionMode(final int selectionMode) {
    super.setSelectionMode(selectionMode);
    int treeMode;
    if (selectionMode == ListSelectionModel.SINGLE_SELECTION) treeMode = TreeSelectionModel.SINGLE_TREE_SELECTION;
    else if (selectionMode == ListSelectionModel.SINGLE_INTERVAL_SELECTION) treeMode = TreeSelectionModel.CONTIGUOUS_TREE_SELECTION;
    else treeMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
    tree.getSelectionModel().setSelectionMode(treeMode);
  }

  /**
   * A TreeCellRenderer that displays a JTree.
   */
  public class TreeTableCellRenderer extends JTree implements TableCellRenderer {
    /**
     * Last table/tree row asked to renderer.
     */
    protected int visibleRow;

    /**
     * Initialize this renderer with the specified tree model.
     * @param model a {@link TreeModel} instance.
     */
    public TreeTableCellRenderer(final TreeModel model) {
      super(model);
      setUI(new CustomTreeUI(JTreeTable.this));
      setOpaque(false);
    }

    /**
     * Overridden to set the colors of the Tree's renderer to match that of the table.
     */
    @Override
    public void updateUI() {
      super.updateUI();
      // Make the tree's cell renderer use the table's cell selection colors.
      final TreeCellRenderer tcr = getCellRenderer();
      if (tcr instanceof DefaultTreeCellRenderer) {
        final DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
        // For 1.1 uncomment this, 1.2 has a bug that will cause an exception to be thrown if the border selection color is null.
        // dtcr.setBorderSelectionColor(null);
        dtcr.setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));
        dtcr.setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));
      }
    }

    /**
     * Sets the row height of the tree, and forwards the row height to the table.
     * {@inheritDoc}
     */
    @Override
    public void setRowHeight(final int rowHeight) {
      if (rowHeight > 0) {
        super.setRowHeight(rowHeight);
        if (JTreeTable.this.getRowHeight() != rowHeight)  JTreeTable.this.setRowHeight(getRowHeight());
      }
    }

    /**
     * This is overridden to set the height to match that of the JTable.
     * {@inheritDoc}
     */
    @Override
    public void setBounds(final int x, final int y, final int w, final int h) {
      super.setBounds(x, 0, w, JTreeTable.this.getHeight());
    }

    /**
     * Subclassed to translate the graphics such that the last visible row will be drawn at 0,0.
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics g) {
      g.translate(0, -visibleRow * getRowHeight());
      try {
        super.paint(g);
      } catch(final Exception e) {
        log.debug(e.getMessage(), e);
      }
    }

    /**
     * TreeCellRenderer method. Overridden to update the visible row.
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
      if (isSelected) setBackground(table.getSelectionBackground());
      else setBackground(table.getBackground());
      visibleRow = row;
      return this;
    }
  }

  /**
   * TreeTableCellEditor implementation. Component returned is the JTree.
   */
  public class TreeTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int r, final int c) {
      return tree;
    }

    /**
     * Overridden to return false, and if the event is a mouse event it is forwarded to the tree.
     * <p>The behavior for this is debatable, and should really be offered as a property. By returning false, all keyboard
     * actions are implemented in terms of the table. By returning true, the tree would get a chance to do something
     * with the keyboard events. For the most part this is ok. But for certain keys, such as left/right, the tree will
     * expand/collapse where as the table focus should really move to a different column. Page up/down should also be
     * implemented in terms of the table. By returning false this also has the added benefit that clicking outside of
     * the bounds of the tree node, but still in the tree column will select the row, whereas if this returned true that wouldn't be the case.
     * <p>By returning false we are also enforcing the policy that the tree will never be editable (at least by a key sequence).
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final EventObject e) {
      if (e instanceof MouseEvent) {
        for (int counter = getColumnCount() - 1; counter >= 0; counter--) {
          if (getColumnClass(counter) == TreeTableModel.class) {
            final MouseEvent me = (MouseEvent) e;
            if (me.isControlDown()) return false;
            final int x = me.getX() - getCellRect(0, counter, true).x;
            final int row = JTreeTable.this.rowAtPoint(me.getPoint());
            final int y = me.getY() - (row * JTreeTable.this.getIntercellSpacing().height);
            final MouseEvent newME = new MouseEvent(tree, me.getID(), me.getWhen(), me.getModifiers(), x, y, me.getClickCount(), me.isPopupTrigger());
            tree.dispatchEvent(newME);
            break;
          }
        }
      }
      return false;
    }
  }

  /**
   * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel to listen for changes in the ListSelectionModel
   * it maintains. Once a change in the ListSelectionModel happens, the paths are updated in the DefaultTreeSelectionModel.
   */
  class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
    /**
     * Set to true when we are updating the ListSelectionModel.
     */
    protected boolean updatingListSelectionModel;
    /**
     *
     */
    protected boolean updatingTreeSelectionModel;

    /**
     * Initialize this selection model.
     */
    public ListToTreeSelectionModelWrapper() {
      super();
      getListSelectionModel().addListSelectionListener(createListSelectionListener());
      getTree().getSelectionModel().addTreeSelectionListener(new TreeSelectionHandler());
    }

    /**
     * Returns the list selection model. ListToTreeSelectionModelWrapper listens for changes to this model and updates
     * the selected paths accordingly.
     * @return a {@link ListSelectionModel} instance.
     */
    ListSelectionModel getListSelectionModel() {
      return listSelectionModel;
    }

    /**
     * This is overridden to set <code>updatingListSelectionModel</code> and message super. This is the only place
     * DefaultTreeSelectionModel alters the ListSelectionModel.
     */
    @Override
    public void resetRowSelection() {
      if (!updatingListSelectionModel) {
        updatingListSelectionModel = true;
        try {
          super.resetRowSelection();
        } finally {
          updatingListSelectionModel = false;
        }
      }
      // Notice how we don't message super if updatingListSelectionModel is true. If updatingListSelectionModel is true,
      // it implies the ListSelectionModel has already been updated and the paths are the only thing that needs to be updated.
    }

    /**
     * Creates and returns an instance of ListSelectionHandler.
     * @return a {@link ListSelectionListener} instance.
     */
    protected ListSelectionListener createListSelectionListener() {
      return new ListSelectionHandler();
    }

    /**
     * If <code>updatingListSelectionModel</code> is false, this will reset the selected paths from the selected rows in the list selection model.
     */
    protected void updateSelectedPathsFromSelectedRows() {
      if (!updatingListSelectionModel && !updatingTreeSelectionModel) {
        updatingListSelectionModel = true;
        try {
          // This is way expensive, ListSelectionModel needs an enumerator for iterating.
          final int min = listSelectionModel.getMinSelectionIndex();
          final int max = listSelectionModel.getMaxSelectionIndex();
          clearSelection();
          if (min != -1 && max != -1) {
            for (int counter = min; counter <= max; counter++) {
              if (listSelectionModel.isSelectedIndex(counter)) {
                final TreePath selPath = tree.getPathForRow(counter);
                if (selPath != null) addSelectionPath(selPath);
              }
            }
          }
        } finally {
          updatingListSelectionModel = false;
        }
      }
    }

    /**
     * If <code>updatingListSelectionModel</code> is false, this will reset the selected paths from the selected rows in
     * the list selection model.
     */
    protected void updateSelectedTableRows() {
      if (!updatingListSelectionModel && !updatingTreeSelectionModel) {
        System.out.println("in updateSelectedTableRows()");
        updatingTreeSelectionModel = true;
        try {
          final TreeSelectionModel treeSelectionModel = getTree().getSelectionModel();
          final int[] rows = treeSelectionModel.getSelectionRows();
          if ((rows == null) || (rows.length == 0)) listSelectionModel.clearSelection();
          else {
            final Set<Integer> selectionSet = new HashSet<>();
            for (final int r: rows) selectionSet.add(r);
            for (int i=0; i<JTreeTable.this.getRowCount(); i++) {
              final boolean sel = listSelectionModel.isSelectedIndex(i);
              if (selectionSet.contains(i)) {
                if (!sel) listSelectionModel.addSelectionInterval(i, i);
              } else {
                if (sel) listSelectionModel.removeSelectionInterval(i, i);
              }
            }
          }
        } finally {
          updatingTreeSelectionModel = false;
        }
      }
    }

    /**
     * Class responsible for calling updateSelectedPathsFromSelectedRows when the selection of the list changes.
     */
    class ListSelectionHandler implements ListSelectionListener {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        updateSelectedPathsFromSelectedRows();
      }
    }

    /**
     *
     */
    class TreeSelectionHandler implements TreeSelectionListener {
      @Override
      public void valueChanged(final TreeSelectionEvent e) {
        updateSelectedTableRows();
      }
    }
  }

  /**
   * A table header providing customizable tooltips.
   */
  class TreeTableHeader extends JTableHeader {
    /**
     * Default constructor.
     * @param tcm .
     */
    public TreeTableHeader(final TableColumnModel tcm) {
      super(tcm);
    }

    @Override
    public String getToolTipText(final MouseEvent event) {
      final Point p = event.getPoint();
      final int index = getColumnModel().getColumnIndexAtX(p.x);
      final int realIndex = getColumnModel().getColumn(index).getModelIndex();
      final TreeTableModelAdapter model = (TreeTableModelAdapter) getModel();
      return model.treeTableModel.getColumnTooltip(realIndex);
    }
  }

  @Override
  protected JTableHeader createDefaultTableHeader() {
    return new TreeTableHeader(getColumnModel());
  }
}
