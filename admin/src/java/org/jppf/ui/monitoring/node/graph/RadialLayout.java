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

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.*;

import org.jppf.ui.monitoring.node.TopologyData;

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
public class RadialLayout extends AbstractLayout<TopologyData, Number>
{
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
  public RadialLayout(final Graph<TopologyData, Number> graph)
  {
    super(graph);
  }

  /**
   * Initialize this layout with the specified graph and radius factor.
   * @param graph the graph to layout.
   * @param radiusFactor radius factor to use.
   */
  public RadialLayout(final Graph<TopologyData, Number> graph, final double radiusFactor)
  {
    super(graph);
    this.radiusFactor = radiusFactor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize()
  {
    Dimension d = getSize();

    if (d != null)
    {
      Collection<TopologyData> drivers = getDrivers();
      int dSize = drivers.size();
      double height = d.getHeight();
      double width = d.getWidth();
      double radius = radiusFactor * (height < width ? height : width);
      radius *= (dSize > 1) ? 1d : 2d;

      int i = 0;
      for (TopologyData driver : drivers)
      {
        Point2D coord = transform(driver);
        double angle = dSize > 1 ? (2d * Math.PI * i) / dSize : 0d;
        if (dSize == 1) coord.setLocation(width / 2d, height / 2d);
        else coord.setLocation(Math.cos(angle) * radius + width / 2d, Math.sin(angle) * radius + height / 2d);
        Collection<TopologyData> nodes = getNodes(driver);
        double firstAngle = dSize > 1 ? angle - Math.PI/2d : 0d;
        int j = 0;
        double factor = dSize > 1 ? 1d : 2d;
        for (TopologyData node : nodes)
        {
          Point2D nodeCoord = transform(node);
          double nodeAngle = firstAngle + factor * Math.PI * j / nodes.size();
          nodeCoord.setLocation(Math.cos(nodeAngle) * radius + coord.getX(), Math.sin(nodeAngle) * radius + coord.getY());
          j++;
        }
        i++;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset()
  {
    initialize();
  }

  /**
   * Get all the vertices that represent a driver.
   * @return a cloolection of <code>TopologyData</code> objects.
   */
  private Collection<TopologyData> getDrivers()
  {
    Set<TopologyData> drivers = new HashSet<TopologyData>();
    Collection<TopologyData> coll = graph.getVertices();
    if (coll != null)
    {
      for (TopologyData data: coll) if (!data.isNode()) drivers.add(data);
    }
    return drivers;
  }

  /**
   * Get the nodes attached to the specified driver.
   * @param driver the driver for which to retrieve the nodes.
   * @return a cloolection of <code>TopologyData</code> objects.
   */
  private Collection<TopologyData> getNodes(final TopologyData driver)
  {
    Set<TopologyData> nodes = new HashSet<TopologyData>();
    Collection<TopologyData> coll = graph.getNeighbors(driver);
    if (coll != null)
    {
      for (TopologyData data: coll) if (data.isNode()) nodes.add(data);
    }
    return nodes;
  }
}
