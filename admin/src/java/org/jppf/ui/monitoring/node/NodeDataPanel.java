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

package org.jppf.ui.monitoring.node;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.monitoring.node.graph.GraphOption;
import org.jppf.ui.options.FormattedNumberOption;
import org.jppf.ui.treetable.*;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class NodeDataPanel extends AbstractTreeTableOption implements ClientListener
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeDataPanel.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Handles the automatic and manual refresh of the tree.
   */
  private transient NodeRefreshHandler refreshHandler = null;
  /**
   * Mapping of connection names to status listener.
   */
  private Map<String, ConnectionStatusListener> listenerMap = new Hashtable<String, ConnectionStatusListener>();
  /**
   * Number of active servers.
   */
  private AtomicInteger nbServers = new AtomicInteger(0);
  /**
   * Number of active nodes.
   */
  private AtomicInteger nbNodes = new AtomicInteger(0);
  /**
   * Manages the tree table updates.
   */
  private NodeDataPanelManager manager;
  /**
   * Separate thread used to sequentialize events that impact the tree table.
   */
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  /**
   * The graph view of the topology.
   */
  private GraphOption graphOption;

  /**
   * Initialize this panel with the specified information.
   */
  public NodeDataPanel()
  {
    BASE = "org.jppf.ui.i18n.NodeDataPage";
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    manager = new NodeDataPanelManager(this);
    createTreeTableModel();
    StatsHandler.getInstance().getJppfClient(null).addClientListener(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel()
  {
    treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
    model = new NodeTreeTableModel(treeTableRoot);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void populateTreeTableModel()
  {
    JPPFClient client = StatsHandler.getInstance().getJppfClient(null);
    List<JPPFClientConnection> allConnections = client.getAllConnections();
    for (JPPFClientConnection c: allConnections) driverAdded(c);
  }

  /**
   * Create, initialize and layout the GUI components displayed in this panel.
   */
  @Override
  public void createUI()
  {
    treeTable = new JPPFTreeTable(model);
    treeTable.getTree().setLargeModel(true);
    treeTable.getTree().setRootVisible(false);
    treeTable.getTree().setShowsRootHandles(true);
    treeTable.expandAll();
    treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
    treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    treeTable.doLayout();
    treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    treeTable.getTree().setCellRenderer(new NodeRenderer());
    treeTable.setDefaultRenderer(Object.class, new NodeTableCellRenderer());
    JScrollPane sp = new JScrollPane(treeTable);
    setUIComponent(sp);
    treeTable.expandAll();
  }

  /**
   * Called when the state information of a node has changed.
   * @param driverName the name of the driver to which the node is attached.
   * @param nodeName the name of the node to update.
   */
  public void nodeDataUpdated(final String driverName, final String nodeName)
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        manager.nodeDataUpdated(driverName, nodeName);
      }
    };
    executor.submit(r);
  }

  /**
   * Called to notify that a driver was added.
   * @param connection a reference to the driver connection.
   */
  public void driverAdded(final JPPFClientConnection connection)
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        manager.driverAdded(connection);
      }
    };
    executor.submit(r);
  }

  /**
   * Called to notify that a driver was removed.
   * @param driverName the name of the driver to remove.
   * @param removeNodesOnly true if only the nodes attached to the driver are to be removed.
   */
  public void driverRemoved(final String driverName, final boolean removeNodesOnly)
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        manager.driverRemoved(driverName, removeNodesOnly);
      }
    };
    executor.submit(r);
    if (debugEnabled) log.debug("submitted driverRemoved task for " + driverName);
  }

  /**
   * Called to notify that a node was added to a driver.
   * @param driverUuid the name of the driver to which the node is added.
   * @param nodeInfo the object that encapsulates the node addition.
   */
  public void nodeAdded(final String driverUuid, final JPPFManagementInfo nodeInfo)
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        final DefaultMutableTreeNode driverNode = manager.findDriver(driverUuid);
        if (driverNode == null) return;
        manager.nodeAdded(driverNode, nodeInfo);
      }
    };
    executor.submit(r);
  }

  /**
   * Called to notify that a node was removed from a driver.
   * @param driverName the name of the driver from which the node is removed.
   * @param nodeName the name of the node to remove.
   */
  public void nodeRemoved(final String driverName, final String nodeName)
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        manager.nodeRemoved(driverName, nodeName);
      }
    };
    executor.submit(r);
  }

  /**
   * Repaint the tree table area.
   */
  void repaintTreeTable()
  {
    executor.submit(new Runnable()
    {
      @Override
      public void run()
      {
        manager.repaintTreeTable();
      }
    });
  }

  /**
   * Get the object that handles the automatic and manual refresh of the tree.
   * @return a <code>NodeRefreshHandler</code> instance.
   */
  public NodeRefreshHandler getRefreshHandler()
  {
    return refreshHandler;
  }

  /**
   * Get a mapping of driver names to their corresponding connection.
   * @return a map of string to <code>JPPFClientConnection</code> instances.
   */
  public synchronized Map<String, JPPFClientConnection> getAllDriverNames()
  {
    Map<String, JPPFClientConnection> map = new HashMap<String, JPPFClientConnection>();
    for (int i=0; i<treeTableRoot.getChildCount(); i++)
    {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      TopologyData data = (TopologyData) driverNode.getUserObject();
      //map.put(data.getJmxWrapper().getId(), data.getClientConnection());
      map.put(data.getUuid(), data.getClientConnection());
    }
    return map;
  }

  /**
   * Refresh the states of all displayed nodes.
   */
  public synchronized void refreshNodeStates()
  {
    for (int i=0; i<treeTableRoot.getChildCount(); i++)
    {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      for (int j=0; j<driverNode.getChildCount(); j++)
      {
        DefaultMutableTreeNode nodeNode = (DefaultMutableTreeNode) driverNode.getChildAt(j);
        TopologyData data = (TopologyData) nodeNode.getUserObject();
        data.refreshNodeState();
      }
    }
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions()
  {
    actionHandler = new JTreeTableActionHandler(treeTable);
    actionHandler.putAction("shutdown.restart.driver", new ServerShutdownRestartAction());
    actionHandler.putAction("driver.reset.statistics", new ServerStatisticsResetAction());
    actionHandler.putAction("update.configuration", new NodeConfigurationAction());
    actionHandler.putAction("show.information", new SystemInformationAction());
    actionHandler.putAction("update.threads", new NodeThreadsAction());
    actionHandler.putAction("reset.counter", new ResetTaskCounterAction());
    actionHandler.putAction("restart.node", new RestartNodeAction());
    actionHandler.putAction("shutdown.node", new ShutdownNodeAction());
    actionHandler.putAction("toggle.active", new ToggleNodeActiveAction(this));
    actionHandler.putAction("select.drivers", new SelectDriversAction(this));
    actionHandler.putAction("select.nodes", new SelectNodesAction(this));
    actionHandler.updateActions();
    treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
    Runnable r = new ActionsInitializer(this, "/topology.toolbar");
    new Thread(r).start();
  }

  /**
   * Notify this listener that a new driver connection was created.
   * @param event the event to notify this listener of.
   * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
   */
  @Override
  public synchronized void newConnection(final ClientEvent event)
  {
    driverAdded(event.getConnection());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void connectionFailed(final ClientEvent event)
  {
    JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) event.getConnection();
    driverRemoved(c.getUuid(), false);
  }

  /**
   * Update the number of active servers or nodes in the status bar.
   * @param name the name of the field to update.
   * @param n the number of servers to add or subtract.
   */
  void updateStatusBar(final String name, final int n)
  {
    try
    {
      AtomicInteger nb = "/StatusNbServers".equals(name) ? nbServers : nbNodes;
      int newNb = nb.addAndGet(n);
      if (debugEnabled) log.debug("updating '" + name + "' with value = " + n + ", result = " + newNb);
      FormattedNumberOption option = (FormattedNumberOption) findFirstWithName(name);
      if (option != null) option.setValue(Double.valueOf(newNb));
    }
    catch(Throwable t)
    {
      log.error(t.getMessage(), t);
    }
  }

  /**
   * Refresh the number of active servers and nodes in the status bar.
   */
  public void refreshStatusBar()
  {
    FormattedNumberOption option = (FormattedNumberOption) findFirstWithName("/StatusNbServers");
    if (option != null) option.setValue(Double.valueOf(nbServers.get()));
    option = (FormattedNumberOption) findFirstWithName("/StatusNbNodes");
    if (option != null) option.setValue(Double.valueOf(nbNodes.get()));
  }

  /**
   * Get the mapping of connection names to status listener.
   * @return a map of string keys to <code>ConnectionStatusListener</code> values.
   */
  Map<String, ConnectionStatusListener> getListenerMap()
  {
    return listenerMap;
  }

  /**
   * Get the tree table updates manager.
   * @return a {@link NodeDataPanelManager} instance.
   */
  public NodeDataPanelManager getManager()
  {
    return manager;
  }

  /**
   * Get the graph view of the topology.
   * @return a {@link GraphOption} instance.
   */
  public GraphOption getGraphOption()
  {
    return graphOption;
  }

  /**
   * Set the graph view of the topology.
   * @param graphOption a {@link GraphOption} instance.
   */
  public void setGraphOption(final GraphOption graphOption)
  {
    if (debugEnabled) log.debug("start");
    if (this.graphOption == null)
    {
      if (graphOption != null) graphOption.setTreeTableOption(this);
      populateTreeTableModel();
      refreshNodeStates();
      if (graphOption != null) graphOption.populate();
      refreshHandler = new NodeRefreshHandler(this);
    }
    this.graphOption = graphOption;
    if (debugEnabled) log.debug("end");
  }
}
