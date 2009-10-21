/*
 * JPPF.
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
package org.jppf.ui.monitoring.job.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.ui.monitoring.job.JobData;

/**
 * This action stops a job.
 */
public class CancelJobAction extends AbstractJobAction
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(CancelJobAction.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this action.
	 */
	public CancelJobAction()
	{
		setupIcon("/org/jppf/ui/resources/terminate.gif");
		putValue(NAME, localize("job.terminate.label"));
	}

	/**
	 * Update this action's enabled state based on a list of selected elements.
	 * @param selectedElements - a list of objects.
	 * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
	 */
	public void updateState(List<Object> selectedElements)
	{
		super.updateState(selectedElements);
		setEnabled(jobDataArray.length > 0);
	}

	/**
	 * Perform the action.
	 * @param event not used.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				for (JobData data: jobDataArray)
				{
					try
					{
						((JMXDriverConnectionWrapper) data.getJmxWrapper()).cancelJob(data.getJobInformation().getJobId());
					}
					catch(Exception e)
					{
						log.error(e.getMessage(), e);
					}
				}
			}
		};
		new Thread(r).start();
	}
}
