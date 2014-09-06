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

package org.jppf.ui.monitoring.node;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.diagnostics.JVMHealthPanel;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.monitoring.node.graph.GraphOption;
import org.jppf.ui.options.FormattedNumberOption;
import org.jppf.ui.treetable.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class NodeDataPanel extends AbstractTreeTableOption implements ClientListener {
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
  private Map<String, ConnectionStatusListener> listenerMap = new Hashtable<>();
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
   * Reference to the JVM health view.
   */
  private JVMHealthPanel jvmHealthPanel;

  /**
   * Initialize this panel with the specified information.
   */
  public NodeDataPanel() {
    BASE = "org.jppf.ui.i18n.NodeDataPage";
    if (debugEnabled) log.debug("initializing NodeDataPanel");
    manager = new NodeDataPanelManager(this);
    createTreeTableModel();
    StatsHandler.getInstance().getClientHandler().getJppfClient(this);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTreeTableModel() {
    treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
    model = new NodeTreeTableModel(treeTableRoot);
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void populateTreeTableModel() {
    JPPFClient client = StatsHandler.getInstance().getClientHandler().getJppfClient();
    List<JPPFClientConnection> allConnections = client.getAllConnections();
    for (JPPFClientConnection c: allConnections) driverAdded(c);
  }

  /**
   * Create, initialize and layout the GUI components displayed in this panel.
   */
  @Override
  public void createUI() {
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
  public void nodeDataUpdated(final String driverName, final String nodeName) {
    Runnable r = new Runnable(){
      @Override public void run() {
        manager.nodeDataUpdated(driverName, nodeName);
      }
    };
    executor.submit(r);
  }

  /**
   * Called to notify that a driver was added.
   * @param connection a reference to the driver connection.
   */
  public void driverAdded(final JPPFClientConnection connection) {
    Runnable r = new Runnable() {
      @Override public void run() {
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
  public void driverRemoved(final String driverName, final boolean removeNodesOnly) {
    Runnable r = new Runnable() {
      @Override public void run() {
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
  public void nodeAdded(final String driverUuid, final JPPFManagementInfo nodeInfo) {
    Runnable r = new Runnable() {
      @Override public void run() {
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
  public void nodeRemoved(final String driverName, final String nodeName) {
    Runnable r = new Runnable() {
      @Override public void run() {
        manager.nodeRemoved(driverName, nodeName);
      }
    };
    executor.submit(r);
  }

  /**
   * Repaint the tree table area.
   */
  void repaintTreeTable() {
    executor.submit(new Runnable() {
      @Override public void run() {
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
  public synchronized Map<String, JPPFClientConnection> getAllDriverNames() {
    Map<String, JPPFClientConnection> map = new HashMap<>();
    for (int i=0; i<treeTableRoot.getChildCount(); i++) {
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
  public synchronized void refreshNodeStates() {
    for (int i=0; i<treeTableRoot.getChildCount(); i++) {
      DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
      TopologyData driverData = (TopologyData) driverNode.getUserObject();
      if (driverNode.getChildCount() <= 0) continue;
      if (driverData.getNodeForwarder() == null) continue;
      Map<String, TopologyData> uuidMap = new HashMap<>();
      Map<String, TopologyData> masterUuidMap = new HashMap<>();
      for (int j=0; j<driverNode.getChildCount(); j++) {
        DefaultMutableTreeNode nodeNode = (DefaultMutableTreeNode) driverNode.getChildAt(j);
        TopologyData data = (TopologyData) nodeNode.getUserObject();
        uuidMap.put(data.getUuid(), data);
        if (data.getNodeInformation().isMasterNode()) masterUuidMap.put(data.getUuid(), data);
      }
      refreshStates(driverData, uuidMap);
      refreshProvisioningStates(driverData, masterUuidMap);
    }
  }

  /**
   * Refresh the states of the node for the specified driver.
   * @param driverData the driver for which to update the nodes.
   * @param nodeUuidMap the map of node uuids to their information.
   */
  private void refreshStates(final TopologyData driverData, final Map<String, TopologyData> nodeUuidMap) {
    if ((nodeUuidMap == null) || nodeUuidMap.isEmpty()) return;
    Map<String, Object> result = null;
    try {
      result = driverData.getNodeForwarder().state(new NodeSelector.UuidSelector(nodeUuidMap.keySet()));
    } catch(IOException e) {
      log.error("error getting node states for driver " + driverData.getUuid() + ", reinitializing the connection", e);
      driverRemoved(driverData.getUuid(), true);
      driverData.initializeProxies();
    } catch(Exception e) {
      log.error("error getting node states for driver " + driverData.getUuid(), e);
    }
    if (result == null) return;
    for (Map.Entry<String, Object> entry: result.entrySet()) {
      TopologyData data = nodeUuidMap.get(entry.getKey());
      if (data == null) continue;
      if (entry.getValue() instanceof Exception) {
        data.setStatus(TopologyDataStatus.DOWN);
        log.warn("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
      }
      else if (entry.getValue() instanceof JPPFNodeState) data.refreshNodeState((JPPFNodeState) entry.getValue());
    }
  }

  /**
   * Refresh the states of the node for the specified driver.
   * @param driverData the driver for which to update the nodes.
   * @param nodeUuidMap the map of node uuids to their information.
   */
  private void refreshProvisioningStates(final TopologyData driverData, final Map<String, TopologyData> nodeUuidMap) {
    if ((nodeUuidMap == null) || nodeUuidMap.isEmpty()) return;
    Map<String, Object> result = null;
    JPPFNodeForwardingMBean forwarder = driverData.getNodeForwarder();
    if (forwarder == null) return;
    try {
      result = forwarder.forwardGetAttribute(new NodeSelector.UuidSelector(nodeUuidMap.keySet()), JPPFNodeProvisioningMBean.MBEAN_NAME, "NbSlaves");
    } catch(IOException e) {
      log.error("error getting number of slaves for driver " + driverData.getUuid() + ", reinitializing the connection", e);
      driverRemoved(driverData.getUuid(), true);
      driverData.initializeProxies();
    } catch(Exception e) {
      log.error("error getting number of slaves for driver " + driverData.getUuid(), e);
    }
    if (result == null) return;
    for (Map.Entry<String, Object> entry: result.entrySet()) {
      TopologyData data = nodeUuidMap.get(entry.getKey());
      if (data == null) continue;
      if (entry.getValue() instanceof Exception) {
        data.setStatus(TopologyDataStatus.DOWN);
        log.warn("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
      }
      else if (entry.getValue() instanceof Integer) data.setNbSlaveNodes((Integer) entry.getValue());
    }
  }

  /**
   * Initialize all actions used in the panel.
   */
  public void setupActions() {
    actionHandler = new JTreeTableActionHandler(treeTable);
    actionHandler.putAction("shutdown.restart.driver", new ServerShutdownRestartAction());
    actionHandler.putAction("driver.reset.statistics", new ServerStatisticsResetAction());
    actionHandler.putAction("update.configuration", new NodeConfigurationAction());
    actionHandler.putAction("show.information", new SystemInformationAction());
    actionHandler.putAction("update.threads", new NodeThreadsAction());
    actionHandler.putAction("reset.counter", new ResetTaskCounterAction());
    actionHandler.putAction("restart.node", new ShutdownOrRestartNodeAction(true, true, "restart.node"));
    actionHandler.putAction("restart.node.deferred", new ShutdownOrRestartNodeAction(true, false, "restart.node.deferred"));
    actionHandler.putAction("shutdown.node", new ShutdownOrRestartNodeAction(false, true, "shutdown.node"));
    actionHandler.putAction("shutdown.node.deferred", new ShutdownOrRestartNodeAction(false, false, "shutdown.node.deferred"));
    actionHandler.putAction("toggle.active", new ToggleNodeActiveAction(this));
    actionHandler.putAction("node.provisioning", new ProvisioningAction());
    actionHandler.putAction("select.drivers", new SelectDriversAction(this));
    actionHandler.putAction("select.nodes", new SelectNodesAction(this));
    actionHandler.updateActions();
    treeTable.addMouseListener(new NodeTreeTableMouseListener(actionHandler));
    Runnable r = new ActionsInitializer(this, "/topology.toolbar");
    Runnable r2 = new ActionsInitializer(this, "/topology.toolbar.bottom");
    new Thread(r).start();
    new Thread(r2).start();
  }

  /**
   * Notify this listener that a new driver connection was created.
   * @param event the event to notify this listener of.
   * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
   */
  @Override
  public synchronized void newConnection(final ClientEvent event) {
    final JPPFClientConnection c = event.getConnection();
    if (c.getDriverUuid() == null) {
      c.addClientConnectionStatusListener(new ClientConnectionStatusListener() {
        @Override
        public void statusChanged(final ClientConnectionStatusEvent event) {
          if (c.getStatus() == JPPFClientConnectionStatus.ACTIVE) {
            c.removeClientConnectionStatusListener(this);
            driverAdded(c);
          }
        }
      });
    }
    else driverAdded(c);
  }

  @Override
  public void connectionFailed(final ClientEvent event) {
    driverRemoved(event.getConnection().getDriverUuid(), false);
  }

  /**
   * Update the number of active servers or nodes in the status bar.
   * @param name the name of the field to update.
   * @param n the number of servers to add or subtract.
   */
  void updateStatusBar(final String name, final int n) {
    try {
      AtomicInteger nb = "/StatusNbServers".equals(name) ? nbServers : nbNodes;
      int newNb = nb.addAndGet(n);
      if (debugEnabled) log.debug("updating '" + name + "' with value = " + n + ", result = " + newNb);
      FormattedNumberOption option = (FormattedNumberOption) findFirstWithName(name);
      if (option != null) option.setValue(Double.valueOf(newNb));
    } catch(Throwable t) {
      log.error(t.getMessage(), t);
    }
  }

  /**
   * Refresh the number of active servers and nodes in the status bar.
   */
  public void refreshStatusBar() {
    FormattedNumberOption option = (FormattedNumberOption) findFirstWithName("/StatusNbServers");
    if (option != null) option.setValue(Double.valueOf(nbServers.get()));
    option = (FormattedNumberOption) findFirstWithName("/StatusNbNodes");
    if (option != null) option.setValue(Double.valueOf(nbNodes.get()));
  }

  /**
   * Get the mapping of connection names to status listener.
   * @return a map of string keys to <code>ConnectionStatusListener</code> values.
   */
  Map<String, ConnectionStatusListener> getListenerMap() {
    return listenerMap;
  }

  /**
   * Get the tree table updates manager.
   * @return a {@link NodeDataPanelManager} instance.
   */
  public NodeDataPanelManager getManager() {
    return manager;
  }

  /**
   * Get the graph view of the topology.
   * @return a {@link GraphOption} instance.
   */
  public GraphOption getGraphOption() {
    return graphOption;
  }

  /**
   * Set the graph view of the topology.
   * @param graphOption a {@link GraphOption} instance.
   */
  public void setGraphOption(final GraphOption graphOption) {
    if (debugEnabled) log.debug("start");
    if (this.graphOption == null) {
      if (graphOption != null) graphOption.setTreeTableOption(this);
      populateTreeTableModel();
      refreshNodeStates();
      if (graphOption != null) graphOption.populate();
      addTopologyChangeListener(graphOption.getGraphHandler());
      refreshHandler = new NodeRefreshHandler(this);
    }
    this.graphOption = graphOption;
    if (debugEnabled) log.debug("end");
  }

  /**
   * Get the JVM health view of the topology.
   * @return a {@link JVMHealthPanel} instance.
   */
  public JVMHealthPanel getJVMHealthPanel() {
    return jvmHealthPanel;
  }

  /**
   * Set the JVM health view of the topology.
   * @param jvmHealthPanel a {@link JVMHealthPanel} instance.
   */
  public void setJVMHealthPanel(final JVMHealthPanel jvmHealthPanel) {
    if (debugEnabled) log.debug("start");
    if (this.jvmHealthPanel == null) {
      if (jvmHealthPanel != null) {
        jvmHealthPanel.setNodePanel(this);
        jvmHealthPanel.populate();
        this.jvmHealthPanel = jvmHealthPanel;
        addTopologyChangeListener(jvmHealthPanel);
        jvmHealthPanel.initRefreshHandler();
      }
    }
    if (debugEnabled) log.debug("end");
  }

  /**
   * Add a topology change listener.
   * @param listener the listener to add.
   */
  public void addTopologyChangeListener(final TopologyChangeListener listener) {
    manager.addTopologyChangeListener(listener);
  }

  /**
   * Remove a topology change listener.
   * @param listener the listener to remove.
   */
  public void removeTopologyChangeListener(final TopologyChangeListener listener) {
    manager.removeTopologyChangeListener(listener);
  }
}
