/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.server.job.management.DriverJobManagementMBean;

/**
 * Instances of this class hold the information related to each node in the job data tree table.
 * @author Laurent Cohen
 */
public class JobData
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JobData.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The type of this job data object.
	 */
	private JobDataType type = null;
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
	private NodeManagementInfo nodeInformation = null;
	/**
	 * Proxy to the job management mbean.
	 */
	private DriverJobManagementMBean proxy = null;

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
	 * @param jmxWrapper - a wrapper holding the connection to the JMX server on a driver.
	 */
	public JobData(JMXDriverConnectionWrapper jmxWrapper)
	{
		this(JobDataType.DRIVER);
		this.jmxWrapper = jmxWrapper;
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
	public JobData(JobInformation jobInformation, NodeManagementInfo nodeInformation)
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
	public NodeManagementInfo getNodeInformation()
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
}
