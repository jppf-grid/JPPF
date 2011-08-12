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
	LayoutFactory(mxGraph graph)
	{
		this.graph = graph;
	}

	/**
	 * Create a layout with the specified name.
	 * @param name the name of the layout to create.
	 * @return a <code>mxIGraphLayout</code> instance.
	 */
	mxIGraphLayout createLayout(String name)
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
		return layout;
	}

	/**
	 * Create a circle layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newCircleLayout()
	{
		mxCircleLayout layout = new mxCircleLayout(graph);
		return layout;
	}

	/**
	 * Create an edge label layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newEdgeLabelLayout()
	{
		mxEdgeLabelLayout layout = new mxEdgeLabelLayout(graph);
		return layout;
	}

	/**
	 * Create a fast organic layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newFastOrganicLayout()
	{
		mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
		return layout;
	}

	/**
	 * Create a hierarchical layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newHierarchicalLayout()
	{
		mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
		return layout;
	}

	/**
	 * Create an organic layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newOrganicLayout()
	{
		mxOrganicLayout layout = new mxOrganicLayout(graph);
		/*
		layout.setOptimizeEdgeCrossing(true);
		layout.setOptimizeEdgeDistance(true);
		layout.setOptimizeEdgeLength(true);
		layout.setOptimizeNodeDistribution(true);
		*/
		layout.setEdgeLengthCostFactor(1d);
		layout.setEdgeCrossingCostFactor(10000d);
		layout.setNodeDistributionCostFactor(0d);
		layout.setMinMoveRadius(1000d);
		return layout;
	}

	/**
	 * Create an othogonal layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newOrthogonalLayout()
	{
		mxOrthogonalLayout layout = new mxOrthogonalLayout(graph);
		return layout;
	}

	/**
	 * Create a parallel edge layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newParallelEdgeLayout()
	{
		mxParallelEdgeLayout layout = new mxParallelEdgeLayout(graph);
		return layout;
	}

	/**
	 * Create a partition layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newPartitionLayout()
	{
		mxPartitionLayout layout = new mxPartitionLayout(graph);
		return layout;
	}

	/**
	 * Create a stack layout for the graph.
	 * @return the layout as a <code>mxIGraphLayout</code> instance.
	 */
	private mxIGraphLayout newStackLayout()
	{
		mxStackLayout layout = new mxStackLayout(graph);
		return layout;
	}
}
