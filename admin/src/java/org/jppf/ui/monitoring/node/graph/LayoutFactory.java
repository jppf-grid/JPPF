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

import com.mxgraph.layout.*;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout;
import com.mxgraph.view.mxGraph;

/**
 * Factory class to create layouts for <code>mxGraph</code> instances.
 * @author Laurent Cohen
 */
class LayoutFactory
{
	/**
	 * The graph for which to create a layout.
	 */
	private mxGraph graph;

	/**
	 * Create a factory instance for the specified graph.
	 * @param graph the graph for which to create layouts.
	 */
	LayoutFactory(final mxGraph graph)
	{
		this.graph = graph;
	}

	/**
	 * Create a layout with the specified name.
	 * @param name the name of the layout to create.
	 * @return a <code>mxIGraphLayout</code> instance.
	 */
	mxIGraphLayout createLayout(final String name)
	{
		mxIGraphLayout layout;
		if ("Circle".equals(name)) layout = newCircleLayout();
		else if ("EdgeLabel".equals(name)) layout = newEdgeLabelLayout();
		else if ("FastOrganic".equals(name)) layout = newFastOrganicLayout();
		else if ("Hierarchical".equals(name)) layout = newHierarchicalLayout();
		else if ("Organic".equals(name)) layout = newOrganicLayout();
		else if ("Orthogonal".equals(name)) layout = newOrthogonalLayout();
		else if ("ParallelEdge".equals(name)) layout = newParallelEdgeLayout();
		else if ("Partition".equals(name)) layout = newPartitionLayout();
		else if ("Stack".equals(name)) layout = newStackLayout();
		else layout = newCompactTreeLayout();
		return layout;
	}

	/**
	 * Create a compact tree layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newCompactTreeLayout()
	{
		mxCompactTreeLayout layout = new mxCompactTreeLayout(graph);
		layout.setNodeDistance(60);
		layout.setResetEdges(true);
		layout.setResizeParent(false);
		return layout;
	}

	/**
	 * Create a circle layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newCircleLayout()
	{
		return new mxCircleLayout(graph);
	}

	/**
	 * Create an edge label layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newEdgeLabelLayout()
	{
		return new mxEdgeLabelLayout(graph);
	}

	/**
	 * Create a fast organic layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newFastOrganicLayout()
	{
		return new mxFastOrganicLayout(graph);
	}

	/**
	 * Create a hierarchical layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newHierarchicalLayout()
	{
		return new mxHierarchicalLayout(graph);
	}

	/**
	 * Create an organic layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newOrganicLayout()
	{
		mxOrganicLayout layout = new mxOrganicLayout(graph);
		layout.setRadiusScaleFactor(1.0);
		layout.setApproxNodeDimensions(false);
		layout.setEdgeCrossingCostFactor(8000.0);
		layout.setNodeDistributionCostFactor(layout.getNodeDistributionCostFactor() * 5.0);
		layout.setEdgeDistanceCostFactor(layout.getEdgeDistanceCostFactor() * 5.0);
		layout.setEdgeLengthCostFactor(layout.getEdgeLengthCostFactor() / 1000.0);
		return layout;
	}

	/**
	 * Create an othogonal layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newOrthogonalLayout()
	{
		return new mxOrthogonalLayout(graph);
	}

	/**
	 * Create a parallel edge layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newParallelEdgeLayout()
	{
		return new mxParallelEdgeLayout(graph);
	}

	/**
	 * Create a partition layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newPartitionLayout()
	{
		return new mxPartitionLayout(graph, false, 10);
	}

	/**
	 * Create a stack layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newStackLayout()
	{
		return new mxStackLayout(graph);
	}
}
