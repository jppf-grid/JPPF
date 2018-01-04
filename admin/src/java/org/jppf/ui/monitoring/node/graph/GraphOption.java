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

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

import org.apache.commons.collections15.functors.ConstantTransformer;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.decorators.*;
import edu.uci.ics.jung.visualization.picking.*;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.renderers.*;

/**
 * Displays and updates the graph view of the grid topology.
 * @author Laurent Cohen
 */
public class GraphOption extends AbstractOption implements ActionHolder {
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The graph visualization component.
   */
  protected VisualizationViewer<AbstractTopologyComponent, Number> viewer = null;
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
  public GraphOption() {
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public synchronized void createUI() {
    if (graphHandler == null) {
      if (debugEnabled) log.debug("creating UI");
      graphHandler = new GraphTopologyHandler(this);
      final SparseMultigraph<AbstractTopologyComponent, Number> graph = graphHandler.getDisplayGraph();
      layoutFactory = new LayoutFactory(graph);
      layout = "Radial";
      viewer = new VisualizationViewer<>(layoutFactory.createLayout(layout));
      layoutFactory.setViewer(viewer);
      viewer.setBackground(Color.white);
      viewer.setPickedVertexState(new MultiPickedState<AbstractTopologyComponent>());
      viewer.setPickSupport(new ShapePickSupport<>(viewer));
      final VertexLabelAsShapeRenderer<AbstractTopologyComponent, Number> vlasr = new VertexLabelAsShapeRenderer<>(viewer.getRenderContext());
      viewer.getRenderer().setVertexLabelRenderer(vlasr);
      viewer.getRenderContext().setVertexShapeTransformer(vlasr);
      final JPPFVertexLabelRenderer renderer = new JPPFVertexLabelRenderer();
      final Dimension d = renderer.getPreferredSize();
      d.width = LayoutFactory.VERTEX_SIZE.width;
      if (d.height < LayoutFactory.VERTEX_SIZE.height) {
        d.height = LayoutFactory.VERTEX_SIZE.height;
        renderer.setPreferredSize(d);
      }
      viewer.getRenderContext().setVertexLabelRenderer(renderer);
      viewer.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
      viewer.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<>(viewer.getPickedVertexState(), viewer.getBackground(), Color.blue));
      viewer.getRenderContext().setVertexDrawPaintTransformer(new ConstantTransformer(null));
      viewer.getRenderContext().setEdgeStrokeTransformer(new ConstantTransformer(new BasicStroke(1f)));
      //viewer.getRenderContext().setEdgeShapeTransformer(new EdgeShape.SimpleLoop());
      //viewer.getRenderContext().setEdgeArrowTransformer();
      viewer.setVertexToolTipTransformer(new ToStringLabeller<AbstractTopologyComponent>() {
        @Override
        public String transform(final AbstractTopologyComponent v) {
          return v.isNode() ? computeNodeTooltip((TopologyNode) v) : computeDriverTooltip((TopologyDriver) v);
        }
      });
      graphComponent = new GraphZoomScrollPane(viewer);
      graphComponent.getVerticalScrollBar().setPreferredSize(new Dimension(GuiUtils.DEFAULT_SCROLLBAR_THICKNESS, 0));
      graphComponent.getHorizontalScrollBar().setPreferredSize(new Dimension(0, GuiUtils.DEFAULT_SCROLLBAR_THICKNESS));
      actionHandler = new GraphActionHandler(viewer);
      final EditingModalGraphMouse<AbstractTopologyComponent, Number> graphMouse = new EditingModalGraphMouse<>(viewer.getRenderContext(), null, null);
      graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
      final PopupMenuMousePlugin<AbstractTopologyComponent, Number> myPlugin = new PopupMenuMousePlugin<>(actionHandler);
      graphMouse.remove(graphMouse.getPopupEditingPlugin());
      graphMouse.add(myPlugin);
      viewer.setGraphMouse(graphMouse);
      graphComponent.addComponentListener(new ViewerComponentListener());
      StatsHandler.getInstance().getShowIPHandler().addShowIPListener(new ShowIPListener() {
        @Override
        public void stateChanged(final ShowIPEvent event) {
          graphComponent.repaint();
        }
      });
    }
  }

  /**
   * Initialize the graph refreshing.
   */
  public void init() {
    if (debugEnabled) log.debug("initializing graph");
    populate();
    StatsHandler.getInstance().getTopologyManager().addTopologyListener(graphHandler);
  }

  @Override
  public JComponent getUIComponent() {
    return graphComponent;
  }

  /**
   * Get the current layout.
   * @return the layout.
   */
  public String getLayout() {
    return layout;
  }

  /**
   * Set the current layout.
   * @param name the layout name.
   */
  public void setLayout(final String name) {
    if (!name.equals(layout)) {
      layout = name;
      if (viewer != null) viewer.setGraphLayout(layoutFactory.createLayout(name));
    }
  }

  /**
   * Set the current layout.
   */
  void setLayout() {
    //setLayout(layout == null ? "Radial" : layout);
    if (viewer != null) viewer.setGraphLayout(layoutFactory.createLayout(layout == null ? "Radial" : layout));
  }

