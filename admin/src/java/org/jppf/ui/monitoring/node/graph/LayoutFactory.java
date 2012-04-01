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

import java.util.Collection;

import org.jppf.ui.monitoring.node.TopologyData;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * Factory class to create layouts for <code>mxGraph</code> instances.
 * @author Laurent Cohen
 */
class LayoutFactory
{
  /**
   * The graph for which to create a layout.
   */
  private Graph<TopologyData, Number> initialGraph = null;
  /**
   * The graph visualization component.
   */
  private VisualizationViewer<TopologyData, Number> viewer = null;

  /**
   * Create a factory instance for the specified graph.
   * @param graph the viewer of the graph for which to create layouts.
   */
  LayoutFactory(final Graph<TopologyData, Number> graph)
  {
    this.initialGraph = graph;
  }

  /**
   * Create a layout with the specified name.
   * @param name the name of the layout to create.
   * @return a <code>mxIGraphLayout</code> instance.
   */
  Layout<TopologyData, Number> createLayout(final String name)
  {
    Layout<TopologyData, Number> layout = null;
    if ("Circle".equals(name)) return createCircleLayout();
    else if ("Fruchterman-Reingold".equals(name)) return createFRLayout();
    else if ("Fruchterman-Reingold-2".equals(name)) return createFRLayout2();
    else if ("Self Organizing Map".equals(name)) return createISOMLayout();
    else if ("Kamada-Kawai".equals(name)) return createKKLayout();
    else if ("Spring".equals(name)) return createSpringLayout();
    else if ("Spring2".equals(name)) return createSpringLayout2();
    else if ("Static".equals(name)) return createStaticLayout();
    else if ("Radial".equals(name)) return createRadialLayout();
    return createISOMLayout();
  }

  /**
   * Create a new circle layout.
   * @return a <code>CircleLayout</code> instance.
   */
  private CircleLayout<TopologyData, Number> createCircleLayout()
  {
    CircleLayout<TopologyData, Number> layout = new CircleLayout<TopologyData, Number>(initialGraph);
    layout.setRadius(150);
    return layout;
  }

  /**
   * Create a new Fruchterman-Reingold layout.
   * @return a <code>FRLayout</code> instance.
   */
  private FRLayout<TopologyData, Number> createFRLayout()
  {
    FRLayout<TopologyData, Number> layout = new FRLayout<TopologyData, Number>(initialGraph);
    layout.setAttractionMultiplier(0.75d);
    layout.setRepulsionMultiplier(0.75d);
    layout.setMaxIterations(700);
    return layout;
  }

  /**
   * Create a new Fruchterman-Reingold (2) layout.
   * @return a <code>FRLayout2</code> instance.
   */
  private FRLayout2<TopologyData, Number> createFRLayout2()
  {
    FRLayout2<TopologyData, Number> layout = new FRLayout2<TopologyData, Number>(initialGraph);
    layout.setAttractionMultiplier(0.75d);
    layout.setRepulsionMultiplier(0.75d);
    layout.setMaxIterations(700);
    return layout;
  }

  /**
   * Create a new Self Organizing Map layout.
   * @return a <code>ISOMLayout</code> instance.
   */
  private ISOMLayout<TopologyData, Number> createISOMLayout()
  {
    ISOMLayout<TopologyData, Number> layout = new ISOMLayout<TopologyData, Number>(initialGraph);
    return layout;
  }

  /**
   * Create a new Kamada-Kawai layout.
   * @return a <code>KKLayout</code> instance.
   */
  private KKLayout<TopologyData, Number> createKKLayout()
  {
    KKLayout<TopologyData, Number> layout = new KKLayout<TopologyData, Number>(initialGraph);
    layout.setAdjustForGravity(true);
    layout.setDisconnectedDistanceMultiplier(0.5d);
    layout.setExchangeVertices(true);
    layout.setLengthFactor(0.9d);
    layout.setMaxIterations(2000);
    return layout;
  }

  /**
   * Create a new Spring layout.
   * @return a <code>SpringLayout</code> instance.
   */
  private SpringLayout<TopologyData, Number> createSpringLayout()
  {
    SpringLayout<TopologyData, Number> layout = new SpringLayout<TopologyData, Number>(initialGraph);
    layout.setForceMultiplier(1d/3d);
    layout.setRepulsionRange(100*100);
    layout.setStretch(0.7d);
    return layout;
  }

  /**
   * Create a new Spring (2) layout.
   * @return a <code>SpringLayout2</code> instance.
   */
  private SpringLayout2<TopologyData, Number> createSpringLayout2()
  {
    SpringLayout2<TopologyData, Number> layout = new SpringLayout2<TopologyData, Number>(initialGraph);
    layout.setForceMultiplier(1d/3d);
    layout.setRepulsionRange(100*100);
    layout.setStretch(0.7d);
    return layout;
  }

  /**
   * Create a new Static layout.
   * @return a <code>StaticLayout</code> instance.
   */
  private RadialLayout createRadialLayout()
  {
    RadialLayout layout = new RadialLayout(initialGraph);
    if (viewer != null) layout.setSize(viewer.getSize());
    return layout;
  }

  /**
   * Create a new Static layout.
   * @return a <code>StaticLayout</code> instance.
   */
  private StaticLayout<TopologyData, Number> createStaticLayout()
  {
    Collection<TopologyData> vertices = initialGraph.getVertices();
    StaticLayout<TopologyData, Number> layout = new StaticLayout<TopologyData, Number>(initialGraph);
    return layout;
  }

  /**
   * Set the graph visualization component.
   * @param viewer a <code>VisualizationViewer</code> instance.
   */
  void setViewer(final VisualizationViewer<TopologyData, Number> viewer)
  {
    this.viewer = viewer;
    initialGraph = viewer.getGraphLayout().getGraph();
  }
}
