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

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.GuiUtils;

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
   * Contains all the checkboxes for the oclumns to hide or show.
   */
  private final List<JCheckBox> checkboxes = new ArrayList<>();

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
    AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    thisPanel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/VisibleColumnsPanel.xml");
    
    JComponent columnsPanel = null;
    JComponent comp = OptionsHandler.findOptionWithName(thisPanel, "/show.hide.columns").getUIComponent();
    if (comp instanceof JScrollPane) columnsPanel = (JComponent) ((JScrollPane) comp).getViewport().getView();
    else columnsPanel = comp;
    TreeTableModel model = treeTableOption.getModel();
    checkboxes.clear();
    for (int i=0; i<model.getColumnCount(); i++) {
      JCheckBox checkbox = new JCheckBox(model.getColumnName(i));
      if (i == 0) {
        checkbox.setSelected(true);
        checkbox.setEnabled(false);
      } else checkbox.setSelected(!treeTableOption.isColumnHidden(i));
      checkboxes.add(checkbox);
      columnsPanel.add(checkbox);
    }

    final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("show.hide.label"), false);
    dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/table-column-hide.png").getImage());
    JButton applyBtn = (JButton) thisPanel.findFirstWithName("/show.hide.apply").getUIComponent();
    AbstractAction applyAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        doApply();
      }
    };
    applyBtn.addActionListener(applyAction);
    JButton closeBtn = (JButton) thisPanel.findFirstWithName("/show.hide.close").getUIComponent();
    AbstractAction closeAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        dialog.setVisible(false);
        dialog.dispose();
        checkboxes.clear();
      }
    };
    closeBtn.addActionListener(closeAction);
    setOkCancelKeys(thisPanel, applyAction, closeAction);
    JButton selectAllBtn = (JButton) thisPanel.findFirstWithName("/show.hide.select.all").getUIComponent();
    selectAllBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        for (JCheckBox checkbox: checkboxes) if (checkbox.isEnabled()) checkbox.setSelected(true);
      }
    });
    JButton unselectAllBtn = (JButton) thisPanel.findFirstWithName("/show.hide.unselect.all").getUIComponent();
    unselectAllBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        for (JCheckBox checkbox: checkboxes) if (checkbox.isEnabled()) checkbox.setSelected(false);
      }
    });
    dialog.getContentPane().add(thisPanel.getUIComponent());
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    if (location != null) dialog.setLocation(location);
    dialog.setVisible(true);
  }

  /**
   * Perform the apply action.
   */
  private void doApply() {
    for (int i=1; i<checkboxes.size(); i++) {
      JCheckBox checkbox = checkboxes.get(i);
      if (checkbox.isSelected()) {
        if (treeTableOption.isColumnHidden(i)) treeTableOption.restoreColumn(i);
      } else {
        if (!treeTableOption.isColumnHidden(i)) treeTableOption.hideColumn(i);
      }
    }
  }
}
