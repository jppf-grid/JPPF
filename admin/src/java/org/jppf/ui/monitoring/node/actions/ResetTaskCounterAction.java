/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

import java.awt.event.ActionEvent;
import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.ui.monitoring.node.TopologyData;

/**
 * This action resets the task counter of a node to 0.
 */
public class ResetTaskCounterAction extends AbstractTopologyAction
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(ResetTaskCounterAction.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this action.
	 */
	public ResetTaskCounterAction()
	{
		setupIcon("/org/jppf/ui/resources/reset.gif");
		putValue(NAME, "Reset task counter");
	}

	/**
	 * Update this action's enabled state based on a list of selected elements.
	 * @param selectedElements - a list of objects.
	 * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
	 */
	public void updateState(List<Object> selectedElements)
	{
		super.updateState(selectedElements);
		setEnabled(nodeDataArray.length > 0);
	}

	/**
	 * Perform the action.
	 * @param event not used.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		for (TopologyData data: nodeDataArray)
		{
			try
			{
				JMXNodeConnectionWrapper jmx = (JMXNodeConnectionWrapper) data.getJmxWrapper();
				jmx.resetTaskCounter();
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}
}
