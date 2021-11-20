/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

/**
 * Action performed to toggle the auto-layout on the graph graph.
 * @author Laurent Cohen
 */
public class ToggleLayoutAction extends AbstractGraphSelectionAction {
  /**
   * The toggle button.
   */
  private AbstractButton button = null;

  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the graph panel to which this action applies.
   */
  public ToggleLayoutAction(final GraphOption panel) {
    super(panel);
    setupIcon("/org/jppf/ui/resources/layout.gif");
    setupNameAndTooltip("graph.toggle.layout.on");
    button = (AbstractButton) panel.findFirstWithName("/graph.toggle.layout").getUIComponent();
    button.setSelected(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void actionPerformed(final ActionEvent e) {
    synchronized (panel) {
      final AbstractButton button = (AbstractButton) panel.findFirstWithName("/graph.toggle.layout").getUIComponent();
      final boolean selected = button.isSelected();
      panel.setAutoLayout(selected);
      setupNameAndTooltip("graph.toggle.layout." + (selected ? "on" : "off"));
    }
  }
}
