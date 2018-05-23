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

import java.text.*;
import java.util.Locale;

import javax.swing.tree.*;

import org.jppf.utils.LocalizationUtils;
import org.slf4j.*;

/**
 * Abstract tree table model implementation for tree table-based options.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFTreeTableModel extends AbstractTreeTableModel {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFTreeTableModel.class);
  /**
   * Base name for localization bundle lookups.
   */
  protected String i18nBase = null;
  /**
   * The locale used to translate coumn headers and cell values.
   */
  protected transient Locale locale;
  /**
   * Formatter for cells that contain integer numbers.
   */
  protected final NumberFormat nfInt;
  /**
   * Formatter for cells that contain decimal numbers.
   */
  protected final NumberFormat nfDec;
  /**
   * Formatter for cells that contain decimal numbers.
   */
  protected final NumberFormat nfMB;

  /**
   * Initialize this model with the specified tree root.
   * @param root the root of the tree.
   */
  public AbstractJPPFTreeTableModel(final TreeNode root) {
    this(root, Locale.getDefault());
  }

  /**
   * Initialize this model with the specified tree root.
   * @param root the root of the tree.
   * @param locale the locale used to translate coumn headers and cell values.
   */
  public AbstractJPPFTreeTableModel(final TreeNode root, final Locale locale) {
    super(root);
    this.locale = locale;
    nfInt = createNumberFormat();
    nfDec = createDecimalNumberFormat();
    nfMB = createMBFormat();
  }

  /**
   * Tells if a cell can be edited.
   * @param node not used.
   * @param column not used.
   * @return true if the cell can be edited, false otherwise.
   */
  @Override
  public boolean isCellEditable(final Object node, final int column) {
    return super.isCellEditable(node, column);
  }

  /**
   * Called when done editing a cell. This method has an empty implementation and does nothing.
   * @param value not used.
   * @param node not used.
   * @param column not used.
   */
  @Override
  public void setValueAt(final Object value, final Object node, final int column) {
  }

  /**
   * Return the child at the specified index from the specified parent node.
   * @param parent the parent to get the child from.
   * @param index the index at which to get the child
   * @return the child node, or null if the index is not valid.
   */
  @Override
  public Object getChild(final Object parent, final int index) {
    try {
      return ((TreeNode) parent).getChildAt(index);
    } catch (final Exception e) {
      log.debug(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Get the number of children for the specified node.
   * @param parent the node for which to get the number of children.
   * @return the number of children as an int.
   */
  @Override
  public int getChildCount(final Object parent) {
    return (parent == null) ? 0 : ((TreeNode) parent).getChildCount();
  }

  /**
   * Insert the specified child into the specified parent's list of children at the specified position.
   * @param child the node to insert into the parent.
   * @param parent the node into which to insert the child.
   * @param pos the position at which to insert the node.
   */
  public void insertNodeInto(final DefaultMutableTreeNode child, final DefaultMutableTreeNode parent, final int pos) {
    parent.insert(child, pos);
    fireTreeNodesInserted(parent, parent.getPath(), new int[] { pos }, new Object[] { child } );
  }

  /**
   * Remove a node from the tree.
   * @param node the node to remove from the parent.
   */
  public void removeNodeFromParent(final DefaultMutableTreeNode node) {
    final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
    final int pos = parent.getIndex(node);
    parent.remove(node);
    fireTreeNodesRemoved(parent, parent.getPath(), new int[] { pos }, new Object[] { node } );
  }

  /**
   * Handle a node update.
   * @param node the node to update.
   */
  public void changeNode(final DefaultMutableTreeNode node) {
    final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
    final int pos = parent.getIndex(node);
    fireTreeNodesChanged(parent, parent.getPath(), new int[] { pos }, new Object[] { node } );
  }

  /**
   * Determine the class of th specified column.
   * @param column - the column index.
   * @return a <code>Class</code> instance.
   */
  @Override
  public Class<?> getColumnClass(final int column) {
    return (column == 0) ? TreeTableModel.class : String.class;
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message the unique name of the localized message.
   * @return a message in the current locale, or the default locale if the localization for the current locale is not found.
   */
  protected String localize(final String message) {
    return LocalizationUtils.getLocalized(getI18nBase(), message, locale);
  }

  @Override
  public String getColumnName(final int column) {
    if ((column >= 0) && (column <= getColumnCount())) {
      return localize(getBaseColumnName(column) + ".label");
    }
    return "";
  }

  @Override
  public String getColumnTooltip(final int column) {
    if ((column >= 0) && (column <= getColumnCount())) {
      String s = localize(getBaseColumnName(column) + ".tooltip");
      if (s.contains("\n")) s = "<html>" + s.replace("\n", "<br>") + "</html>";
      return s;
    }
    return "";
  }

  /**
   * Get the non-localized name of the specified column.
   * @param column the index of the column for which to get a name.
   * @return the column name as a string.
   */
  public abstract String getBaseColumnName(final int column);

  /**
   * Get a number formatter for the number of tasks for each node.
   * @return a {@code NumberFormat} instance.
   */
  protected NumberFormat createNumberFormat() {
    final NumberFormat nf = NumberFormat.getIntegerInstance(locale);
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(0);
    return nf;
  }

  /**
   * Get a number formatter for the number of tasks for each node.
   * @return a {@code NumberFormat} instance.
   */
  protected NumberFormat createDecimalNumberFormat() {
    final NumberFormat nf = DecimalFormat.getIntegerInstance(locale);
    nf.setGroupingUsed(true);
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(1);
    return nf;
  }

  /**
   * Get a number formatter for values expressed in MB.
   * @return a {@code NumberFormat} instance.
   */
  protected NumberFormat createMBFormat() {
    final NumberFormat nf = DecimalFormat.getIntegerInstance(locale);
    nf.setGroupingUsed(true);
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(1);
    return nf;
  }

  /**
   * @return the base name for localization bundle lookups.
   */
  public String getI18nBase() {
    return i18nBase;
  }
}
