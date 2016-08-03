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

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.*;

import org.jppf.client.monitoring.topology.*;
import org.slf4j.*;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;

/**
 * Radial layout for JPPF topology graph.
 * <p> This layout places all the driver vertices on a circle centered on the current view.
 * The node vertices for each driver are then placed in a half-circle centered on the driver vertex,
 * in the angle range [driverAngle - PI/2, driverAngle + PI/2], where <i>driverAngle</i> is the angle
 * formed by the line segment between the center of the view and the driver vertex, with regards to the coordinates system.
 * <p>This is not a generic layout algorithm as it assumes the graph to have the topological properties of a JPPF Grid:
 * <ul>
 * <li>drivers are roots in the graph</li>
 * <li>drivers can be linked together (i.e. cyclic graph)</li>
 * <li>each node is only attached to a single driver (i.e. "terminal vertices")</li>
 * </ul>
 * @author Laurent Cohen
 */
public class RadialLayout extends AbstractLayout<AbstractTopologyComponent, Number> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RadialLayout.class);
  /**
   * The default radius factor to use when none is specified.
   */
  public static final double DEFAULT_RADIUS_FACTOR = 0.23d;
  /**
   * The distance from the center of the view to each driver vertex,
   * and from each node vertex to its corresponding driver vertex.
   * If the view has a width <i>w</i> and height <i>h</i>, then the actual radius will be:<br>
   * radius = min(w, h) * radiusFactor
   */
  private double radiusFactor = DEFAULT_RADIUS_FACTOR;

  /**
   * Initialize this layout with the specified graph.
   * @param graph the graph to layout.
   */
  public RadialLayout(final Graph<AbstractTopologyComponent, Number> graph) {
    super(graph);
  }

  /**
   * Initialize this layout with the specified graph and radius factor.
   * @param graph the graph to layout.
   * @param radiusFactor radius factor to use.
   */
  public RadialLayout(final Graph<AbstractTopologyComponent, Number> graph, final double radiusFactor) {
    super(graph);
    this.radiusFactor = radiusFactor;
  }

  @Override
  public void initialize() {
    Dimension d = getSize();
    if (d != null) {
      Collection<TopologyDriver> drivers = getDrivers();
      int dSize = drivers.size();
      double height = d.getHeight();
      double width = d.getWidth();
      double radius = radiusFactor * (height < width ? height : width);
      radius *= (dSize > 1) ? 1d : 2d;

      int vertextWidth = LayoutFactory.VERTEX_SIZE.width;
      int i = 0;
      for (TopologyDriver driver : drivers) {
        Point2D coord = transform(driver);
        //double angle = dSize > 1 ? (2d * Math.PI * i) / dSize : 0d;
        double angle = dSize > 1 ? -Math.PI/2d + (2d * Math.PI * i) / dSize : 0d;
        if (dSize == 1) coord.setLocation(width / 2d, height / 2d);
        else coord.setLocation(Math.cos(angle) * radius + width / 2d, Math.sin(angle) * radius + height / 2d);
        Collection<TopologyNode> nodes = getNodes(driver);
        double firstAngle = dSize > 1 ? angle - Math.PI/2d : 0d;
        int j = 0;
        double factor = dSize > 1 ? 1d : 2d;
        for (TopologyNode node : nodes) {
          Point2D nodeCoord = transform(node);
          double nodeAngle = firstAngle + factor * Math.PI * j / nodes.size();
          double nodeX = Math.cos(nodeAngle) * radius + coord.getX();
          // make sure the vertex fits fully in the view
          if (nodeX < vertextWidth/2) nodeX = vertextWidth/2;
          double offset = nodeX + vertextWidth/2 - width;
          if (offset > 0) nodeX -= offset;
          nodeCoord.setLocation(nodeX, Math.sin(nodeAngle) * radius + coord.getY());
          j++;
        }
        i++;
      }
    }
  }

  @Override
  public void reset() {
    initialize();
  }

  /**
   * Get all the vertices that represent a driver.
   * @return a cloolection of <code>TopologyData</code> objects.
   */
  private Collection<TopologyDriver> getDrivers() {
    try {
      Set<TopologyDriver> drivers = new HashSet<>();
      Collection<AbstractTopologyComponent> coll = graph.getVertices();
      if (coll != null) {
        for (AbstractTopologyComponent data: coll) if (data.isDriver()) drivers.add((TopologyDriver) data);
      }
      return drivers;
    } catch(Exception e) {
      log.debug(e.getMessage(), e);
      return Collections.<TopologyDriver>emptyList();
    }
  }

  /**
   * Get the nodes attached to the specified driver.
   * @param driver the driver for which to retrieve the nodes.
   * @return a cloolection of <code>TopologyData</code> objects.
   */
  private Collection<TopologyNode> getNodes(final TopologyDriver driver) {
    try {
      Set<TopologyNode> nodes = new HashSet<>();
      Collection<AbstractTopologyComponent> coll = graph.getNeighbors(driver);
      if (coll != null) {
        for (AbstractTopologyComponent data: coll) if (data.isNode()) nodes.add((TopologyNode) data);
      }
      return nodes;
    } catch(Exception e) {
      log.debug(e.getMessage(), e);
      return Collections.<TopologyNode>emptyList();
    }
  }
}
