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

package org.jppf.logging.jmx;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

/**
 * Implementation of a  simple logger that sends logged messages as JMX notifications.
 * @author Laurent Cohen
 */
public class JmxLoggerImpl extends NotificationBroadcasterSupport implements JmxLogger
{
	/**
	 * The mbrean object name sent with the notifications.
	 */
	private static final ObjectName OBJECT_NAME = makeObjectName();
	/**
	 * Sequence number generator.
	 */
	private static AtomicLong sequence = new AtomicLong(0);

	/**
	 * Default constructor.
	 */
	public JmxLoggerImpl()
	{
	}

	/**
	 * Log the specified message as a JMX notification.
	 * @param message the message to log.
	 */
	public void log(String message)
	{
		Notification notif = new Notification("JmxLogNotification", OBJECT_NAME, sequence.incrementAndGet(), message);
		sendNotification(notif);
	}

	/**
	 * Create the {@link ObjectName} used as source of the notifications.
	 * @return an {@link ObjectName} instance.
	 */
	private static ObjectName makeObjectName()
	{
		try
		{
			return new ObjectName(JmxLogger.DEFAULT_MBEAN_NAME);
		}
		catch(Exception e)
		{
			System.out.println("Error: failed to send JMX log notification (" + e.getMessage() + ")");
		}
		return null;
	}
}
