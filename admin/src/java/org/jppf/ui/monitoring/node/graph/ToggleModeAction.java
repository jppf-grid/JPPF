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

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.*;

/**
 * Action performed to toggle the graph mode from selection to pan and vice versa.
 * @author Laurent Cohen
 */
public class ToggleModeAction extends AbstractGraphSelectionAction {
  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the graph panel to which this action applies.
   */
  public ToggleModeAction(final GraphOption panel) {
    super(panel);
    setupIcon("/org/jppf/ui/resources/task-active.gif");
    setupNameAndTooltip("graph.toggle.mode");
  }

  @Override
  @SuppressWarnings("unchecked")
  public void actionPerformed(final ActionEvent e) {
    synchronized(panel) {
      final AbstractButton button = (AbstractButton) panel.findFirstWithName("/graph.toggle.mode").getUIComponent();
      final VisualizationViewer<AbstractTopologyComponent, Number> viewer = panel.getViewer();
      final EditingModalGraphMouse<AbstractTopologyComponent, Number> graphMouse = (EditingModalGraphMouse<AbstractTopologyComponent, Number>) viewer.getGraphMouse();
      graphMouse.setMode(button.isSelected() ? ModalGraphMouse.Mode.TRANSFORMING : ModalGraphMouse.Mode.PICKING);
    }
  }
}
