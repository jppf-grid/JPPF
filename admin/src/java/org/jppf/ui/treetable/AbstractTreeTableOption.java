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

import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.ui.actions.*;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Abstract implementation of a tree table-based option.
 * @author Laurent Cohen
 */
public abstract class AbstractTreeTableOption extends AbstractOption implements ActionHolder {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractTreeTableOption.class);
  /**
   * Base name for localization bundle lookups.
   */
  protected String BASE;
  /**
   * The tree table model associated with the tree table.
   */
  protected transient AbstractJPPFTreeTableModel model;
  /**
   * The root of the tree model.
   */
  protected DefaultMutableTreeNode treeTableRoot;
  /**
   * A tree table component displaying the driver and nodes information.
   */
  protected JPPFTreeTable treeTable;
  /**
   * Handles all actions in toolbars or popup menus.
   */
  protected JTreeTableActionHandler actionHandler;
  /**
   * Mapping of hidden columns to their position in the tree table model.
   */
  private final Map<Integer, TableColumn> columnsMap = new TreeMap<>();
  /**
   * Positions of the visible table columns represented by their index in the tree table model.
   */
  private final List<Integer> visibleColumnIndexes = new ArrayList<>();

  /**
   * Get the object that handles all actions in toolbars or popup menus.
   * @return a <code>JTreeTableActionHandler</code> instance.
   */
  @Override
  public JTreeTableActionHandler getActionHandler() {
    return actionHandler;
  }

  /**
   * Get the tree table component displaying the driver and nodes information.
   * @return a <code>JPPFTreeTable</code> instance.
   */
  public JPPFTreeTable getTreeTable() {
    return treeTable;
  }

  /**
   * Not implemented.
   * @param enabled not used.
   * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
   */
  @Override
  public void setEnabled(final boolean enabled) {
  }

  /**
   * Not implemented.
   * @param enabled not used.
   * @see org.jppf.ui.options.OptionElement#setEventsEnabled(boolean)
   */
  @Override
  public void setEventsEnabled(final boolean enabled) {
  }

  /**
   * Not implemented.
   * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
   */
  @Override
  protected void setupValueChangeNotifications() {
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message - the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  protected String localize(final String message) {
    return LocalizationUtils.getLocalized(BASE, message);
  }

  /**
   * Set the columns width based on values stored as preferences.
   */
  public void setupTableColumns() {
    for (int i=1; i<treeTable.getColumnCount(); i++) columnsMap.put(i, treeTable.getColumnModel().getColumn(i)); 
    final Preferences pref = OptionsHandler.getPreferences();
    String key = getName() + "_column_widths";
    String s = pref.get(key, null);
    if (s != null) {
      final String[] wStr = RegexUtils.SPACES_PATTERN.split(s);
      for (int i=0; i<Math.min(treeTable.getColumnCount(), wStr.length); i++) {
        int width = 60;
        try {
          width = Integer.valueOf(wStr[i]);
        } catch(final NumberFormatException e) {
          log.debug(e.getMessage(), e);
        }
        treeTable.getColumnModel().getColumn(i).setPreferredWidth(width);
      }
    }
    key = getName() + "_visible_columns";
    s = pref.get(key, null);
    final List<Integer> visibleIndexes = new ArrayList<>();
    if (s != null) {
      final String[] posStr = RegexUtils.SPACES_PATTERN.split(s);
      for (String str: posStr) {
        int pos = -1;
        try {
          pos = Integer.valueOf(str);
        } catch(final NumberFormatException e) {
          log.debug(e.getMessage(), e);
        }
        if (pos > 0) visibleIndexes.add(pos);
      }
    }
    if (!visibleIndexes.isEmpty()) restoreColumns(visibleIndexes);
    else visibleColumnIndexes.addAll(columnsMap.keySet());
  }

  /**
   * Set the columns width based on values stored as preferences.
   */
  public void saveTableColumnsSettings() {
    final Preferences pref = OptionsHandler.getPreferences();
    String key = getName() + "_column_widths";
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<treeTable.getColumnCount(); i++) {
      final int width = treeTable.getColumnModel().getColumn(i).getPreferredWidth();
      if (i > 0) sb.append(' ');
      sb.append(width);
    }
    pref.put(key, sb.toString());
    key = getName() + "_visible_columns";
    sb = new StringBuilder();
    int count = 0;
    for (final int index: visibleColumnIndexes) {
      if (count > 0) sb.append(' ');
      sb.append(index);
      count++;
    }
    pref.put(key, sb.toString());
  }

  /**
   * Get the root of the tree model.
   * @return a {@link DefaultMutableTreeNode} instance.
   */
  public DefaultMutableTreeNode getTreeTableRoot() {
    return treeTableRoot;
  }

  /**
   * get the tree table model associated with the tree table.
   * @return an {@link AbstractJPPFTreeTableModel} instance.
   */
  public AbstractJPPFTreeTableModel getModel() {
    return model;
  }

  /**
   * Restore the previously hidden columns with the specified positions in the tree table model.
   * @param positions the positions of the columns position in the tree table model (<i>not</i> the table column model).
   */
  public void restoreColumns(final Collection<Integer> positions) {
    final TableColumnModel tcm = treeTable.getColumnModel();
    final List<TableColumn> columns = new ArrayList<>();
    for (int i=1; i<tcm.getColumnCount(); i++) columns.add(tcm.getColumn(i));
    for (final TableColumn column: columns) tcm.removeColumn(column);
    visibleColumnIndexes.clear();
    for (final int index: positions) {
      final TableColumn column = columnsMap.get(index);
      if (column != null) {
        tcm.addColumn(column);
        visibleColumnIndexes.add(index);
      }
    }
  }

  /**
   * Determine whether the column with the specified position in the tree table model is hidden.
   * @param pos the column position in the tree table model (<i>not</i> the table column model).
   * @return {@code true} if the column is hidden, {@code false} otherwise.
   */
  public boolean isColumnHidden(final int pos) {
    return !visibleColumnIndexes.contains(pos);
  }

  /**
   * @return the positions of the visible table columns represented by their index in the tree table model.
   */
  public List<Integer> getVisibleColumnIndexes() {
    return visibleColumnIndexes;
  }
}
