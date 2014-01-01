/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

import org.apache.commons.collections15.functors.ConstantTransformer;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.options.AbstractOption;
import org.slf4j.*;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.decorators.*;
import edu.uci.ics.jung.visualization.picking.*;
import edu.uci.ics.jung.visualization.renderers.VertexLabelAsShapeRenderer;

/**
 * Displays and updates the graph view of the grid topology.
 * @author Laurent Cohen
 */
public class GraphOption extends AbstractOption implements ActionHolder
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(GraphOption.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The tree view.
   */
  protected NodeDataPanel treeTableOption = null;
  /**
   * The graph visualization component.
   */
  protected VisualizationViewer<TopologyData, Number> viewer = null;
  /**
   * The graph component.
   */
  protected GraphZoomScrollPane graphComponent = null;
  /**
   * The graph layout.
   */
  protected transient String layout = null;
  /**
   * Creates the layout objects based on their name.
   */
  protected transient LayoutFactory layoutFactory = null;
  /**
   * Manages the actions for this graph.
   */
  protected transient ActionHandler actionHandler = null;
  /**
   * 
   */
  AtomicBoolean repaintFlag = new AtomicBoolean(true);
  /**
   * Handles operations modifying the graph.
   */
  private GraphTopologyHandler graphHandler = null;
  /**
   * Determines whether to automatically layout the graph upon topology changes.
   */
  private AtomicBoolean autoLayout = new AtomicBoolean(true);

  /**
   * Default constructor.
   */
  public GraphOption()
  {
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized void createUI()
  {
    if (graphHandler == null)
    {
      if (debugEnabled) log.debug("creating UI");
      graphHandler = new GraphTopologyHandler(this);
      SparseMultigraph<TopologyData, Number> graph = graphHandler.getDisplayGraph();
      layoutFactory = new LayoutFactory(graph);
      layout = "Radial";
      viewer = new VisualizationViewer<>(layoutFactory.createLayout(layout));
      layoutFactory.setViewer(viewer);
      viewer.setBackground(Color.white);
      viewer.setPickedVertexState(new MultiPickedState());
      viewer.setPickSupport(new ShapePickSupport(viewer));
      VertexLabelAsShapeRenderer<TopologyData, Number> vlasr = new VertexLabelAsShapeRenderer<>(viewer.getRenderContext());
      viewer.getRenderer().setVertexLabelRenderer(vlasr);
      viewer.getRenderContext().setVertexShapeTransformer(vlasr);
      JPPFVertexLabelRenderer renderer = new JPPFVertexLabelRenderer();
      int height = 50;
      Dimension d = renderer.getPreferredSize();
      d.width = 100;
      if (d.height < height)
      {
        d.height = height;
        renderer.setPreferredSize(d);
      }
      viewer.getRenderContext().setVertexLabelRenderer(renderer);
      viewer.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<>(viewer.getPickedVertexState(), viewer.getBackground(), Color.blue));
      viewer.getRenderContext().setVertexDrawPaintTransformer(new ConstantTransformer(null));
      viewer.getRenderContext().setEdgeStrokeTransformer(new ConstantTransformer(new BasicStroke(0.5f)));
      viewer.setVertexToolTipTransformer(new ToStringLabeller<TopologyData>() {
        @Override
        public String transform(final TopologyData v) {
          return v.isNode() ? computeNodeTooltip(v) : super.transform(v);
        }
      });
      graphComponent = new GraphZoomScrollPane(viewer);
      actionHandler = new GraphActionHandler(viewer);
      EditingModalGraphMouse<TopologyData, Number> graphMouse = new EditingModalGraphMouse<>(viewer.getRenderContext(), null, null);
      graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
      PopupMenuMousePlugin<TopologyData, Number> myPlugin = new PopupMenuMousePlugin<>(actionHandler);
      graphMouse.remove(graphMouse.getPopupEditingPlugin());
      graphMouse.add(myPlugin);
      viewer.setGraphMouse(graphMouse);
      graphComponent.addComponentListener(new ViewerComponentListener());
    }
  }

  @Override
  public JComponent getUIComponent()
  {
    return graphComponent;
  }

  /**
   * Set the corresponding tree view onto this graph.
   * @param treeTableOption a {@link AbstractTreeTableOption} instance.
   */
  public void setTreeTableOption(final NodeDataPanel treeTableOption)
  {
    this.treeTableOption = treeTableOption;
  }

  /**
   * Get the current layout.
   * @return the layout.
   */
  public String getLayout()
  {
    return layout;
  }

  /**
   * Set the current layout.
   * @param name the layout name.
   */
  public void setLayout(final String name)
  {
    layout = name;
    if (viewer != null) viewer.setGraphLayout(layoutFactory.createLayout(name));
  }

  /**
   * Set the current layout.
   */
  public void setLayout()
  {
    setLayout("Radial");
  }

  @Override
  public ActionHandler getActionHandler()
  {
    return actionHandler;
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions()
  {
    synchronized(actionHandler)
    {
      actionHandler.putAction("graph.shutdown.restart.driver", new ServerShutdownRestartAction());
      actionHandler.putAction("graph.driver.reset.statistics", new ServerStatisticsResetAction());
      actionHandler.putAction("graph.update.configuration", new NodeConfigurationAction());
      actionHandler.putAction("graph.show.information", new SystemInformationAction());
      actionHandler.putAction("graph.update.threads", new NodeThreadsAction());
      actionHandler.putAction("graph.reset.counter", new ResetTaskCounterAction());
      actionHandler.putAction("graph.restart.node", new RestartNodeAction());
      actionHandler.putAction("graph.shutdown.node", new ShutdownNodeAction());
      actionHandler.putAction("graph.toggle.active", new ToggleNodeActiveAction(treeTableOption));
      actionHandler.putAction("graph.select.drivers", new SelectGraphDriversAction(this));
      actionHandler.putAction("graph.select.nodes", new SelectGraphNodesAction(this));
      actionHandler.putAction("graph.button.collapse", new ExpandOrCollapseGraphAction(this, true));
      actionHandler.putAction("graph.button.expand", new ExpandOrCollapseGraphAction(this, false));
      actionHandler.putAction("graph.toggle.mode", new ToggleModeAction(this));
      actionHandler.putAction("graph.toggle.layout", new ToggleLayoutAction(this));
      actionHandler.updateActions();
    }
    Runnable r = new ActionsInitializer(this, "/graph.topology.toolbar");
    new Thread(r).start();
  }

  /**
   * Get the graph visualization component.
   * @return a <code>VisualizationViewer</code> instance.
   */
  public VisualizationViewer<TopologyData, Number> getViewer()
  {
    return viewer;
  }

  /**
   * Repaint the graph after changes have occurred.
   * @param updateLayout true if the layout should be updated, false otherwise.
   */
  void repaintGraph(final boolean updateLayout)
  {
    if (!repaintFlag.get()) return;
    if (getUIComponent() != null)
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          if (updateLayout) setLayout();
          else
          {
            getUIComponent().invalidate();
            getUIComponent().repaint();
          }
        }
      });
    }
  }

  /**
   * Compute ther tooltipe for a node vertex.
   * @param node contains the information to put in the tooltip.
   * @return the text to set as tooltip.
   */
  private String computeNodeTooltip(final TopologyData node)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("<html>").append(node.getId()).append("<br>");
    sb.append("Threads: ").append(node.getNodeState().getThreadPoolSize());
    sb.append(" | Tasks: ").append(node.getNodeState().getNbTasksExecuted());
    sb.append("</html>");
    return sb.toString();
  }

  /**
   * Redraw the graph.
   */
  public void populate()
  {
    graphHandler.populate(treeTableOption.getTreeTableRoot());
  }

  /**
   * Get the object that handles operations modifying the graph.
   * @return a {@link GraphTopologyHandler} instance.
   */
  public GraphTopologyHandler getGraphHandler()
  {
    return graphHandler;
  }

  @Override
  public void setEnabled(final boolean enabled)
  {
  }

  @Override
  protected void setupValueChangeNotifications()
  {
  }

  /**
   * Determine whether to automatically layout the graph.
   * @return <code>true</code> if auto-layout is on, <code>false</code> otherwise.
   */
  public boolean isAutoLayout()
  {
    return autoLayout.get();
  }

  /**
   * Specify whether to automatically layout the graph.
   * @param autoLayout <code>true</code> to set auto-layout on, <code>false</code> otherwise.
   */
  public void setAutoLayout(final boolean autoLayout)
  {
    this.autoLayout.set(autoLayout);
  }

  /**
   * Listens to resize events to perform a graph layout.
   */
  public class ViewerComponentListener implements ComponentListener
  {
    @Override
    public void componentResized(final ComponentEvent e)
    {
      if (e.getComponent() != null) repaintGraph(isAutoLayout());
    }

    @Override
    public void componentMoved(final ComponentEvent e)
    {
    }

    @Override
    public void componentShown(final ComponentEvent e)
    {
    }

    @Override
    public void componentHidden(final ComponentEvent e)
    {
    }
  }
}
