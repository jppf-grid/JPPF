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

import org.jppf.client.*;
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.slf4j.*;

/**
 * Instances of this class hold the information related to each node in the job data tree table.
 * @author Laurent Cohen
 */
public class JobData
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JobData.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The type of this job data object.
	 */
	private JobDataType type = null;
	/**
	 * A driver connection.
	 */
	private JPPFClientConnection clientConnection = null;
	/**
	 * Wrapper holding the connection to the JMX server on a driver. 
	 */
	private JMXDriverConnectionWrapper jmxWrapper = null;
	/**
	 * Information on the job or sub-job in a JPPF driver or node.
	 */
	private JobInformation jobInformation = null;
	/**
	 * Information on the JPPF node in which part of a job is executing.
	 */
	private JPPFManagementInfo nodeInformation = null;
	/**
	 * Proxy to the job management mbean.
	 */
	private DriverJobManagementMBean proxy = null;
	/**
	 * Receives notifications from the MBean.
	 */
	private NotificationListener notificationListener = null;

	/**
	 * Initialize this job data with the specified type.
	 * @param type - the type of this job data object as a <code>JobDataType</code> enum value.
	 */
	protected JobData(JobDataType type)
	{
		this.type = type;
	}

	/**
	 * Initialize this job data as a driver related object.
	 * @param clientConnection - a reference to the driver connection.
	 */
	public JobData(JPPFClientConnection clientConnection)
	{
		this(JobDataType.DRIVER);
		this.clientConnection = clientConnection;
		this.jmxWrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
	}

	/**
	 * Initialize this job data as a holding information about a job submitted to a driver.
	 * @param jobInformation - information on the job in a JPPF driver.
	 */
	public JobData(JobInformation jobInformation)
	{
		this(JobDataType.JOB);
		this.jobInformation = jobInformation;
	}

	/**
	 * Initialize this job data as a holding information about a sub-job dispatched to a node.
	 * @param jobInformation - information on the job in a JPPF driver.
	 * @param nodeInformation - information on the JPPF node in which part of a job is executing.
	 */
	public JobData(JobInformation jobInformation, JPPFManagementInfo nodeInformation)
	{
		this(JobDataType.SUB_JOB);
		this.jobInformation = jobInformation;
		this.nodeInformation = nodeInformation;
	}

	/**
	 * Get the type of this job data object.
	 * @return a <code>JobDataType</code> enum value.
	 */
	public JobDataType getType()
	{
		return type;
	}

	/**
	 * Get the wrapper holding the connection to the JMX server on a driver. 
	 * @return a <code>JMXDriverConnectionWrapper</code> instance.
	 */
	public JMXDriverConnectionWrapper getJmxWrapper()
	{
		return jmxWrapper;
	}

	/**
	 * Set the wrapper holding the connection to the JMX server on a driver. 
	 * @param jmxWrapper a <code>JMXDriverConnectionWrapper</code> instance.
	 */
	public void setJmxWrapper(JMXDriverConnectionWrapper jmxWrapper)
	{
		this.jmxWrapper = jmxWrapper;
	}

	/**
	 * Get the information on the job or sub-job in a JPPF driver or node.
	 * @return a <code>JobInformation</code> instance,
	 */
	public JobInformation getJobInformation()
	{
		return jobInformation;
	}

	/**
	 * Get the information on the JPPF node in which part of a job is executing.
	 * @return a <code>NodeManagementInfo</code> instance.
	 */
	public JPPFManagementInfo getNodeInformation()
	{
		return nodeInformation;
	}

	/**
	 * Get a reference to the procy to the job management mbean.
	 * @return a DriverJobManagementMBean instance.
	 */
	public DriverJobManagementMBean getProxy()
	{
		if (jmxWrapper == null) return null;
		if (proxy == null)
		{
			try
			{
				ObjectName objectName = new ObjectName(JPPFAdminMBean.DRIVER_JOB_MANAGEMENT_MBEAN_NAME);
				MBeanServerConnection mbsc = jmxWrapper.getMbeanConnection();
				proxy = (DriverJobManagementMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, objectName, DriverJobManagementMBean.class, true);
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				else log.warn(e.getMessage());
			}
		}
		return proxy;
	}

	/**
	 * Get a string representaiton of this object.
	 * @return a string representing this object.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String s = "";
		switch(type)
		{
			case DRIVER:
				s = (jmxWrapper == null) ? "unkown" : jmxWrapper.getId();
				break;
			case JOB:
				s = jobInformation.getJobId();
				break;
			case SUB_JOB:
				s = nodeInformation.getHost() + ":" + nodeInformation.getPort();
				break;
		}
		return s;
	}

	/**
	 * Get the MBean notification listener.
	 * @return a <code>NotificationListener</code> instance.
	 */
	public NotificationListener getNotificationListener()
	{
		return notificationListener;
	}

	/**
	 * Set the MBean notification listener.
	 * @param listener a <code>NotificationListener</code> instance.
	 * @throws Exception if any error occurs.
	 */
	public void changeNotificationListener(NotificationListener listener) throws Exception
	{
		if (notificationListener != null)
		{
			if (proxy != null) proxy.removeNotificationListener(notificationListener);
		}
		notificationListener = listener;
		if (notificationListener != null)
		{
			if (proxy != null) proxy.addNotificationListener(notificationListener, null, null);
		}
	}

	/**
	 * Get a reference to the driver connection.
	 * @return a <code>JPPFClientConnection</code> instance.
	 */
	public JPPFClientConnection getClientConnection()
	{
		return clientConnection;
	}
}
