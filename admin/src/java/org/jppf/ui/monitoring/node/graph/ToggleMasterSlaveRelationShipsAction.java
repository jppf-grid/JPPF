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

package org.jppf.ui.monitoring.node.graph;

import java.awt.event.*;

import javax.swing.AbstractButton;

import org.jppf.ui.monitoring.node.MasterSlaveRelationShipsHandler;
import org.jppf.ui.monitoring.node.actions.AbstractTopologyAction;
import org.jppf.ui.options.OptionElement;

/**
 * Action performed to toggle the auto-layout on the graph graph.
 * @author Laurent Cohen
 */
public class ToggleMasterSlaveRelationShipsAction extends AbstractTopologyAction {
  /**
   * 
   */
  private static final String BTN_NAME = "toggle.master.slave";
  /**
   * The toggle button.
   */
  private AbstractButton button = null;
  /**
   * 
   */
  private final MasterSlaveRelationShipsHandler panel;

  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the graph panel to which this action applies.
   */
  public ToggleMasterSlaveRelationShipsAction(final MasterSlaveRelationShipsHandler panel) {
    this.panel = panel;
    setupIcon("/org/jppf/ui/resources/sparkle.png");
    setupNameAndTooltip(BTN_NAME + ".on");
    //setupTooltip(BTN_NAME + ".on");
    button = (AbstractButton) ((OptionElement) panel).findFirstWithName("/" + BTN_NAME).getUIComponent();
    button.setSelected(true);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    synchronized (panel) {
      final AbstractButton button = (AbstractButton) ((OptionElement) panel).findFirstWithName("/" + BTN_NAME).getUIComponent();
      final boolean selected = button.isSelected();
      panel.setShowMasterSlaveRelationShip(selected);
      setupNameAndTooltip(BTN_NAME + "." + (selected ? "on" : "off"));
    }
  }
}
