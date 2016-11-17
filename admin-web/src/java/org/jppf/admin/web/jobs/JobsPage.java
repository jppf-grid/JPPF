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

package org.jppf.admin.web.jobs;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.jppf.admin.web.*;
import org.jppf.admin.web.jobs.maxnodes.MaxNodesLink;
import org.jppf.admin.web.jobs.priority.PriorityLink;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.ui.monitoring.job.JobTreeTableModel;
import org.jppf.ui.treetable.TreeViewType;
import org.jppf.ui.utils.JobsUtils;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

/**
 * The jobs view page.
 * @author Laurent Cohen
 */
@MountPath("jobs")
public class JobsPage extends AbstractTableTreePage {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JobsPage.class);
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
  public JobsPage() {
    super(TreeViewType.JOBS, "jobs");
    JobsTreeData data = JPPFWebSession.get().getJobsData();
    JobsTreeListener listener = (JobsTreeListener) data.getListener();
    if (listener == null) {
      listener = new JobsTreeListener(treeModel, selectionHandler);
      listener.setTableTree(tableTree);
      data.setListener(listener);
      JPPFWebConsoleApplication.get().getJobMonitor().addJobMonitoringListener(listener);
    } else listener.setTableTree(tableTree);
  }

  @Override
  protected void createTreeTableModel() {
    JPPFWebSession session = JPPFWebSession.get();
    TableTreeData data = session.getTableTreeData(viewType);
    treeModel = data.getModel();
    if (treeModel == null) {
      treeModel = new JobTreeTableModel(new DefaultMutableTreeNode("tree.root.name"), session.getLocale());
      // populate the tree table model
      for (JobDriver driver: JPPFWebConsoleApplication.get().getJobMonitor().getJobDrivers()) {
        JobsUtils.addDriver(treeModel, driver);
        for (Job job: driver.getJobs()) {
          JobsUtils.addJob(treeModel, driver, job);
          for (JobDispatch dispatch: job.getJobDispatches()) JobsUtils.addJobDispatch(treeModel, job, dispatch);
        }
      }
      data.setModel(treeModel);
    }
  }

  /**
   * @return the list of columns.
   */
  @Override
  protected List<? extends IColumn<DefaultMutableTreeNode, String>> createColumns() {
    List<IColumn<DefaultMutableTreeNode, String>> columns = new ArrayList<>();
    Locale locale = getSession().getLocale();
    if (locale == null) locale = Locale.US;
    columns.add(new JobTreeColumn(Model.of("Tree")));
    columns.add(new JobColumn(JobTreeTableModel.JOB_STATE));
    columns.add(new JobColumn(JobTreeTableModel.INITIAL_TASK_COUNT));
    columns.add(new JobColumn(JobTreeTableModel.TASK_COUNT));
    columns.add(new JobColumn(JobTreeTableModel.PRIORITY));
    columns.add(new JobColumn(JobTreeTableModel.MAX_NODES));
    return columns;
  }

  @Override
  protected void createActions() {
    ActionHandler actionHandler = JPPFWebSession.get().getTableTreeData(viewType).getActionHandler();
    actionHandler.addActionLink(toolbar, new CancelJobLink());
    actionHandler.addActionLink(toolbar, new SuspendJobLink(false));
    actionHandler.addActionLink(toolbar, new SuspendJobLink(true));
    actionHandler.addActionLink(toolbar, new ResumeJobLink());
    actionHandler.addActionLink(toolbar, new MaxNodesLink(toolbar));
    actionHandler.addActionLink(toolbar, new PriorityLink(toolbar));
    actionHandler.addActionLink(toolbar, new SelectJobsLink(JobsConstants.SELECT_JOBS_ACTION));
    actionHandler.addActionLink(toolbar, new ExpandAllLink(JobsConstants.EXPAND_ALL_ACTION, viewType));
    actionHandler.addActionLink(toolbar, new CollapseAllLink(JobsConstants.COLLAPSE_ALL_ACTION, viewType));
  }

  /**
   * This class renders cells of the first column as tree.
   */
  public class JobTreeColumn extends TreeColumn<DefaultMutableTreeNode, String> {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this column.
     * @param displayModel the header display string.
     */
    public JobTreeColumn(final IModel<String> displayModel) {
      super(displayModel);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      super.populateItem(cellItem, componentId, rowModel);
      DefaultMutableTreeNode node = rowModel.getObject();
      AbstractJobComponent comp = (AbstractJobComponent) node.getUserObject();
      String cssClass = null;
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      boolean inactive = false;
      if (comp instanceof Job) {
        Job data = (Job) comp;
        inactive = data.getJobInformation().isSuspended();
        if (inactive) cssClass = (selected) ? "tree_inactive_selected" : "tree_inactive";
        else cssClass = (selected) ? "tree_selected" : "node_up";
      } else if (comp instanceof JobDriver) {
        TopologyDriver driver = ((JobDriver) comp).getTopologyDriver();
        if (driver.getConnection().getStatus().isWorkingStatus()) cssClass = (selected) ? "tree_selected" : "driver_up";
        else cssClass = (selected) ? "tree_inactive_selected" : "driver_down";
      } else if (comp instanceof JobDispatch) {
        cssClass = "node_up";
      }
      if (cssClass != null) cellItem.add(new AttributeModifier("class", cssClass));
    }
  }

  /**
   * This class renders cells of each columns except the first.
   */
  public class JobColumn extends AbstractColumn<DefaultMutableTreeNode, String> {
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
    public JobColumn(final int index) {
      super(Model.of(treeModel.getColumnName(index)));
      this.index = index;
      if (debugEnabled) log.debug("adding column index {}", index);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<DefaultMutableTreeNode>> cellItem, final String componentId, final IModel<DefaultMutableTreeNode> rowModel) {
      NodeModel<DefaultMutableTreeNode> nodeModel = (NodeModel<DefaultMutableTreeNode>) rowModel;
      DefaultMutableTreeNode treeNode = nodeModel.getObject();
      AbstractJobComponent comp = (AbstractJobComponent) treeNode.getUserObject();
      String value = (String) treeModel.getValueAt(treeNode, index);
      cellItem.add(new Label(componentId, value));
      if (traceEnabled) log.trace(String.format("index %d populating value=%s, treeNode=%s", index, value, treeNode));
      String cssClass = null;
      boolean selected = selectionHandler.isSelected(comp.getUuid());
      if ((comp instanceof Job) || (comp instanceof JobDispatch)) cssClass = "node_up " + getCssClass();
      else if (!selected) cssClass = "empty";
      if (selected) {
        if (cssClass == null) cssClass = "tree_selected";
        else cssClass += " tree_selected";
      }
      if (cssClass != null) cellItem.add(new AttributeModifier("class", cssClass));
    }

    @Override
    public String getCssClass() {
      switch (index) {
        case JobTreeTableModel.INITIAL_TASK_COUNT:
        case JobTreeTableModel.TASK_COUNT:
        case JobTreeTableModel.MAX_NODES:
        case JobTreeTableModel.PRIORITY:
          return "number";
      }
      return "string";
    }
  }
}
