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

package org.jppf.admin.web.health;

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
import org.jppf.admin.web.health.threaddump.ThreadDumpLink;
import org.jppf.admin.web.health.thresholds.ThresholdsLink;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.ui.monitoring.LocalizedListItem;
import org.jppf.ui.monitoring.diagnostics.JVMHealthTreeTableModel;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.utils.*;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

/**
 *
 * @author Laurent Cohen
 */
@MountPath(AbstractJPPFPage.PATH_PREFIX + "health")
@AuthorizeInstantiation({"jppf-manager", "jppf-monitor"})
public class HealthPage extends AbstractTableTreePage {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(HealthPage.class);
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
  public HealthPage() {
    super(TreeViewType.HEALTH, "health");
    HealthTreeData data = JPPFWebSession.get().getHealthData();
    HealthTreeListener listener = (HealthTreeListener) data.getListener();
    listener.setTableTree(tableTree);
  }

  @Override
  protected List<? extends IColumn<DefaultMutableTreeNode, String>> createColumns() {
    List<IColumn<DefaultMutableTreeNode, String>> columns = new ArrayList<>();
    columns.add(new HealthTreeColumn(Model.of("Tree")));
    for (LocalizedListItem item: selectableLayout.getVisibleItems()) columns.add(new HealthColumn(item.index));
    return columns;
  }

  @Override
  protected void createActions() {
    ActionHandler actionHandler = JPPFWebSession.get().getTableTreeData(viewType).getActionHandler();
    actionHandler.addActionLink(toolbar, new GCLink());
    actionHandler.addActionLink(toolbar, new ThreadDumpLink(toolbar));
    actionHandler.addActionLink(toolbar, new HeapDumpLink());
    actionHandler.addActionLink(toolbar, new ThresholdsLink(toolbar));
    actionHandler.addActionLink(toolbar, new ExpandAllLink(HealthConstants.EXPAND_ALL_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new CollapseAllLink(HealthConstants.COLLAPSE_ALL_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new SelectDriversLink(HealthConstants.SELECT_DRIVERS_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new SelectNodesLink(HealthConstants.SELECT_NODES_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new SelectAllLink(HealthConstants.SELECT_ALL_ACTION, viewType));
  }

  /**
   * This class renders cells of the first column as tree.
   */
  public class HealthTreeColumn extends TreeColumn<DefaultMutableTreeNode, String> {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this column.
     * @param displayModel the header display string.
     */
    public HealthTreeColumn(final IModel<String> displayModel) {
      super(displayModel);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      super.populateItem(cellItem, componentId, rowModel);
      DefaultMutableTreeNode node = rowModel.getObject();
      AbstractTopologyComponent comp = (AbstractTopologyComponent) node.getUserObject();
      String cssClass = "default_cursor ";
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      boolean inactive = false;
      if (comp.isPeer()) cssClass += "peer ";
      else if (comp.isNode()) {
        TopologyNode data = (TopologyNode) node.getUserObject();
        //if (traceEnabled) log.trace("node status: {}", data.getStatus());
        inactive = !data.getManagementInfo().isActive();
        if (data.getStatus() == TopologyNodeStatus.UP) {
          if (inactive) cssClass += selected ? "tree_inactive_selected " : "tree_inactive ";
          else cssClass += selected ? "tree_selected " : "node_up ";
        }
        else cssClass += selected ? "tree_inactive_selected " : "node_tree_down ";
      } else if (comp.isDriver()) {
        TopologyDriver driver = (TopologyDriver) node.getUserObject();
        if (driver.getConnection().getStatus().isWorkingStatus()) cssClass += (selected) ? "tree_selected " : "driver_up ";
        else cssClass = (selected) ? "tree_inactive_selected" : "driver_down";
      }
      cellItem.add(new AttributeModifier("class", cssClass));
    }
  }

  /**
   * This class renders cells of each columns except the first.
   */
  public class HealthColumn extends AbstractColumn<DefaultMutableTreeNode, String> {
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
    public HealthColumn(final int index) {
      super(Model.of(treeModel.getColumnName(index)));
      this.index = index;
      if (debugEnabled) log.debug("adding column index {}", index);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      NodeModel<DefaultMutableTreeNode> nodeModel = (NodeModel<DefaultMutableTreeNode>) rowModel;
      DefaultMutableTreeNode treeNode = nodeModel.getObject();
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      String value = (String) treeModel.getValueAt(treeNode, index);
      cellItem.add(new Label(componentId, value));
      String css = "default_cursor ";
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      if (!comp.isPeer()) css += (selected ? "tree_selected" : getThresholdCssClass(comp)) + " " + getCssClass();
      cellItem.add(new AttributeModifier("class", css));
      if (traceEnabled && (index == 1)) log.trace(String.format("index=%d, value=%s, css=%s, comp=%s", index, value, css, comp));
    }

    @Override
    public String getCssClass() {
      switch (index) {
        case JVMHealthTreeTableModel.CPU_LOAD:
        case JVMHealthTreeTableModel.HEAP_MEM_MB:
        case JVMHealthTreeTableModel.HEAP_MEM_PCT:
        case JVMHealthTreeTableModel.NON_HEAP_MEM_MB:
        case JVMHealthTreeTableModel.NON_HEAP_MEM_PCT:
        case JVMHealthTreeTableModel.RAM_MB:
        case JVMHealthTreeTableModel.RAM_PCT:
        case JVMHealthTreeTableModel.SYSTEM_CPU_LOAD:
        case JVMHealthTreeTableModel.THREADS:
          return "default_cursor number";
      }
      return "default_cursor string";
    }

    /**
     * @param comp the tree component for which to determine the background color.
     * @return the css class that specifies the cell's background color.
     */
    private String getThresholdCssClass(final AbstractTopologyComponent comp) {
      HealthTreeData data = JPPFWebSession.get().getHealthData();
      HealthSnapshot snapshot = comp.getHealthSnapshot();
      double value = -1d;
      String css = "health_tree";
      AlertThresholds thresholds = null;
      if (SystemUtils.isOneOf(index, JVMHealthTreeTableModel.CPU_LOAD, JVMHealthTreeTableModel.SYSTEM_CPU_LOAD)) {
        thresholds = data.getCpuThresholds();
        switch (index) {
          case JVMHealthTreeTableModel.CPU_LOAD:
            value = snapshot.getCpuLoad();
            break;
          case JVMHealthTreeTableModel.SYSTEM_CPU_LOAD:
            value = snapshot.getSystemCpuLoad();
            break;
        }
      } else if (!SystemUtils.isOneOf(index, JVMHealthTreeTableModel.THREADS, JVMHealthTreeTableModel.URL)) {
        thresholds = data.getMemoryThresholds();
        switch (index) {
          case JVMHealthTreeTableModel.HEAP_MEM_PCT:
          case JVMHealthTreeTableModel.HEAP_MEM_MB:
            value = snapshot.getHeapUsedRatio();
            break;
          case JVMHealthTreeTableModel.NON_HEAP_MEM_PCT:
          case JVMHealthTreeTableModel.NON_HEAP_MEM_MB:
            value = snapshot.getNonheapUsedRatio();
            break;
          case JVMHealthTreeTableModel.RAM_PCT:
          case JVMHealthTreeTableModel.RAM_MB:
            value = snapshot.getRamUsedRatio();
            break;
        }
      }
      if ((thresholds != null) && (value >= 0d)) {
        value *= 100d;
        if (value >= thresholds.getCritical()) css = "health_critical";
        else if (value >= thresholds.getWarning()) css = "health_warning";
      }
      //if (traceEnabled) log.trace(String.format(""));
      return css;
    }
  }
}
