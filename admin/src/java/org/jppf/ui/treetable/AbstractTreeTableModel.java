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
/*
 * %W% %E%
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */
package org.jppf.ui.treetable;

import javax.swing.event.*;
import javax.swing.tree.TreePath;

import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * An abstract implementation of the TreeTableModel interface, handling the list of listeners.
 * 
 * @version %I% %G%
 * 
 * @author Philip Milne
 */

public abstract class AbstractTreeTableModel implements TreeTableModel {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractTreeTableModel.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The tree root.
   */
  protected Object root;

  /**
   * The list of event listeners for this tree table model.
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Initialize this model with the specified tree root.
   * @param root the root of the tree.
   */
  public AbstractTreeTableModel(final Object root) {
    this.root = root;
  }

  //
  // Default implementations for methods in the TreeModel interface.
  //

  @Override
  public Object getRoot() {
    return root;
  }

  @Override
  public boolean isLeaf(final Object node) {
    return getChildCount(node) == 0;
  }

  @Override
  public void valueForPathChanged(final TreePath path, final Object newValue) {
  }

  @Override
  public int getIndexOfChild(final Object parent, final Object child) {
    // This is not called in the JTree's default mode: use a naive implementation.
    for (int i = 0; i < getChildCount(parent); i++) {
      if (getChild(parent, i).equals(child)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void addTreeModelListener(final TreeModelListener l) {
    listenerList.add(TreeModelListener.class, l);
  }

  @Override
  public void removeTreeModelListener(final TreeModelListener l) {
    listenerList.remove(TreeModelListener.class, l);
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @param source the source of the event.
   * @param path the path of the parent node whose children have changed.
   * @param childIndices the indices of the children that changed.
   * @param children an array of the children that changed.
   * @see EventListenerList
   */
  protected void fireTreeNodesChanged(final Object source, final Object[] path, final int[] childIndices, final Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent event = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (event == null) event = new TreeModelEvent(source, path, childIndices, children);
        try {
          ((TreeModelListener) listeners[i + 1]).treeNodesChanged(event);
        } catch (Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @param source the source of the event.
   * @param path the path of the parent node whose children have changed.
   * @param childIndices the indices of the children that were inserted.
   * @param children an array of the children that were inserted.
   * @see EventListenerList
   */
  protected void fireTreeNodesInserted(final Object source, final Object[] path, final int[] childIndices, final Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @param source the source of the event.
   * @param path the path of the parent node whose children have changed.
   * @param childIndices the indices of the children that were removed.
   * @param children an array of the children that were removed.
   * @see EventListenerList
   */
  protected void fireTreeNodesRemoved(final Object source, final Object[] path, final int[] childIndices, final Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        try {
          if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
          ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
        } catch(Exception ex) {
          log.error(ex.getMessage(), ex);
        }
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for notification on this event type. The event instance is
   * lazily created using the parameters passed into the fire method.
   * @param source the source of the event.
   * @param path the path of the parent node whose children have changed.
   * @param childIndices the indices of the children that have changed.
   * @param children an array of the children that changed.
   * @see EventListenerList
   */
  protected void fireTreeStructureChanged(final Object source, final Object[] path, final int[] childIndices, final Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
        ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
      }
    }
  }

  //
  // Default implementations for methods in the TreeTableModel interface.
  //

  @Override
  public Class getColumnClass(final int column) {
    return Object.class;
  }

  /**
   * By default, make the column with the Tree in it the only editable one. Making this column editable causes the
   * JTable to forward mouse and keyboard events in the Tree column to the underlying JTree.
   * {@inheritDoc}
   */
  @Override
  public boolean isCellEditable(final Object node, final int column) {
    return getColumnClass(column) == TreeTableModel.class;
  }

  @Override
  public void setValueAt(final Object aValue, final Object node, final int column) {
  }

  @Override
  public String getColumnTooltip(final int column) {
    return null;
  }

  // Left to be implemented in the subclass:

  /*
   * public Object getChild(Object parent, int index) public int getChildCount(Object parent) public int
   * getColumnCount() public String getColumnName(Object node, int column) public Object getValueAt(Object node, int
   * column)
   */
}
