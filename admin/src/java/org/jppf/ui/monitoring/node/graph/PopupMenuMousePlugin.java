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

import java.awt.Point;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.actions.*;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;

/**
 * A GraphMousePlugin that brings up distinct popup menus when an edge or vertex is appropriately clicked in a graph. If
 * these menus contain components that implement either the EdgeMenuListener or VertexMenuListener then the
 * corresponding interface methods will be called prior to the display of the menus (so that they can display context
 * sensitive information for the edge or vertex).
 * @param <V> the type of the vertices.
 * @param <E> the type of the edges.
 * @author Laurent Cohen
 */
public class PopupMenuMousePlugin<V, E> extends AbstractPopupGraphMousePlugin
{
  /**
   * The object that handles toolbar and menu actions.
   */
  private ActionHandler actionHandler = null;

  /**
   * Initialize this popup handler with the specified action handler.
   * @param actionHandler the action handler to use.
   */
  public PopupMenuMousePlugin(final ActionHandler actionHandler)
  {
    super(InputEvent.BUTTON3_MASK);
    this.actionHandler = actionHandler;
  }

  /**
   * Handle right-click event.
   * @param e the mouse event.
   */
  @Override
  protected void handlePopup(final MouseEvent e)
  {
    final VisualizationViewer<V, E> viewer = (VisualizationViewer<V, E>) e.getSource();
    Point p = e.getPoint();

    GraphElementAccessor<V, E> pickSupport = viewer.getPickSupport();
    if (pickSupport != null)
    {
      final V vertex = pickSupport.getVertex(viewer.getGraphLayout(), p.getX(), p.getY());
      if (vertex != null)
      {
        JPopupMenu vertexMenu = createMenu(vertex, viewer, e.getLocationOnScreen());
        vertexMenu.show(viewer, e.getX(), e.getY());
      }
    }
  }

  /**
   * 
   * @param vertex the vertex for which to create the menu.
   * @param viewer the visualization viewer.
   * @param point the location of the mouse right-click.
   * @return the created popup menu.
   */
  private JPopupMenu createMenu(final V vertex, final VisualizationViewer viewer, final Point point)
  {
    //viewer.l
    JPopupMenu menu = new JPopupMenu();
    menu.add(createMenuItem(actionHandler.getAction("graph.shutdown.restart.driver"), point));
    menu.add(createMenuItem(actionHandler.getAction("graph.driver.reset.statistics"), point));
    menu.addSeparator();
    menu.add(createMenuItem(actionHandler.getAction("graph.show.information"), point));
    menu.add(createMenuItem(actionHandler.getAction("graph.update.configuration"), point));
    menu.add(createMenuItem(actionHandler.getAction("graph.update.threads"), point));
    menu.add(createMenuItem(actionHandler.getAction("graph.restart.node"), point));
    menu.add(createMenuItem(actionHandler.getAction("graph.shutdown.node"), point));
    menu.add(createMenuItem(actionHandler.getAction("graph.reset.counter"), point));
    /*
    menu.addSeparator();
    menu.add(createMenuItem(actionHandler.getAction("graph.toggle.mode"), point));
    */
    return menu;
  }

  /**
   * Create a menu item.
   * @param action the action associated with the neu item.
   * @param location the location to use for any window create by the action.
   * @return a <code>JMenuItem</code> instance.
   */
  private static JMenuItem createMenuItem(final Action action, final Point location)
  {
    if (action instanceof AbstractUpdatableAction) ((AbstractUpdatableAction) action).setLocation(location);
    return new JMenuItem(action);
  }
}
