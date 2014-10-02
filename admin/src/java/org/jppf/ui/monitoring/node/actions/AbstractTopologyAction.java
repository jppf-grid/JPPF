/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.ui.monitoring.node.actions;

import java.util.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.actions.AbstractUpdatableAction;
import org.jppf.utils.collections.*;

/**
 * Abstract superclass for all actions in the topology panel.
 * @author Laurent Cohen
 */
public abstract class AbstractTopologyAction extends AbstractUpdatableAction
{
  /**
   * Constant for an empty <code>TopologyData</code> array.
   */
  protected static final AbstractTopologyComponent[] EMPTY_TOPOLOGY_DATA_ARRAY = new AbstractTopologyComponent[0];
  /**
   * The object representing the JPPF nodes in the tree table.
   */
  protected AbstractTopologyComponent[] dataArray = EMPTY_TOPOLOGY_DATA_ARRAY;

  /**
   * Initialize this action.
   */
  protected AbstractTopologyAction()
  {
    BASE = "org.jppf.ui.i18n.NodeDataPage";
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements)
  {
    super.updateState(selectedElements);
    List<AbstractTopologyComponent> list = new ArrayList<>();
    for (Object o: selectedElements)
    {
      AbstractTopologyComponent data = (AbstractTopologyComponent) o;
      if (data.isNode())
      {
        JPPFManagementInfo info = ((TopologyNode) data).getManagementInfo();
        if (info != null) list.add(data);
      }
    }
    dataArray = list.toArray(list.isEmpty() ? EMPTY_TOPOLOGY_DATA_ARRAY : new AbstractTopologyComponent[list.size()]);
  }

  /**
   * Get a mapping of driver node forwarder MBeans to corresponding selected nodes.
   * @return a mapping of driver uuids to lists of node uuid values.
   */
  protected CollectionMap<TopologyDriver, String> getDriverMap()
  {
    CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
    for (AbstractTopologyComponent data: dataArray)
    {
      if (!data.isNode()) continue;
      TopologyDriver parent = (TopologyDriver) data.getParent();
      if (parent != null) map.putValue(parent, data.getUuid());
    }
    return map;
  }
}
