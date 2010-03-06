/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import org.jppf.ui.actions.AbstractUpdatableAction;
import org.jppf.ui.monitoring.node.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractTopologyAction extends AbstractUpdatableAction
{
	/**
	 * The object rpresenting the JPPF nodes in the tree table.
	 */
	protected TopologyData[] nodeDataArray = new TopologyData[0];

	/**
	 * Update this action's enabled state based on a list of selected elements.
	 * @param selectedElements - a list of objects.
	 * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
	 */
	public void updateState(List<Object> selectedElements)
	{
		super.updateState(selectedElements);
		List<TopologyData> list = new ArrayList<TopologyData>();
		for (Object o: selectedElements)
		{
			TopologyData data = (TopologyData) o;
			if (TopologyDataType.NODE.equals(data.getType())) list.add(data);
		}
		nodeDataArray = list.toArray(new TopologyData[0]);
	}
}
