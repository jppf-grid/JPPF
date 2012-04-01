/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.*;

import javax.swing.tree.TreePath;

import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.monitoring.node.actions.AbstractTopologyAction;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * Abstract superclass for actions that select nodes in the tree table.
 * @author Laurent Cohen
 */
public abstract class AbstractGraphSelectionAction extends AbstractTopologyAction
{
  /**
   * Constant for an empty <code>TreePath</code>.
   */
  protected static final TreePath[] EMPTY_TREE_PATH = new TreePath[0];
  /**
   * The tree table panel to which this action applies.
   */
  protected final GraphOption panel;

  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the tree table panel to which this action applies.
   */
  public AbstractGraphSelectionAction(final GraphOption panel)
  {
    this.panel = panel;
    setEnabled(true);
  }

  /**
   * Get the list of all tree nodes representing a driver.
   * @return an list of driver cells.
   */
  protected Collection<TopologyData> getVertices()
  {
    VisualizationViewer<TopologyData, Number> viewer = panel.getViewer();
    SparseMultigraph<TopologyData, Number> graph = (SparseMultigraph<TopologyData, Number>) viewer.getGraphLayout().getGraph();
    return graph.getVertices();
  }
}
