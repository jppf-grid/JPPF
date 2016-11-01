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

package org.jppf.admin.web.health;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.*;
import org.jppf.admin.web.topology.TopologyTreeData;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.CollectionMap;
import org.slf4j.*;

/**
 * This class represents the GC button and its associated action in the JVM Health view.
 * @author Laurent Cohen
 */
public class GCLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(GCLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize.
   */
  public GCLink() {
    super(HealthConstants.GC_ACTION, Model.of("GC"), "gc.gif");
    setEnabled(false);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on gc");
    JPPFWebSession session = JPPFWebSession.get();
    final TableTreeData data = session.getTopologyData();
    List<DefaultMutableTreeNode> selected = data.getSelectedTreeNodes();
    if (!selected.isEmpty()) {
      List<TopologyDriver> drivers = TopologyTreeData.getSelectedDrivers(selected);
      for (TopologyDriver driver: drivers) {
        try {
          JMXDriverConnectionWrapper jmx = driver.getJmx();
          if ((jmx != null) && jmx.isConnected()) jmx.getDiagnosticsProxy().gc();
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
      CollectionMap<TopologyDriver, String> nodesMap = TopologyTreeData.getNodesMultimap(selected);
      for (Map.Entry<TopologyDriver, Collection<String>> entry: nodesMap.entrySet()) {
        try {
          JMXDriverConnectionWrapper jmx = entry.getKey().getJmx();
          if ((jmx != null) && jmx.isConnected()) jmx.getNodeForwarder().gc(new UuidSelector(entry.getValue()));
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * GC action is enabled when the user has the manager role and at least one node or driver is selected. 
   */
  public static class Action extends AbstractManagerRoleAction {
    @Override
    public void setEnabled(final List<DefaultMutableTreeNode> selected) {
      enabled = !selected.isEmpty();
    }
  }
}
