/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import org.jppf.ui.monitoring.node.TopologyData;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.*;

/**
 * Action performed to toggle the graph mode from selection to pan and vice versa.
 * @author Laurent Cohen
 */
public class ToggleModeAction extends AbstractGraphSelectionAction
{
  /**
   * Listens to state changes for the toggle button.
   */
  private ItemListener itemListener = null;
  /**
   * The toggle button.
   */
  private AbstractButton button = null;

  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the graph panel to which this action applies.
   */
  public ToggleModeAction(final GraphOption panel)
  {
    super(panel);
    setupIcon("/org/jppf/ui/resources/task-active.gif");
    setupNameAndTooltip("graph.toggle.mode");
    button = (AbstractButton) panel.findFirstWithName("/graph.toggle.mode").getUIComponent();
    itemListener = new ItemListener()
    {
      @Override
      public void itemStateChanged(final ItemEvent e)
      {
        actionPerformed(null);
      }
    };
    button.addItemListener(itemListener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public void actionPerformed(final ActionEvent e)
  {
    synchronized(panel)
    {
      AbstractButton button = (AbstractButton) panel.findFirstWithName("/graph.toggle.mode").getUIComponent();
      VisualizationViewer<TopologyData, Number> viewer = panel.getViewer();
      EditingModalGraphMouse<TopologyData, Number> graphMouse = (EditingModalGraphMouse<TopologyData, Number>) viewer.getGraphMouse();
      graphMouse.setMode(button.isSelected() ? ModalGraphMouse.Mode.TRANSFORMING : ModalGraphMouse.Mode.PICKING);
      /*
      if (e != null)
      {
        button.removeItemListener(itemListener);
        button.setSelected(!button.isSelected());
        button.addItemListener(itemListener);
      }
      */
    }
  }
}
