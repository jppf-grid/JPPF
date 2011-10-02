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

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.ui.monitoring.node.TopologyData;
import org.slf4j.*;

/**
 * Attempts to restart a task that is currently running on a node.
 */
public class RestartTaskAction extends JPPFAbstractNodeAction
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(RestartTaskAction.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Id of the task to cancel.
	 */
	private String taskId = null;

	/**
	 * Initialize this action.
	 * @param taskId id of the task to cancel.
	 * @param dataArray - the information on the nodes this action applies to.
	 */
	public RestartTaskAction(String taskId, TopologyData...dataArray)
	{
		super(dataArray);
		setupIcon("/org/jppf/ui/resources/restart.gif");
		putValue(NAME, "Task id " + taskId);
		if (dataArray.length > 1) setEnabled(false);
	}

	/**
	 * Perform the action.
	 * @param event not used.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
    public void actionPerformed(ActionEvent event)
	{
		try
		{
			Runnable r = new Runnable()
			{
				@Override
                public void run()
				{
					try
					{
						((JMXNodeConnectionWrapper) dataArray[0].getJmxWrapper()).restartTask(taskId);
					}
					catch(Exception e)
					{
						log.error(e.getMessage(), e);
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
