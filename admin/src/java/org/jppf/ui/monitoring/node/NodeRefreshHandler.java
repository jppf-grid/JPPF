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
import java.util.concurrent.atomic.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.slf4j.*;

/**
 * Instances of this class hold information about the associations between JPPF drivers and
 * their attached nodes, for management and monitoring purposes.
 * @author Laurent Cohen
 */
public class NodeRefreshHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeRefreshHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * JPPF client used to submit execution requests.
   */
  private JPPFClient jppfClient = null;
  /**
   * Timer used to query the driver management data.
   */
  private Timer refreshTimer = null;
  /**
   * Interval, in milliseconds, between refreshes from the server.
   */
  private long refreshInterval = 1000L;
  /**
   * The panel to refresh.
   */
  private NodeDataPanel nodeDataPanel = null;
  /**
   * Count of refresh invocations.
   */
  private AtomicLong refreshCount = new AtomicLong(0L);
  /**
   * Determines whether we are currently refreshing.
   */
  private AtomicBoolean refreshing = new AtomicBoolean(false);

  /**
   * Initialize this node handler.
   * @param nodeDataPanel - the panel to refresh.
   */
  public NodeRefreshHandler(final NodeDataPanel nodeDataPanel)
  {
    this.nodeDataPanel = nodeDataPanel;
    this.jppfClient = StatsHandler.getInstance().getJppfClient(null);
    initialize();
  }

  /**
   * Initialize this node refresh handler.
   */
  private void initialize()
  {
    //refresh();
    startRefreshTimer();
  }

  /**
   * Refresh the tree structure asynchronously (not in the AWT event thread).
   */
  public void refresh()
  {
    if (refreshing.get()) return;
    refreshing.set(true);
    try
    {
      refresh0();
    }
    finally
    {
      refreshing.set(false);
    }
  }

  /**
   * Refresh the tree structure.
   */
  private synchronized void refresh0()
  {
    Collection<JPPFClientConnection> connectionList = jppfClient.getAllConnections();
    Set<String> uuidSet = new HashSet<String>();
    Map<String, JPPFClientConnection> map = new HashMap<String, JPPFClientConnection>();
    for (JPPFClientConnection c: connectionList)
    {
      JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) c;
      if (uuidSet.contains(conn.getUuid())) continue;
      uuidSet.add(conn.getUuid());
      //map.put(conn.getJmxConnection().getId(), c);
      map.put(conn.getUuid(), c);
    }
    Map<String, JPPFClientConnection> connectionMap = nodeDataPanel.getAllDriverNames();

    // handle drivers that were removed
    List<String> driversToProcess = new ArrayList<String>();
    for (Map.Entry<String, JPPFClientConnection> entry: connectionMap.entrySet())
    {
      //String name = entry.getKey();
      String uuid = ((AbstractJPPFClientConnection) entry.getValue()).getUuid();
      if (!map.containsKey(uuid)) driversToProcess.add(uuid);
      else refreshNodes(uuid);
    }
    for (String uuid: driversToProcess)
    {
      if (debugEnabled) log.debug("removing driver " + uuid);
      nodeDataPanel.driverRemoved(uuid, false);
    }

    // handle drivers that were added
    driversToProcess = new ArrayList<String>();
    for (Map.Entry<String, JPPFClientConnection> entry: map.entrySet())
    {
      String uuid = entry.getKey();
      if (!connectionMap.containsKey(uuid)) driversToProcess.add(uuid);
    }
    for (String uuid: driversToProcess)
    {
      if (debugEnabled) log.debug("adding driver " + uuid);
      nodeDataPanel.driverAdded(map.get(uuid));
    }
    nodeDataPanel.refreshNodeStates();
    nodeDataPanel.repaintTreeTable();
  }

  /**
   * Refresh the nodes currently attached to the specified driver.
   * @param driverUuid the name of the driver.
   */
  private synchronized void refreshNodes(final String driverUuid) {
    DefaultMutableTreeNode driverNode = nodeDataPanel.getManager().findDriver(driverUuid);
    //if (debugEnabled) log.debug("driverNode = " + driverNode);
    if (driverNode == null) return;
    Set<String> panelNames = new HashSet<String>();
    for (int i=0; i<driverNode.getChildCount(); i++) {
      DefaultMutableTreeNode nodeNode = (DefaultMutableTreeNode) driverNode.getChildAt(i);
      TopologyData data = (TopologyData) nodeNode.getUserObject();
      panelNames.add(data.getUuid());
    }
    TopologyData data = (TopologyData) driverNode.getUserObject();
    JMXDriverConnectionWrapper wrapper = (JMXDriverConnectionWrapper) data.getJmxWrapper();
    if (!wrapper.isConnected()) return;
    Collection<JPPFManagementInfo> nodesInfo = null;
    try {
      nodesInfo = wrapper.nodesInformation();
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      return;
    }
    if (nodesInfo == null) return;
    Map<String, JPPFManagementInfo> actualMap = new HashMap<String, JPPFManagementInfo>();
    for (JPPFManagementInfo info: nodesInfo) actualMap.put(info.getUuid(), info);
    List<String> nodesToProcess = new ArrayList<String>(panelNames.size());
    for (String name: panelNames) {
      if (!actualMap.containsKey(name)) nodesToProcess.add(name);
    }
    for (String name: nodesToProcess) {
      if (debugEnabled) log.debug("removing node " + name);
      nodeDataPanel.nodeRemoved(driverUuid, name);
    }
    for (Map.Entry<String, JPPFManagementInfo> entry: actualMap.entrySet()) {
      if (!panelNames.contains(entry.getKey())) {
        if (debugEnabled) log.debug("adding node " + entry.getKey());
        nodeDataPanel.nodeAdded(driverUuid, entry.getValue());
      }
      else {
        DefaultMutableTreeNode nodeNode = nodeDataPanel.getManager().findNode(driverNode, entry.getKey());
        if (nodeNode != null) {
          TopologyData nodeData = (TopologyData) nodeNode.getUserObject();
          if (entry.getValue().isActive() != nodeData.getNodeInformation().isActive()) {
            nodeData.getNodeInformation().setActive(entry.getValue().isActive());
            nodeDataPanel.nodeDataUpdated(driverUuid, entry.getKey());
          }
        }
      }
    }
  }

  /**
   * Stop the automatic refresh of the nodes state through a timer.
   */
  public void stopRefreshTimer()
  {
    if (refreshTimer != null)
    {
      refreshTimer.cancel();
      refreshTimer = null;
    }
  }

  /**
   * Start the automatic refresh of the nodes state through a timer.
   */
  public void startRefreshTimer()
  {
    if (refreshTimer != null) return;
    if (refreshInterval <= 0L) return;
    refreshTimer = new Timer("JPPF Topology Update Timer");
    TimerTask task = new TimerTask()
    {
      @Override
      public void run()
      {
        refresh();
      }
    };
    refreshTimer.schedule(task, 1000L, refreshInterval);
  }
}
