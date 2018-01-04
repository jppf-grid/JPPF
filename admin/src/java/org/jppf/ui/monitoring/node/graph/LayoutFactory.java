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

import java.awt.Dimension;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * Factory class to create layouts for <code>mxGraph</code> instances.
 * @author Laurent Cohen
 */
class LayoutFactory {
  /**
   * The graph for which to create a layout.
   */
  private Graph<AbstractTopologyComponent, Number> initialGraph = null;
  /**
   * The graph visualization component.
   */
  private VisualizationViewer<AbstractTopologyComponent, Number> viewer = null;
  /**
   * The size of all the verices (drivers and nodes) in the graph.
   */
  static final Dimension VERTEX_SIZE = new Dimension(100, 50);

  /**
   * Create a factory instance for the specified graph.
   * @param graph the viewer of the graph for which to create layouts.
   */
  LayoutFactory(final Graph<AbstractTopologyComponent, Number> graph) {
    this.initialGraph = graph;
  }

  /**
   * Create a layout with the specified name.
   * @param name the name of the layout to create.
   * @return a <code>mxIGraphLayout</code> instance.
   */
  Layout<AbstractTopologyComponent, Number> createLayout(final String name) {
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
  private CircleLayout<AbstractTopologyComponent, Number> createCircleLayout() {
    final CircleLayout<AbstractTopologyComponent, Number> layout = new CircleLayout<>(initialGraph);
    layout.setRadius(150);
    return layout;
  }

  /**
   * Create a new Fruchterman-Reingold layout.
   * @return a <code>FRLayout</code> instance.
   */
  private FRLayout<AbstractTopologyComponent, Number> createFRLayout() {
    final FRLayout<AbstractTopologyComponent, Number> layout = new FRLayout<>(initialGraph);
    layout.setAttractionMultiplier(0.75d);
    layout.setRepulsionMultiplier(0.75d);
    layout.setMaxIterations(700);
    return layout;
  }

  /**
   * Create a new Fruchterman-Reingold (2) layout.
   * @return a <code>FRLayout2</code> instance.
   */
  private FRLayout2<AbstractTopologyComponent, Number> createFRLayout2() {
    final FRLayout2<AbstractTopologyComponent, Number> layout = new FRLayout2<>(initialGraph);
    layout.setAttractionMultiplier(0.75d);
    layout.setRepulsionMultiplier(0.75d);
    layout.setMaxIterations(700);
    return layout;
  }

  /**
   * Create a new Self Organizing Map layout.
   * @return a <code>ISOMLayout</code> instance.
   */
  private ISOMLayout<AbstractTopologyComponent, Number> createISOMLayout() {
    final ISOMLayout<AbstractTopologyComponent, Number> layout = new ISOMLayout<>(initialGraph);
    return layout;
  }

  /**
   * Create a new Kamada-Kawai layout.
   * @return a <code>KKLayout</code> instance.
   */
  private KKLayout<AbstractTopologyComponent, Number> createKKLayout() {
    final KKLayout<AbstractTopologyComponent, Number> layout = new KKLayout<>(initialGraph);
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
  private SpringLayout<AbstractTopologyComponent, Number> createSpringLayout() {
    final SpringLayout<AbstractTopologyComponent, Number> layout = new SpringLayout<>(initialGraph);
    layout.setForceMultiplier(1d / 3d);
    layout.setRepulsionRange(100 * 100);
    layout.setStretch(0.7d);
    return layout;
  }

  /**
   * Create a new Spring (2) layout.
   * @return a <code>SpringLayout2</code> instance.
   */
  private SpringLayout2<AbstractTopologyComponent, Number> createSpringLayout2() {
    final SpringLayout2<AbstractTopologyComponent, Number> layout = new SpringLayout2<>(initialGraph);
    layout.setForceMultiplier(1d / 3d);
    layout.setRepulsionRange(100 * 100);
    layout.setStretch(0.7d);
    return layout;
  }

  /**
   * Create a new Static layout.
   * @return a <code>StaticLayout</code> instance.
   */
  private RadialLayout createRadialLayout() {
    final RadialLayout layout = new RadialLayout(initialGraph);
    if (viewer != null) layout.setSize(viewer.getSize());
    return layout;
  }

  /**
   * Create a new Static layout.
   * @return a <code>StaticLayout</code> instance.
   */
  private StaticLayout<AbstractTopologyComponent, Number> createStaticLayout() {
    final StaticLayout<AbstractTopologyComponent, Number> layout = new StaticLayout<>(initialGraph);
    return layout;
  }

  /**
   * Set the graph visualization component.
   * @param viewer a <code>VisualizationViewer</code> instance.
   */
  void setViewer(final VisualizationViewer<AbstractTopologyComponent, Number> viewer) {
    this.viewer = viewer;
    initialGraph = viewer.getGraphLayout().getGraph();
  }
}
