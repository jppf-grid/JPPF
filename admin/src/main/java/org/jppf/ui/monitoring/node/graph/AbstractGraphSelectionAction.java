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

import java.util.Collection;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.ui.monitoring.node.actions.AbstractTopologyAction;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * Abstract superclass for actions that select nodes in the tree table.
 * @author Laurent Cohen
 */
public abstract class AbstractGraphSelectionAction extends AbstractTopologyAction {
  /**
   * The tree table panel to which this action applies.
   */
  protected final GraphOption panel;

  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the tree table panel to which this action applies.
   */
  public AbstractGraphSelectionAction(final GraphOption panel) {
    super();
    this.panel = panel;
    setEnabled(true);
  }

  /**
   * Get the list of all tree nodes representing a driver.
   * @return an list of driver cells.
   */
  protected Collection<AbstractTopologyComponent> getVertices() {
    final VisualizationViewer<AbstractTopologyComponent, Number> viewer = panel.getViewer();
    final SparseMultigraph<AbstractTopologyComponent, Number> graph = (SparseMultigraph<AbstractTopologyComponent, Number>) viewer.getGraphLayout().getGraph();
    return graph.getVertices();
  }
}
