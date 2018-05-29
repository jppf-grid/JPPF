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

package org.jppf.ui.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

import org.jppf.ui.monitoring.LocalizedListItem;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.picklist.PickList;
import org.jppf.ui.treetable.*;
import org.jppf.utils.LocalizationUtils;

/**
 * Abstract superclass for all actions in the topology panel.
 * @author Laurent Cohen
 */
public class ShowHideColumnsAction extends AbstractUpdatableAction {
  /**
   * The option holding the tree table.
   */
  private final AbstractTreeTableOption treeTableOption;
  /**
   * Panel containing the dialog for entering the number of threads and their priority.
   */
  private OptionElement thisPanel = null;

  /**
   * Initialize this action.
   * @param treeTableOption the option holding the tree table.
   */
  public ShowHideColumnsAction(final AbstractTreeTableOption treeTableOption) {
    BASE = "org.jppf.ui.i18n.VisibleColumnsPage";
    this.treeTableOption = treeTableOption;
    setupIcon("/org/jppf/ui/resources/table-column-hide.png");
    setupNameAndTooltip("show.hide");
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final AbstractButton btn = (AbstractButton) event.getSource();
    location = ((btn != null) && btn.isShowing()) ? location = btn.getLocationOnScreen() : new Point(0, 0);
    thisPanel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/VisibleStatsPanel.xml");
    final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), LocalizationUtils.getLocalized(BASE, "visible.columns.panel.label"), false);
    final PickListOption option = (PickListOption) thisPanel.findFirstWithName("visible.stats.selection");
    final PickList<Object> pickList = option.getPickList();
    pickList.setLeftTitle(LocalizationUtils.getLocalized(BASE, "visible.columns.left.title"));
    pickList.setRightTitle(LocalizationUtils.getLocalized(BASE, "visible.columns.right.title"));
    final AbstractJPPFTreeTableModel model = treeTableOption.getModel();
    final List<LocalizedListItem> allItems = new ArrayList<>();
    final List<LocalizedListItem> visibleItems = new ArrayList<>();
    for (int i=1; i<model.getColumnCount(); i++) {
      final LocalizedListItem item = new LocalizedListItem(model.getBaseColumnName(i), i, model.getColumnName(i), model.getColumnTooltip(i));
      allItems.add(item);
    }
    final List<Integer> visibleIndexes = treeTableOption.getVisibleColumnIndexes();
    for (int i=0; i<visibleIndexes.size(); i++) {
      final int index = visibleIndexes.get(i);
      //final LocalizedListItem item = new LocalizedListItem(model.getBaseColumnName(index), index, model.getColumnName(index), model.getColumnTooltip(index));
      final LocalizedListItem item = allItems.get(index - 1);
      visibleItems.add(item);
    }
    option.populate(new ArrayList<Object>(allItems), new ArrayList<Object>(visibleItems));
    final JButton applyBtn = (JButton) thisPanel.findFirstWithName("/visible.stats.apply").getUIComponent();
    final AbstractAction applyAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        final List<Object> picked = pickList.getPickedItems();
        final List<LocalizedListItem> visibleItems = (picked == null) ? new ArrayList<LocalizedListItem>() : new ArrayList<LocalizedListItem>(picked.size());
        for (final Object o: picked) visibleItems.add((LocalizedListItem) o);
        applyVisibleColumns(visibleItems);
      }
    };
    applyBtn.addActionListener(applyAction);
    final JButton closeBtn = (JButton) thisPanel.findFirstWithName("/visible.stats.close").getUIComponent();
    final AbstractAction closeAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        dialog.setVisible(false);
        dialog.dispose();
      }
    };
    closeBtn.addActionListener(closeAction);
    AbstractUpdatableAction.setOkCancelKeys(thisPanel, applyAction, closeAction);
    dialog.getContentPane().add(thisPanel.getUIComponent());
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    if (location != null) dialog.setLocation(location);
    dialog.setVisible(true);
  }

  /**
   * 
   * @param visibleItems the items selected in the pick list.
   */
  private void applyVisibleColumns(final List<LocalizedListItem> visibleItems) {
    final List<Integer> visiblePositions = new ArrayList<>(visibleItems.size());
    for (final LocalizedListItem item: visibleItems) visiblePositions.add(item.getIndex());
    treeTableOption.restoreColumns(visiblePositions);
  }
}
