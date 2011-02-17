/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
import java.util.*;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.ui.monitoring.node.*;
import org.slf4j.*;

/**
 * This action stops a node.
 */
public class ServerStatisticsResetAction extends AbstractTopologyAction
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ServerStatisticsResetAction.class);

	/**
	 * Initialize this action.
	 */
	public ServerStatisticsResetAction()
	{
		setupIcon("/org/jppf/ui/resources/server_restart.gif");
		setupNameAndTooltip("driver.reset.statistics");
	}

	/**
	 * Update this action's enabled state based on a list of selected elements.
	 * This method sets the enabled state to true if at list one driver is selected in the tree.
	 * @param selectedElements a list of objects.
	 * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
	 */
	public void updateState(List<Object> selectedElements)
	{
		for (Object o: selectedElements)
		{
			if (!(o instanceof TopologyData)) continue;
			TopologyData data = (TopologyData) o;
			if (TopologyDataType.DRIVER.equals(data.getType()))
			{
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);
	}

	/**
	 * Perform the action.
	 * @param event encapsulates the source of the event and additional information.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		try
		{
			final List<JMXDriverConnectionWrapper> driverConnections = new ArrayList<JMXDriverConnectionWrapper>();
			for (Object o: selectedElements)
			{
				if (!(o instanceof TopologyData)) continue;
				TopologyData data = (TopologyData) o;
				if (TopologyDataType.DRIVER.equals(data.getType())) driverConnections.add((JMXDriverConnectionWrapper) data.getJmxWrapper());
			}
			Runnable r = new Runnable() {
				public void run() {
					for (JMXDriverConnectionWrapper jmx: driverConnections) {
						try {
							jmx.resetStatistics();
						}
						catch(Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			};
			new Thread(r).start();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

}
