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

package org.jppf.admin.web.topology;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.*;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.util.time.Duration;
import org.jppf.admin.web.*;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.topology.nodeconfig.NodeConfigLink;
import org.jppf.admin.web.topology.nodethreads.NodeThreadsLink;
import org.jppf.admin.web.topology.provisioning.ProvisioningLink;
import org.jppf.admin.web.topology.serverstop.DriverStopRestartLink;
import org.jppf.admin.web.topology.systeminfo.SystemInfoLink;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.monitoring.node.NodeTreeTableModel;
import org.jppf.ui.treetable.*;
import org.jppf.ui.utils.TreeTableUtils;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

/**
 * This web page displays the topology tree.
 * @author Laurent Cohen
 */
@MountPath("topology")
public class TopologyTree extends TemplatePage implements TopologyListener, TableTreeHolder {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TopologyTree.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Server stop/restart action id.
   */
  public static String SERVER_STOP_RESTART_ACTION = "topology.server_stop_restart";
  /**
   * Server reset stats action id.
   */
  public static String SERVER_RESET_STATS_ACTION = "topology.server_reset_stats";
  /**
   * Node configuration update action id.
   */
  public static String NODE_CONFIG_ACTION = "topology.node_config";
  /**
   * System info action id.
   */
  public static String SYSTEM_INFO_ACTION = "topology.info";
  /**
   * Node thread pool config action id.
   */
  public static String NODE_THREADS_ACTION = "topology.node_threads";
  /**
   * Node reset task counter action id.
   */
  public static String NODE_RESET_TASKS_ACTION = "topology.node_reset_tasks";
  /**
   * Cancel pending action id.
   */
  public static String CANCEL_PENDING_ACTION = "topology.cancel_pending_action";
  /**
   * Stop node action id.
   */
  public static String NODE_STOP_ACTION = "topology.node_stop";
  /**
   * Restart node action id.
   */
  public static String NODE_RESTART_ACTION = "topology.node_restart";
  /**
   * Deferred stop node action id.
   */
  public static String NODE_STOP_DEFERRED_ACTION = "topology.node_stop_deferred";
  /**
   * Deferred restart node action id.
   */
  public static String NODE_RESTART_DEFERRED_ACTION = "topology.node_restart_deferred";
  /**
   * Deferred suspend node action id.
   */
  public static String NODE_SUSPEND_ACTION = "topology.node_suspend";
  /**
   * Provisioning action id.
   */
  public static String PROVISIONING_ACTION = "topology.provisioning";
  /**
   * Expand all action id.
   */
  public static String EXPAND_ALL_ACTION = "topology.expand";
  /**
   * Collapse action id.
   */
  public static String COLLAPSE_ALL_ACTION = "topology.collapse";
  /**
   * Select drivers action id.
   */
  public static String SELECT_DRIVERS_ACTION = "topology.select_drivers";
  /**
   * Select nodes action id.
   */
  public static String SELECT_NODES_ACTION = "topology.select_nodes";
  /**
   * Select all action id.
   */
  public static String SELECT_ALL_ACTION = "topology.select_all";
  /**
   * The tree table component.
   */
  private transient JPPFTableTree tableTree;
  /**
   * The tree table model.
   */
  private transient AbstractJPPFTreeTableModel topologyModel;
  /**
   * Handles the selection of rows in the tree table.
   */
  private transient SelectionHandler selectionHandler;
  /**
   * 
   */
  private transient Form<String> toolbar;
  /**
   * 
   */
  private final transient AjaxSelfUpdatingTimerBehavior refreshTimer = new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5));

  /**
   * Initialize this web page.
   */
  public TopologyTree() {
    setVersioned(false);
    createActions();
    add(toolbar);
    TableTreeData data = getJPPFSession().getTopologyData();
    selectionHandler = data.getSelectionHandler();
    tableTree = createTableTree();
    tableTree.add(new WindowsTheme()); // adds windows-style handles on nodes with children
    tableTree.add(refreshTimer);
    tableTree.addUpdateTarget(toolbar);
    data.selectionChanged(selectionHandler);
    if (debugEnabled) log.debug("table tree created");
    add(tableTree);
    if (debugEnabled) log.debug("table tree added to page");
  }

  /**
   * Create the tree table.
   * @return a {@link JPPFTableTree} instance.
   */
  private JPPFTableTree createTableTree() {
    if (debugEnabled) log.debug("creating topology model");
    createTopologyModel();
    if (debugEnabled) log.debug("topology model created");
    JPPFTableTree tree = new JPPFTableTree(TreeViewType.TOPOLOGY, "table.tree", createColumns(), topologyModel, Integer.MAX_VALUE, selectionHandler, new TopologyNodeRenderer());
    DataTable<DefaultMutableTreeNode, String> table = tree.getTable();
    HeadersToolbar<String> header = new HeadersToolbar<>(table, null);
    table.addTopToolbar(header);
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) topologyModel.getRoot();
    for (int i = 0; i < root.getChildCount(); i++) tree.expand((DefaultMutableTreeNode) root.getChildAt(i));
    if (debugEnabled) log.debug("tree created");
    return tree;
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTopologyModel() {
    JPPFWebSession session = getJPPFSession();
    TableTreeData data = session.getTopologyData();
    topologyModel = data.getModel();
    if (topologyModel == null) {
      JPPFWebConsoleApplication app = (JPPFWebConsoleApplication) getApplication();
      topologyModel = new NodeTreeTableModel(new DefaultMutableTreeNode(app.localize("tree.root.name")), session.getLocale());
      populateTreeTableModel();
      app.getTopologyManager().addTopologyListener(this);
      data.setModel(topologyModel);
    }
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private synchronized void populateTreeTableModel() {
    JPPFWebConsoleApplication app = (JPPFWebConsoleApplication) getApplication();
    for (TopologyDriver driver : app.getTopologyManager().getDrivers()) {
      TreeTableUtils.addDriver(topologyModel, driver);
      for (AbstractTopologyComponent child : driver.getChildren()) {
        TreeTableUtils.addNode(topologyModel, driver, (TopologyNode) child);
      }
    }
  }

  /**
   * @return the list of columns.
   */
  List<? extends IColumn<DefaultMutableTreeNode, String>> createColumns() {
    List<IColumn<DefaultMutableTreeNode, String>> columns = new ArrayList<>();
    Locale locale = getSession().getLocale();
    if (locale == null) locale = Locale.US;
    columns.add(new TopologyTreeColumn(Model.of("Tree")));
    columns.add(new TopologyColumn(NodeTreeTableModel.NODE_THREADS));
    columns.add(new TopologyColumn(NodeTreeTableModel.NODE_STATUS));
    columns.add(new TopologyColumn(NodeTreeTableModel.EXECUTION_STATUS));
    columns.add(new TopologyColumn(NodeTreeTableModel.NB_TASKS));
    columns.add(new TopologyColumn(NodeTreeTableModel.NB_SLAVES));
    columns.add(new TopologyColumn(NodeTreeTableModel.PENDING_ACTION));
    return columns;
  }

  /**
   * @return a form holding the toolbar components.
   */
  private ActionHandler createActions() {
    TableTreeData data = getJPPFSession().getTopologyData();
    ActionHandler actionHandler = data.getActionHandler();
    if (actionHandler == null) actionHandler = new ActionHandler();
    if (toolbar == null) {
      toolbar = new Form<>("topology.tree.toolbar");
      actionHandler.addActionLink(toolbar, new DriverStopRestartLink(toolbar));
      actionHandler.addActionLink(toolbar, new ServerResetStatsLink());
      actionHandler.addActionLink(toolbar, new SystemInfoLink(toolbar));
      actionHandler.addActionLink(toolbar, new NodeConfigLink(toolbar));
      actionHandler.addActionLink(toolbar, new NodeThreadsLink(toolbar));
      actionHandler.addActionLink(toolbar, new ResetTaskCounterLink());
      actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.STOP));
      actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.RESTART));
      actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.STOP_DEFERRED));
      actionHandler.addActionLink(toolbar, new StopRestartNodeLink(StopRestartNodeLink.ActionType.RESTART_DEFERRED));
      actionHandler.addActionLink(toolbar, new CancelPendingActionLink());
      actionHandler.addActionLink(toolbar, new SuspendNodeLink());
      actionHandler.addActionLink(toolbar, new ProvisioningLink(toolbar));
      actionHandler.addActionLink(toolbar, new ExpandAllLink());
      actionHandler.addActionLink(toolbar, new CollapseAllLink());
      actionHandler.addActionLink(toolbar, new SelectDriversLink());
      actionHandler.addActionLink(toolbar, new SelectNodesLink());
      actionHandler.addActionLink(toolbar, new SelectAllLink());
      data.setActionHandler(actionHandler);
    }
    return actionHandler;
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    TreeTableUtils.addDriver(topologyModel, event.getDriver());
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    TreeTableUtils.removeDriver(topologyModel, event.getDriver());
  }

  @Override
  public void driverUpdated(final TopologyEvent event) {
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    DefaultMutableTreeNode node = TreeTableUtils.addNode(topologyModel, event.getDriver(), event.getNodeOrPeer());
    if (node != null) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
      if (parent.getChildCount() == 1) tableTree.expand(parent);
    }
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    TreeTableUtils.removeNode(topologyModel, event.getDriver(), event.getNodeOrPeer());
  }

  @Override
  public synchronized void nodeUpdated(final TopologyEvent event) {
    if (event.getUpdateType() == TopologyEvent.UpdateType.NODE_STATE) {
      TreeTableUtils.updateNode(topologyModel, event.getDriver(), event.getNodeOrPeer());
    }
  }

  @Override
  public AjaxSelfUpdatingTimerBehavior getRefreshTimer() {
    return refreshTimer;
  }

  @Override
  public JPPFTableTree getTableTree() {
    return tableTree;
  }

  @Override
  public Form<String> getToolbar() {
    return toolbar;
  }

  /**
   * @return the tree table model.
   */
  public AbstractJPPFTreeTableModel getTopologyModel() {
    return topologyModel != null ? topologyModel : getJPPFSession().getTopologyData().getModel();
  }

  /**
   * This class renders cells of the first column as tree.
   */
  public class TopologyTreeColumn extends TreeColumn<DefaultMutableTreeNode, String> {
    /** Explicit serailVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this column.
     * @param displayModel the header display string.
     */
    public TopologyTreeColumn(final IModel<String> displayModel) {
      super(displayModel);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      super.populateItem(cellItem, componentId, rowModel);
      DefaultMutableTreeNode node = rowModel.getObject();
      AbstractTopologyComponent comp = (AbstractTopologyComponent) node.getUserObject();
      String cssClass = null;
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      boolean inactive = false;
      if (comp.isPeer()) cssClass = "peer";
      else if (comp.isNode()) {
        TopologyNode data = (TopologyNode) node.getUserObject();
        if (traceEnabled) log.trace("node status: {}", data.getStatus());
        inactive = !data.getManagementInfo().isActive();
        if (data.getStatus() == TopologyNodeStatus.UP) {
          if (inactive) cssClass = (selected) ? "tree_inactive_selected" : "tree_inactive";
          else cssClass = (selected) ? "tree_selected" : "node_up";
        }
        else cssClass = (selected) ? "tree_inactive_selected" : "node_tree_down";
      } else if (comp.isDriver()) {
        TopologyDriver driver = (TopologyDriver) node.getUserObject();
        if (driver.getConnection().getStatus().isWorkingStatus()) cssClass = (selected) ? "tree_selected" : "driver_up";
        else cssClass = (selected) ? "tree_inactive_selected" : "driver_down";
      }
      if (cssClass != null) cellItem.add(new AttributeModifier("class", cssClass));
    }
  }

  /**
   * This class renders cells of each columns except the first.
   */
  public class TopologyColumn extends AbstractColumn<DefaultMutableTreeNode, String> {
    /** Explicit serailVersionUID. */
    private static final long serialVersionUID = 1L;
    /**
     * The column index.
     */
    private final int index;

    /**
     * Initialize this column.
     * @param index the column index.
     */
    public TopologyColumn(final int index) {
      super(Model.of(topologyModel.getColumnName(index)));
      this.index = index;
      if (debugEnabled) log.debug("adding column index {}", index);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      NodeModel<DefaultMutableTreeNode> nodeModel = (NodeModel<DefaultMutableTreeNode>) rowModel;
      DefaultMutableTreeNode treeNode = nodeModel.getObject();
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      String value = (String) topologyModel.getValueAt(treeNode, index);
      cellItem.add(new Label(componentId, value));
      if (traceEnabled) log.trace(String.format("index %d populating value=%s, treeNode=%s", index, value, treeNode));
      String cssClass = null;
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      if (comp.isNode()) {
        TopologyNode data = (TopologyNode) treeNode.getUserObject();
        if (data.isNode()) {
          cssClass = (data.getStatus() == TopologyNodeStatus.UP) ? "node_up" : "node_down";
          switch (index) {
            case NodeTreeTableModel.NB_SLAVES:
            case NodeTreeTableModel.NB_TASKS:
              cssClass += " number";
              break;
            default:
              cssClass += " string";
              break;
          }
        }
      } else if (!selected) cssClass = "empty";
      if (selected && !comp.isPeer()) {
        if (cssClass == null) cssClass = "tree_selected";
        else cssClass += " tree_selected";
      }
      if (cssClass != null) cellItem.add(new AttributeModifier("class", cssClass));
    }

    @Override
    public String getCssClass() {
      switch (index) {
        case NodeTreeTableModel.NB_SLAVES:
        case NodeTreeTableModel.NB_TASKS:
          return "number";
      }
      return "string";
    }
  }
}