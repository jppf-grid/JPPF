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

package org.jppf.admin.web.topology.nodeconfig;

import java.util.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.AbstractModalLink;
import org.jppf.admin.web.topology.*;
import org.jppf.admin.web.topology.systeminfo.SystemInfoLink;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.ui.monitoring.node.actions.NodeConfigurationAction;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class NodeConfigLink extends AbstractModalLink<NodeConfigForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SystemInfoLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * @param form .
   */
  public NodeConfigLink(final Form<String> form) {
    super(TopologyTree.NODE_CONFIG_ACTION, Model.of("Node configuration"), "update.gif", NodeConfigPage.class, form);
    modal.setInitialWidth(350);
    modal.setInitialHeight(475);
  }

  @Override
  protected NodeConfigForm createForm() {
    return new NodeConfigForm(modal, new Runnable() { @Override public void run() { doOK(); } });
  }

  /**
   * Called when the ok button is closed.
   */
  private void doOK() {
    JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TopologyTreeData data = session.getTopologyData();
    List<TopologyNode> selectedNodes = TopologyTreeData.getSelectedNodes(data.getSelectedTreeNodes());
    if (!selectedNodes.isEmpty()) {
      try {
        TopologyNode node = selectedNodes.get(0);
        TopologyDriver parent = (TopologyDriver) node.getParent();
        if (parent == null) return;
        Map<Object, Object> config = NodeConfigurationAction.getPropertiesAsMap(modalForm.getConfig());
        parent.getForwarder().updateConfiguration(new UuidSelector(node.getUuid()), config, modalForm.isForceRestart(), modalForm.isInterrupt());
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TopologyTreeData data = session.getTopologyData();
    List<TopologyNode> selectedNodes = TopologyTreeData.getSelectedNodes(data.getSelectedTreeNodes());
    String config = NodeConfigurationAction.getPropertiesAsString(selectedNodes.get(0));
    modalForm.setConfig(config);
    super.onClick(target);
  }
}
