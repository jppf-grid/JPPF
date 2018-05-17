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

package org.jppf.admin.web.topology;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.jppf.admin.web.*;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.topology.loadbalancing.LoadBalancingLink;
import org.jppf.admin.web.topology.nodeconfig.NodeConfigLink;
import org.jppf.admin.web.topology.nodethreads.NodeThreadsLink;
import org.jppf.admin.web.topology.provisioning.ProvisioningLink;
import org.jppf.admin.web.topology.serverstop.DriverStopRestartLink;
import org.jppf.admin.web.topology.systeminfo.SystemInfoLink;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.monitoring.LocalizedListItem;
import org.jppf.ui.monitoring.node.NodeTreeTableModel;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

/**
 * This web page displays the topology tree.
 * @author Laurent Cohen
 */
@MountPath(AbstractJPPFPage.PATH_PREFIX + "topology")
@AuthorizeInstantiation({"jppf-manager", "jppf-monitor"})
public class TopologyPage extends AbstractTableTreePage {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TopologyPage.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean traceEnabled = log.isTraceEnabled();

  /**
   * Initialize this web page.
   */
  public TopologyPage() {
    super(TreeViewType.TOPOLOGY, "topology");
    final TopologyTreeData data = JPPFWebSession.get().getTopologyData();
    final TopologyTreeListener listener = (TopologyTreeListener) data.getListener();
    listener.setTableTree(tableTree);
  }

  @Override
  protected List<? extends IColumn<DefaultMutableTreeNode, String>> createColumns() {
    final List<IColumn<DefaultMutableTreeNode, String>> columns = new ArrayList<>();
    columns.add(new TopologyTreeColumn(Model.of("Tree")));
    for (final LocalizedListItem item: selectableLayout.getVisibleItems()) columns.add(new TopologyColumn(item.getIndex()));
    return columns;
  }

  @Override
  protected void createActions() {
    final ActionHandler actionHandler = JPPFWebSession.get().getTableTreeData(viewType).getActionHandler();
    actionHandler.addActionLink(toolbar, new DriverStopRestartLink(toolbar));
    actionHandler.addActionLink(toolbar, new ServerResetStatsLink());
    actionHandler.addActionLink(toolbar, new LoadBalancingLink(toolbar));
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
    actionHandler.addActionLink(toolbar, new ExpandAllLink(TopologyConstants.EXPAND_ALL_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new CollapseAllLink(TopologyConstants.COLLAPSE_ALL_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new SelectDriversLink(TopologyConstants.SELECT_DRIVERS_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new SelectNodesLink(TopologyConstants.SELECT_NODES_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new SelectAllLink(TopologyConstants.SELECT_ALL_ACTION, viewType));
  }

  /**
   * This class renders cells of the first column as tree.
   */
  public class TopologyTreeColumn extends TreeColumn<DefaultMutableTreeNode, String> {
    /** Explicit serialVersionUID. */
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
      final DefaultMutableTreeNode node = rowModel.getObject();
      final AbstractTopologyComponent comp = (AbstractTopologyComponent) node.getUserObject();
      String cssClass = "default_cursor ";
      final boolean selected = selectionHandler.isSelected(comp.getUuid());
      boolean inactive = false;
      if (comp.isPeer()) cssClass += "peer ";
      else if (comp.isNode()) {
        final TopologyNode data = (TopologyNode) node.getUserObject();
        if (traceEnabled) log.trace("node status: {}", data.getStatus());
        inactive = !data.getManagementInfo().isActive();
        if (data.getStatus() == TopologyNodeStatus.UP) {
          if (inactive) cssClass += (selected) ? "tree_inactive_selected " : "tree_inactive ";
          else cssClass += (selected) ? "tree_selected " : "node_up ";
        }
        else cssClass += (selected) ? "tree_inactive_selected " : "node_tree_down ";
      } else if (comp.isDriver()) {
        final TopologyDriver driver = (TopologyDriver) node.getUserObject();
        if (driver.getConnection().getStatus().isWorkingStatus()) cssClass += (selected) ? "tree_selected " : "driver_up ";
        else cssClass += (selected) ? "tree_inactive_selected " : "driver_down ";
      }
      cellItem.add(new AttributeModifier("class", cssClass));
    }
  }

  /**
   * This class renders cells of each columns except the first.
   */
  public class TopologyColumn extends AbstractColumn<DefaultMutableTreeNode, String> {
    /** Explicit serialVersionUID. */
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
      super(Model.of(treeModel.getColumnName(index)));
      this.index = index;
      if (debugEnabled) log.debug("adding column index {}", index);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      final NodeModel<DefaultMutableTreeNode> nodeModel = (NodeModel<DefaultMutableTreeNode>) rowModel;
      final DefaultMutableTreeNode treeNode = nodeModel.getObject();
      final AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      final String value = (String) treeModel.getValueAt(treeNode, index);
      cellItem.add(new Label(componentId, value));
      if (traceEnabled) log.trace(String.format("index %d populating value=%s, treeNode=%s", index, value, treeNode));
      String cssClass = "default_cursor ";
      final boolean selected = selectionHandler.isSelected(comp.getUuid());
      if (comp.isNode()) {
        final TopologyNode data = (TopologyNode) treeNode.getUserObject();
        if (data.isNode()) cssClass += ((data.getStatus() == TopologyNodeStatus.UP) ? "node_up " : "node_down ") + getCssClass();
      } else if (!selected) cssClass += "empty ";
      if (selected && !comp.isPeer()) cssClass += "tree_selected ";
      cellItem.add(new AttributeModifier("class", cssClass));
    }

    @Override
    public String getCssClass() {
      switch (index) {
        case NodeTreeTableModel.NB_SLAVES:
        case NodeTreeTableModel.NB_TASKS:
          return "default_cursor number ";
      }
      return "default_cursor string ";
    }
  }
}
