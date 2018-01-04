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

package org.jppf.admin.web.jobs.maxnodes;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.*;
import org.jppf.admin.web.jobs.JobsConstants;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.utils.AbstractModalLink;
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
public class MaxNodesLink extends AbstractModalLink<MaxNodesForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(MaxNodesLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * @param form .
   */
  public MaxNodesLink(final Form<String> form) {
    super(JobsConstants.UPDATE_MAX_NODES_ACTION, Model.of("Max nodes"), "select_nodes.gif", MaxNodesPage.class, form);
    modal.setInitialWidth(350);
    modal.setInitialHeight(150);
  }

  @Override
  protected MaxNodesForm createForm() {
    return new MaxNodesForm(modal, new Runnable() { @Override public void run() { doOK(); } });
  }
  
  /**
   * Called when the ok button is clicked.
   */
  private void doOK() {
    final JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TableTreeData data = session.getJobsData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    final CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
    for (final DefaultMutableTreeNode treeNode: selectedNodes) {
      final AbstractJobComponent comp = (AbstractJobComponent) treeNode.getUserObject();
      if ((comp instanceof Job) && (comp.getParent() != null)) {
        final Job job = (Job) comp;
        final List<JobDriver> drivers = JPPFWebConsoleApplication.get().getJobMonitor().getDriversForJob(job.getUuid());
        for (final JobDriver driver: drivers) map.putValue(driver.getTopologyDriver(), job.getUuid());
      }
    }
    final boolean unlimited = modalForm.isUnlimited();
    final int nbNodes = unlimited ? Integer.MAX_VALUE : modalForm.getNbNodes();
    for (final Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
      final TopologyDriver driver = entry.getKey();
      final JobSelector selector = new JobUuidSelector(entry.getValue());
      try {
        driver.getJobManager().updateMaxNodes(selector, nbNodes);
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
