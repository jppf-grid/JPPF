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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.*;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.job.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ResumeJobLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ResumeJobLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * 
   */
  public ResumeJobLink() {
    super(JobsConstants.RESUME_ACTION, Model.of("Resume job"), "resume.gif");
    setEnabled(false);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on resume");
    JPPFWebSession session = JPPFWebSession.get();
    final TableTreeData data = session.getJobsData();
    List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      final CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
      for (DefaultMutableTreeNode treeNode: selectedNodes) {
        AbstractJobComponent comp = (AbstractJobComponent) treeNode.getUserObject();
        if ((comp instanceof Job) && (comp.getParent() != null)) {
          Job job = (Job) comp;
          if (job.getJobInformation().isSuspended()) {
            List<JobDriver> drivers = JPPFWebConsoleApplication.get().getJobMonitor().getDriversForJob(job.getUuid());
            for (JobDriver driver: drivers) map.putValue(driver.getTopologyDriver(), job.getUuid());
          }
        }
      }
      for (Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
        TopologyDriver driver = entry.getKey();
        JobSelector selector = new JobUuidSelector(entry.getValue());
        try {
          driver.getJobManager().resumeJobs(selector);
        } catch(Exception e) {
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
      enabled = false;
      for (DefaultMutableTreeNode treeNode: selected) {
        if (treeNode.getUserObject() instanceof Job) {
          Job job = (Job) treeNode.getUserObject();
          if (job.getJobInformation().isSuspended()) {
            enabled = true;
            break;
          }
        }
      }
    }
  }
}