  @Override
  public ActionHandler getActionHandler() {
    return actionHandler;
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions() {
    synchronized(actionHandler) {
      actionHandler.putAction("graph.shutdown.restart.driver", new ServerShutdownRestartAction());
      actionHandler.putAction("graph.load.balancing.settings", new LoadBalancingAction());
      actionHandler.putAction("graph.driver.reset.statistics", new ServerStatisticsResetAction());
      actionHandler.putAction("graph.update.configuration", new NodeConfigurationAction());
      actionHandler.putAction("graph.show.information", new SystemInformationAction());
      actionHandler.putAction("graph.update.threads", new NodeThreadsAction());
      actionHandler.putAction("graph.reset.counter", new ResetTaskCounterAction());
      actionHandler.putAction("graph.restart.node", new ShutdownOrRestartNodeAction(true, true, "restart.node"));
      actionHandler.putAction("graph.restart.node.deferred", new ShutdownOrRestartNodeAction(true, false, "restart.node.deferred"));
      actionHandler.putAction("graph.shutdown.node", new ShutdownOrRestartNodeAction(false, true, "shutdown.node"));
      actionHandler.putAction("graph.shutdown.node.deferred", new ShutdownOrRestartNodeAction(false, false, "shutdown.node.deferred"));
      actionHandler.putAction("graph.cancel.deferred.action", new CancelDeferredAction());
      actionHandler.putAction("graph.toggle.active", new ToggleNodeActiveAction());
      actionHandler.putAction("graph.node.provisioning", new ProvisioningAction());
      actionHandler.putAction("graph.select.drivers", new SelectGraphDriversAction(this));
      actionHandler.putAction("graph.select.nodes", new SelectGraphNodesAction(this));
      actionHandler.putAction("graph.button.collapse", new ExpandOrCollapseGraphAction(this, true));
      actionHandler.putAction("graph.button.expand", new ExpandOrCollapseGraphAction(this, false));
      actionHandler.putAction("graph.toggle.mode", new ToggleModeAction(this));
      actionHandler.putAction("graph.toggle.layout", new ToggleLayoutAction(this));
      actionHandler.updateActions();
    }
    final Runnable r = new ActionsInitializer(this, "/graph.topology.toolbar");
    final Runnable r2 = new ActionsInitializer(this, "/graph.topology.toolbar.bottom");
    new Thread(r).start();
    new Thread(r2).start();
  }

  /**
   * Get the graph visualization component.
   * @return a <code>VisualizationViewer</code> instance.
   */
  public VisualizationViewer<AbstractTopologyComponent, Number> getViewer() {
    return viewer;
  }

  /**
   * Repaint the graph after changes have occurred.
   * @param updateLayout true if the layout should be updated, false otherwise.
   */
  void repaintGraph(final boolean updateLayout) {
    if (!repaintFlag.get()) return;
    if (getUIComponent() != null) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          if (updateLayout) setLayout();
          else {
            getUIComponent().invalidate();
            getUIComponent().repaint();
          }
        }
      });
    }
  }

  /**
   * Compute the tooltip for a node vertex.
   * @param node contains the information to put in the tooltip.
   * @return the text to set as tooltip.
   */
  private static String computeNodeTooltip(final TopologyNode node) {
    final StringBuilder sb = new StringBuilder();
    sb.append("<html>uuid: ").append(node.getUuid()).append("<br>");
    sb.append("Threads: ").append(node.getNodeState().getThreadPoolSize());
    sb.append(" | Tasks: ").append(node.getNodeState().getNbTasksExecuted());
    if (node.getManagementInfo().isMasterNode()) sb.append(" | Slaves: ").append(node.getNbSlaveNodes());
    sb.append("<br>");
    sb.append("Pending action: ").append(node.getPendingAction());
    sb.append("</html>");
    return sb.toString();
  }

  /**
   * Compute the tooltip for a driver vertex.
   * @param driver contains the information to put in the tooltip.
   * @return the text to set as tooltip.
   */
  private static String computeDriverTooltip(final TopologyDriver driver) {
    final StringBuilder sb = new StringBuilder();
    sb.append("<html>uuid: ").append(driver.getUuid());
    sb.append("</html>");
    return sb.toString();
  }

  /**
   * Redraw the graph.
   */
  public void populate() {
    graphHandler.populate();
  }

  /**
   * Get the object that handles operations modifying the graph.
   * @return a {@link GraphTopologyHandler} instance.
   */
  public GraphTopologyHandler getGraphHandler() {
    return graphHandler;
  }

  @Override
  public void setEnabled(final boolean enabled) {
  }

  @Override
  protected void setupValueChangeNotifications() {
  }

  /**
   * Determine whether to automatically layout the graph.
   * @return <code>true</code> if auto-layout is on, <code>false</code> otherwise.
   */
  public boolean isAutoLayout() {
    return autoLayout.get();
  }

  /**
   * Specify whether to automatically layout the graph.
   * @param autoLayout <code>true</code> to set auto-layout on, <code>false</code> otherwise.
   */
  public void setAutoLayout(final boolean autoLayout) {
    this.autoLayout.set(autoLayout);
  }

  /**
   * Listens to resize events to perform a graph layout.
   */
  public class ViewerComponentListener implements ComponentListener {
    @Override
    public void componentResized(final ComponentEvent e) {
      if (e.getComponent() != null) repaintGraph(isAutoLayout());
    }

    @Override
    public void componentMoved(final ComponentEvent e) {
    }

    @Override
    public void componentShown(final ComponentEvent e) {
    }

    @Override
    public void componentHidden(final ComponentEvent e) {
    }
  }
}
