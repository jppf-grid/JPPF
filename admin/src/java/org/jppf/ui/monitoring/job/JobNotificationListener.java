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
package org.jppf.ui.monitoring.job;

import javax.management.*;

import org.apache.commons.logging.*;
import org.jppf.job.JobNotification;

/**
 * Implementation of a notification listener for processing of job events.
 */
public class JobNotificationListener implements NotificationListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JobNotificationListener.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * String identifying the driver that sends the notifications.
	 */
	private String driverName = null;
	/**
	 * The panel to which the notifications are delegated.
	 */
	private JobDataPanel jobDataPanel = null;

	/**
	 * Initialize this listener with the specified driver name.
	 * @param driverName - a string identifying the driver that sends the notifications.
	 * @param jobDataPanel - the panel to which the notifications are delegated.
	 */
	public JobNotificationListener(JobDataPanel jobDataPanel, String driverName)
	{
		this.driverName = driverName;
		this.jobDataPanel = jobDataPanel;
	}

	/**
	 * Handle notifications of job events.
	 * @param notification - encapsulates the job event ot handle.
	 * @param handback - not used.
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	public void handleNotification(Notification notification, Object handback)
	{
		if (!(notification instanceof JobNotification)) return;
		JobNotification notif = (JobNotification) notification;
		if (traceEnabled) log.trace("received notIfication: " + notif);
		switch(notif.getEventType())
		{
			case JOB_QUEUED:
				jobDataPanel.jobAdded(driverName, notif.getJobInformation());
				break;
			case JOB_ENDED:
				jobDataPanel.jobRemoved(driverName, notif.getJobInformation());
				break;
			case JOB_UPDATED:
				jobDataPanel.jobUpdated(driverName, notif.getJobInformation());
				break;
			case JOB_DISPATCHED:
				jobDataPanel.subJobAdded(driverName, notif.getJobInformation(), notif.getNodeInfo());
				break;
			case JOB_RETURNED:
				jobDataPanel.subJobRemoved(driverName, notif.getJobInformation(), notif.getNodeInfo());
				break;
		}
	}
}
