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

package org.jppf.ui.monitoring.node.graph;

import java.awt.event.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.ui.actions.AbstractActionHandler;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.picking.MultiPickedState;

/**
 * Abstract implementation of the <code>ActionManager</code> interface for <code>mxGraph</code> components.
 * @author Laurent Cohen
 */
public class GraphActionHandler extends AbstractActionHandler
{
  /**
   * The JTreeTable whose actions are managed.
   */
  protected VisualizationViewer<AbstractTopologyComponent, Number> graph = null;

  /**
   * Initialize this action manager with the specified JTreeTable component.
   * @param graph the graph whose actions are managed.
   */
  public GraphActionHandler(final VisualizationViewer<AbstractTopologyComponent, Number> graph)
  {
    this.graph = graph;
    MultiPickedState<?> mps = (MultiPickedState<?>) graph.getPickedVertexState();
    mps.addItemListener(new ItemListener()
    {
      @Override
      public void itemStateChanged(final ItemEvent e)
      {
        computeSelectedElements();
        updateActions();
      }
    });
  }

  /**
   * Compute the list of elements selected in the component.
   */
  protected synchronized void computeSelectedElements()
  {
    selectedElements.clear();
    Object[] sel = ((MultiPickedState<AbstractTopologyComponent>) graph.getPickedVertexState()).getSelectedObjects();
    if ((sel == null) || (sel.length <= 0)) return;
    for (Object data: sel) selectedElements.add(data);
  }
}
