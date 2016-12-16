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

package org.jppf.admin.web.topology.provisioning;

import java.io.*;
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
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ProvisioningLink extends AbstractModalLink<ProvisioningForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ProvisioningLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * @param form .
   */
  public ProvisioningLink(final Form<String> form) {
    super(TopologyConstants.PROVISIONING_ACTION, Model.of("Provisioning"), "provisioning.png", ProvisioningPage.class, form);
    modal.setInitialWidth(600);
    modal.setInitialHeight(340);
  }

  @Override
  protected ProvisioningForm createForm() {
    return new ProvisioningForm(modal, new Runnable() { @Override public void run() { doOK(); } });
  }

  /**
   * Called when the ok button is closed.
   */
  private void doOK() {
    JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TableTreeData data = session.getTopologyData();
    List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    final CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
    for (DefaultMutableTreeNode treeNode: selectedNodes) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if ((comp.getParent() != null) && comp.isNode()) {
        JPPFManagementInfo info = comp.getManagementInfo();
        if ((info != null) && info.isMasterNode()) map.putValue((TopologyDriver) comp.getParent(), comp.getUuid());
      }
    }
    TypedProperties props = null;
    if (modalForm.isUseOverrides()) {
      try (Reader reader = new StringReader(modalForm.getOverrides())) {
        props = new TypedProperties().loadAndResolve(reader);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    for (Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
      TopologyDriver parent = entry.getKey();
      NodeSelector selector = new UuidSelector(entry.getValue());
      try {
        Map<String, Object> result = parent.getForwarder().provisionSlaveNodes(selector, modalForm.getNbSlaves(), modalForm.isInterrupt(), props);
        printForwardingRequestErrors(result);
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Prints the eventual errors resulting from a node forwarding request.
   * @param result the map containing the results for the request.
   */
  private void printForwardingRequestErrors(final Map<String, Object> result) {
    if (debugEnabled) {
      for (Map.Entry<String, Object> entry: result.entrySet()) {
        if (entry.getValue() instanceof Throwable) {
          Throwable t = (Throwable) entry.getValue();
          if (debugEnabled) log.debug("provisioning request for node '{}' resulted in error: {}", entry.getKey(), ExceptionUtils.getStackTrace(t));
        }
      }
    }
  }
}
