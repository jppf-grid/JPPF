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
  protected String BASE = null;
  /**
   * The tree table model associated with the tree table.
   */
  protected transient AbstractJPPFTreeTableModel model = null;
  /**
   * The root of the tree model.
   */
  protected DefaultMutableTreeNode treeTableRoot = null;
  /**
   * A tree table component displaying the driver and nodes information.
   */
  protected JPPFTreeTable treeTable = null;
  /**
   * Handles all actions in toolbars or popup menus.
   */
  protected JTreeTableActionHandler actionHandler = null;
  /**
   * Mapping of hidden columns to their position in the tree table model.
   */
  private final Map<Integer, TableColumn> hiddenColumns = new TreeMap<>();

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
    Preferences pref = OptionsHandler.getPreferences();
    String key = getName() + "_column_widths";
    String s = pref.get(key, null);
    if (s != null) {
      String[] wStr = RegexUtils.SPACES_PATTERN.split(s);
      for (int i=0; i<Math.min(treeTable.getColumnCount(), wStr.length); i++) {
        int width = 60;
        try {
          width = Integer.valueOf(wStr[i]);
        } catch(NumberFormatException e) {
          log.debug(e.getMessage(), e);
        }
        treeTable.getColumnModel().getColumn(i).setPreferredWidth(width);
      }
    }
    key = getName() + "_hidden_columns";
    s = pref.get(key, null);
    if (s != null) {
      String[] posStr = RegexUtils.SPACES_PATTERN.split(s);
      for (String str: posStr) {
        int pos = -1;
        try {
          pos = Integer.valueOf(str);
        } catch(NumberFormatException e) {
          log.debug(e.getMessage(), e);
        }
        if (pos > 0) hideColumn(pos);
      }
    }
  }

  /**
   * Set the columns width based on values stored as preferences.
   */
  public void saveTableColumnsWidth() {
    Preferences pref = OptionsHandler.getPreferences();
    String key = getName() + "_column_widths";
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<treeTable.getColumnCount(); i++) {
      int width = treeTable.getColumnModel().getColumn(i).getPreferredWidth();
      if (i > 0) sb.append(' ');
      sb.append(width);
    }
    pref.put(key, sb.toString());
    key = getName() + "_hidden_columns";
    sb = new StringBuilder();
    int count = 0;
    for (int i=1; i<treeTable.getModel().getColumnCount(); i++) {
      if (count > 0) sb.append(' ');
      if (isColumnHidden(i)) sb.append(i);
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
   * Hide the column with the specified position in the tree table model.
   * @param pos the column position in the tree table model (<i>not</i> the table column model).
   */
  public void hideColumn(final int pos) {
    if ((pos < 0) || (pos >= model.getColumnCount()) || hiddenColumns.containsKey(pos)) return;
    TableColumn col = treeTable.getColumn(pos);
    if (col == null) return;
    treeTable.removeColumn(col);
    hiddenColumns.put(pos, col);
  }

  /**
   * Restore the previously hidden columns with the specified positions in the tree table model.
   * @param positions the positions of the columns position in the tree table model (<i>not</i> the table column model).
   */
  public void hideColumns(final Collection<Integer> positions) {
    for (int n: positions) hideColumn(n);
  }

  /**
   * Restore the previously hidden column with the specified position in the tree table model.
   * @param pos the column position in the tree table model (<i>not</i> the table column model).
   */
  public void restoreColumn(final int pos) {
    TableColumn col = hiddenColumns.remove(pos);
    if (col == null) return;
    treeTable.addColumn(col);
    TableColumnModel tcm = treeTable.getColumnModel();
    if (pos <= tcm.getColumnCount()) {
      int idx = tcm.getColumnIndex(pos);
      tcm.moveColumn(idx, pos);
    }
  }

  /**
   * Restore the previously hidden columns with the specified positions in the tree table model.
   * @param positions the positions of the columns position in the tree table model (<i>not</i> the table column model).
   */
  public void restoreColumns(final int[] positions) {
    Arrays.sort(positions);
    for (int n: positions) restoreColumn(n);
  }

  /**
   * Restore the previously hidden columns with the specified positions in the tree table model.
   * @param positions the positions of the columns position in the tree table model (<i>not</i> the table column model).
   */
  public void restoreColumns(final Collection<Integer> positions) {
    Set<Integer> set = new TreeSet<>(positions);
    for (int n: set) restoreColumn(n);
  }

  /**
   * Determine whether the column with the specified position in the tree table model is hidden.
   * @param pos the column position in the tree table model (<i>not</i> the table column model).
   * @return {@code true} if the column is hidden, {@code false} otherwise.
   */
  public boolean isColumnHidden(final int pos) {
    return hiddenColumns.containsKey(pos);
  }
}
