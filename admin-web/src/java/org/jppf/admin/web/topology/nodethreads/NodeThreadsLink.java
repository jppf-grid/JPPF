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

package org.jppf.admin.web.topology.nodethreads;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.topology.TopologyConstants;
import org.jppf.admin.web.utils.AbstractModalLink;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class NodeThreadsLink extends AbstractModalLink<NodeThreadsForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeThreadsLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * @param form .
   */
  public NodeThreadsLink(final Form<String> form) {
    super(TopologyConstants.NODE_THREADS_ACTION, Model.of("Node thread pool"), "threads.gif", NodeThreadsPage.class, form);
    modal.setInitialWidth(350);
    modal.setInitialHeight(150);
  }

  @Override
  protected NodeThreadsForm createForm() {
    return new NodeThreadsForm(modal, new Runnable() { @Override public void run() { doOK(); } });
  }
  
  /**
   * Called when the ok button is clicked.
   */
  private void doOK() {
    final JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TableTreeData data = session.getTopologyData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    final CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
    for (final DefaultMutableTreeNode treeNode: selectedNodes) {
      final AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if ((comp.getParent() != null) && comp.isNode()) {
        final JPPFManagementInfo info = comp.getManagementInfo();
        if (info != null) map.putValue((TopologyDriver) comp.getParent(), comp.getUuid());
      }
    }
    for (final Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
      final TopologyDriver parent = entry.getKey();
      final NodeSelector selector = new UuidSelector(entry.getValue());
      try {
        parent.getForwarder().updateThreadPoolSize(selector, modalForm.getNbThreads());
        parent.getForwarder().updateThreadsPriority(selector, modalForm.getPriority());
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
