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
import org.apache.wicket.ajax.*;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.monitoring.node.NodeTreeTableModel;
import org.jppf.ui.utils.TreeTableUtils;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This web page displays the topology tree.
 * @author Laurent Cohen
 */
public class TopologyTree extends TemplatePage implements TopologyListener {
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
   * The tree table component.
   */
  private transient JPPFTableTree tableTree;
  /**
   * The tree table model.
   */
  private transient NodeTreeTableModel topologyModel;
  /**
   * Handles the selection of rows in the tree table.
   */
  private transient SelectionHandler selectionHandler;

  /**
   * Initialize this web page.
   */
  public TopologyTree() {
    JPPFWebSession session = getJPPFSession();
    selectionHandler = session.getTopologySelectionHandler();
    if (selectionHandler == null) {
      selectionHandler = new MultipleSelectionHandler().setFilter(new SelectionHandler.Filter() {
        @Override
        public boolean accepts(final DefaultMutableTreeNode node) {
          return (node != null) && !((AbstractTopologyComponent) node.getUserObject()).isPeer();
        }
      });
      session.setTopologySelectionHandler(selectionHandler);
    }
    Form<String> form = new Form<>("topology.tree.toolbar");
    form.add(new AjaxLink<String>("topology.info", Model.of("Info")) {
      @Override
      public void onClick(final AjaxRequestTarget target) {
        if (debugEnabled) log.debug("clicked on info!");
      }
    });
    form.add(new ExpandAllLink());
    form.add(new CollapseAllLink());
    form.add(new SelectDriversLink());
    form.add(new SelectNodesLink());
    form.add(new SelectAllLink());
    add(form);
    tableTree = createTableTree();
    tableTree.add(new WindowsTheme()); // adds windows-style handles on nodes with children
    tableTree.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
    if (debugEnabled) log.debug("table tree created");
    add(tableTree);
    if (debugEnabled) log.debug("table tree added to page");
  }

  /**
   * Create the tree table.
   * @return a {@link JPPFTableTree} instance.
   */
  private JPPFTableTree createTableTree() {
    JPPFWebSession session = getJPPFSession();
    JPPFTableTree tree = session.getTopologyTableTree();
    if (tree == null) {
      if (debugEnabled) log.debug("creating topology model");
      createTopologyModel();
      if (debugEnabled) log.debug("topology model created");
      tree = new JPPFTableTree("topology_tree", createColumns(), topologyModel, Integer.MAX_VALUE, selectionHandler, new TopologyNodeRenderer());
      DataTable<DefaultMutableTreeNode, String> table = tree.getTable();
      HeadersToolbar<String> header = new HeadersToolbar<>(table, null);
      table.addTopToolbar(header);
      DefaultMutableTreeNode root = (DefaultMutableTreeNode) topologyModel.getRoot();
      for (int i = 0; i < root.getChildCount(); i++) tree.expand((DefaultMutableTreeNode) root.getChildAt(i));
      if (debugEnabled) log.debug("tree created");
      session.setTopologyTableTree(tree);
    }
    return tree;
  }

  /**
   * Create and initialize the tree table model holding the drivers and nodes data.
   */
  private void createTopologyModel() {
    JPPFWebSession session = getJPPFSession();
    topologyModel = session.getTopologyModel();
    if (topologyModel == null) {
      JPPFWebConsoleApplication app = (JPPFWebConsoleApplication) getApplication();
      topologyModel = new NodeTreeTableModel(new DefaultMutableTreeNode(app.localize("tree.root.name")), session.getLocale());
      populateTreeTableModel();
      app.getTopologyManager().addTopologyListener(this);
      session.setTopologyModel(topologyModel);
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

  /**
   * @return the tree table model.
   */
  public NodeTreeTableModel getTopologyModel() {
    return topologyModel != null ? topologyModel : getJPPFSession().getTopologyModel();
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
      if (comp.isPeer()) cssClass = "peer";
      else if (comp.isNode()) {
        TopologyNode data = (TopologyNode) node.getUserObject();
        if (traceEnabled) log.trace("node status: {}", data.getStatus());
        cssClass = (data.getStatus() == TopologyNodeStatus.UP) ? "node_up" : "node_tree_down";
      } else if (comp.isDriver()) {
        TopologyDriver driver = (TopologyDriver) node.getUserObject();
        cssClass = (driver.getConnection().getStatus().isWorkingStatus()) ? "driver_up" : "driver_down";
      } else if (!selected) cssClass = "empty";
      if (selected && !comp.isPeer()) {
        if (cssClass == null) cssClass = "tree_selected";
        else cssClass += " tree_selected";
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