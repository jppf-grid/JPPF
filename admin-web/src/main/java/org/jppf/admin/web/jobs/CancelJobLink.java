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

package org.jppf.admin.web.jobs;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.*;
import org.jppf.admin.web.tabletree.TableTreeData;
import org.jppf.admin.web.utils.*;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.job.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class CancelJobLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(CancelJobLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();

  /**
   * 
   */
  public CancelJobLink() {
    super(JobsConstants.CANCEL_ACTION, Model.of("Cancel job"), "terminate.gif");
    setEnabled(false);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on cancel");
    final JPPFWebSession session = JPPFWebSession.get();
    final TableTreeData data = session.getJobsData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      final CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
      for (final DefaultMutableTreeNode treeNode: selectedNodes) {
        final AbstractJobComponent comp = (AbstractJobComponent) treeNode.getUserObject();
        if ((comp instanceof Job) && (comp.getParent() != null)) {
          final Job job = (Job) comp;
          final List<JobDriver> drivers = JPPFWebConsoleApplication.get().getJobMonitor().getDriversForJob(job.getUuid());
          for (final JobDriver driver: drivers) map.putValue(driver.getTopologyDriver(), job.getUuid());
        }
      }
      for (final Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
        final TopologyDriver driver = entry.getKey();
        final JobSelector selector = new JobUuidSelector(entry.getValue());
        try {
          driver.getJobManager().cancelJobs(selector);
        } catch(final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * 
   */
  public static class Action extends AbstractManagerRoleAction {
    @Override
    public void setEnabled(final List<DefaultMutableTreeNode> selected) {
      enabled = isJobSelected(selected);
    }
  }
}
