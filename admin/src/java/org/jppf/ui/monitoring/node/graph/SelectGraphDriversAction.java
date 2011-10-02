/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.awt.event.ActionEvent;

import com.mxgraph.model.*;
import com.mxgraph.view.mxGraph;

/**
 * Action performed to select all drivers in the topology view.
 * @author Laurent Cohen
 */
public class SelectGraphDriversAction extends AbstractGraphSelectionAction
{
	/**
	 * Initialize this action with the specified tree table panel.
	 * @param panel the tree table panel to which this action applies.
	 */
	public SelectGraphDriversAction(GraphOption panel)
	{
		super(panel);
		setupIcon("/org/jppf/ui/resources/select_drivers.gif");
		setupNameAndTooltip("select.drivers");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void actionPerformed(ActionEvent e)
	{
		synchronized(panel)
		{
			mxGraph graph = panel.getGraph();
			mxGraphModel model = (mxGraphModel) graph.getModel();
			model.beginUpdate();
			try
			{
				Object[] drivers = getDriverVertices();
				if ((drivers == null) || (drivers.length == 0)) return;
				graph.setSelectionCells(drivers);
			}
			finally
			{
				model.endUpdate();
			}
		}
	}
}
