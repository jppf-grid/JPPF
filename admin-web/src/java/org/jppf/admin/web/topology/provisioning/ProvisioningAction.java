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

package org.jppf.admin.web.topology.provisioning;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.utils.AbstractManagerRoleAction;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JPPFManagementInfo;

/**
 * 
 * @author Laurent Cohen
 */
public class ProvisioningAction extends AbstractManagerRoleAction {
  @Override
  public void setEnabled(final List<DefaultMutableTreeNode> selected) {
    enabled = false;
    for (DefaultMutableTreeNode treeNode: selected) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isNode()) {
        JPPFManagementInfo info = ((TopologyNode) comp).getManagementInfo();
        if ((info != null) && info.isMasterNode()) {
          enabled = true;
          break;
        }
      }
    }
  }
}
