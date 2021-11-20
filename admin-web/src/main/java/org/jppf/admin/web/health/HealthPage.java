/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.apache.wicket.*;
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
import org.jppf.admin.web.layout.SelectableLayoutImpl;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.diagnostics.*;
import org.jppf.management.diagnostics.provider.MonitoringConstants;
import org.jppf.ui.monitoring.LocalizedListItem;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.utils.configuration.*;
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
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean traceEnabled = log.isTraceEnabled();

  /**
   * Initialize this web page.
   */
  public HealthPage() {
    super(TreeViewType.HEALTH, "health");
    final HealthTreeData data = JPPFWebSession.get().getHealthData();
    final HealthTreeListener listener = (HealthTreeListener) data.getListener();
    listener.setTableTree(tableTree);
  }

  @Override
  protected List<? extends IColumn<DefaultMutableTreeNode, String>> createColumns() {
    final List<IColumn<DefaultMutableTreeNode, String>> columns = new ArrayList<>();
    columns.add(new HealthTreeColumn(Model.of("Tree")));
    for (LocalizedListItem item: selectableLayout.getVisibleItems()) {
      final HealthColumn column = new HealthColumn(item.getIndex());
      //column.getHeader().add(new AttributeModifier("title", LocalizationUtils.getLocalized(base, key, JPPFWebSession.get().getLocale())));
      columns.add(column);
    }
    return columns;
  }

  @Override
  protected void createActions() {
    final ActionHandler actionHandler = JPPFWebSession.get().getTableTreeData(viewType).getActionHandler();
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

  @Override
  protected void createSelectableLayout(final String propertyName) {
    final Locale locale = JPPFWebSession.get().getLocale();
    final List<JPPFProperty<?>> properties = MonitoringDataProviderHandler.getAllProperties();
    final List<LocalizedListItem> allItems = new ArrayList<>();
    for (int i=0; i<properties.size(); i++) {
      final JPPFProperty<?> prop = properties.get(i);
      allItems.add(new LocalizedListItem(prop.getName(), i + 1, prop.getShortLabel(locale), prop.getDocumentation(locale)));
    }
    selectableLayout = new SelectableLayoutImpl(allItems, propertyName);
  }

  /**
   * 
   * @param index the index of the rpoperty to retrieve.
   * @return the property.
   */
  private static JPPFProperty<?> getProperty(final int index) {
    final List<JPPFProperty<?>> properties = MonitoringDataProviderHandler.getAllProperties();
    return properties.get(index);
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
      final DefaultMutableTreeNode node = rowModel.getObject();
      final AbstractTopologyComponent comp = (AbstractTopologyComponent) node.getUserObject();
      String cssClass = "default_cursor ";
      final boolean selected = selectionHandler.isSelected(comp.getUuid());
      boolean inactive = false;
      if (comp.isPeer()) cssClass += "peer ";
      else if (comp.isNode()) {
        final TopologyNode data = (TopologyNode) node.getUserObject();
        inactive = !data.getManagementInfo().isActive();
        if (data.getStatus() == TopologyNodeStatus.UP) {
          if (inactive) cssClass += selected ? "tree_inactive_selected " : "tree_inactive ";
          else cssClass += selected ? "tree_selected " : "node_up ";
        }
        else cssClass += selected ? "tree_inactive_selected " : "node_tree_down ";
      } else if (comp.isDriver()) {
        final TopologyDriver driver = (TopologyDriver) node.getUserObject();
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
     * The column header tooltip.
     */
    private final String tooltip;

    /**
     * Initialize this column.
     * @param index the column index.
     */
    public HealthColumn(final int index) {
      //super(Model.of(treeModel.getColumnName(index)));
      super(Model.of(getProperty(index - 1).getShortLabel(JPPFWebSession.get().getLocale())));
      this.tooltip = getProperty(index - 1).getDocumentation(JPPFWebSession.get().getLocale());
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
      final boolean selected = selectionHandler.isSelected(comp.getUuid());
      String css = "";
      if (!comp.isPeer()) css += (selected ? "tree_selected" : getThresholdCssClass(comp));
      css += " " + getCssClass();
      cellItem.add(new AttributeModifier("class", css));
      if (traceEnabled && (index == 1)) log.trace(String.format("index=%d, value=%s, css=%s, comp=%s", index, value, css, comp));
    }

    @Override
    public String getCssClass() {
      final JPPFProperty<?> prop = getProperty(index - 1);
      if (prop instanceof NumberProperty) return "default_cursor number";
      return "default_cursor string";
    }

    /**
     * @param comp the tree component for which to determine the background color.
     * @return the css class that specifies the cell's background color.
     */
    private String getThresholdCssClass(final AbstractTopologyComponent comp) {
      final HealthTreeData data = JPPFWebSession.get().getHealthData();
      final HealthSnapshot snapshot = comp.getHealthSnapshot();
      double value = -1d;
      String css = "health_tree";
      final JPPFProperty<?> prop = getProperty(index - 1);
      AlertThresholds thresholds = null;
      boolean deadlocked = false;
      switch(prop.getName()) {
        case MonitoringConstants.LIVE_THREADS_COUNT:
        case MonitoringConstants.DEADLOCKED:
          deadlocked = snapshot.getBoolean(MonitoringConstants.DEADLOCKED);
          break;
        case MonitoringConstants.HEAP_USAGE_MB:
        case MonitoringConstants.HEAP_USAGE_RATIO:
          value = snapshot.getDouble(MonitoringConstants.HEAP_USAGE_RATIO);
          thresholds = data.getMemoryThresholds();
          break;
        case MonitoringConstants.NON_HEAP_USAGE_MB:
        case MonitoringConstants.NON_HEAP_USAGE_RATIO:
          value = snapshot.getDouble(MonitoringConstants.NON_HEAP_USAGE_RATIO);
          thresholds = data.getMemoryThresholds();
          break;
        case MonitoringConstants.RAM_USAGE_MB:
        case MonitoringConstants.RAM_USAGE_RATIO:
          value = snapshot.getDouble(MonitoringConstants.RAM_USAGE_RATIO);
          thresholds = data.getMemoryThresholds();
          break;
        case MonitoringConstants.PROCESS_CPU_LOAD:
          value = snapshot.getDouble(MonitoringConstants.PROCESS_CPU_LOAD);
          thresholds = data.getCpuThresholds();
          break;
        case MonitoringConstants.SYSTEM_CPU_LOAD:
          value = snapshot.getDouble(MonitoringConstants.SYSTEM_CPU_LOAD);
          thresholds = data.getCpuThresholds();
          break;
      }
      if ((thresholds != null) && (value >= 0d)) {
        if (value >= thresholds.getCritical()) css = "health_critical";
        else if (value >= thresholds.getWarning()) css = "health_warning";
      } else if (deadlocked) css = "health_deadlocked";
      return css;
    }

    @Override
    public Component getHeader(final String componentId) {
      final Component comp = super.getHeader(componentId);
      comp.add(new AttributeModifier("class", "default_cursor"));
      return comp.add(new AttributeModifier("title", tooltip));
    }
  }
}
