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

package org.jppf.jca.spi;

import java.io.Serializable;

import javax.resource.*;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.*;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.*;
import org.jppf.jca.util.JPPFAccessorImpl;
import org.jppf.jca.work.*;
import org.jppf.jca.work.submission.JPPFSubmissionManager;
import org.jppf.utils.JPPFUuid;

/**
 * Implementation of the JPPF Resource Adapter for J2EE.
 * This class initiates a JPPF client with a pool of driver connections.
 * @author Laurent Cohen
 */
public class JPPFResourceAdapter extends JPPFAccessorImpl implements ResourceAdapter, Serializable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFJcaClient.class);
	/**
	 * Host name or IP address for the JPPF driver.
	 */
	private String serverHost = "localhost";
	/**
	 * Port for the class server in the driver.
	 */
	private int classServerPort = 11111;
	/**
	 * Port for the client application server in the driver.
	 */
	private int appServerPort = 11112;
	/**
	 * Number of JPPF driver connections.
	 */
	private int connectionPoolSize = 5;
	/**
	 * Bootstrap context provided by the application server.
	 */
	private transient BootstrapContext ctx = null;
	/**
	 * Manages asynchronous work submission to the JPPF driver.
	 */
	//private JPPFSubmissionManager submissionManager = null;

	/**
	 * Start this resource adapater with the specified bootstrap context.
	 * This method is invoked by the application server exactly once for each resource adapter instance. 
	 * @param ctx bootstrap context provided by the application server.
	 * @throws ResourceAdapterInternalException if an error occurred while starting this resource adapter.
	 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
	 */
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException
	{
		this.ctx = ctx;
		log.info("Starting JPPF resource adapter");
		WorkManager workManager = ctx.getWorkManager();
		jppfClient =
			new JPPFJcaClient(new JPPFUuid().toString(), connectionPoolSize, serverHost, classServerPort, appServerPort);
		log.info("Starting JPPF resource adapter: jppf client="+jppfClient);
		JPPFSubmissionManager submissionManager = new JPPFSubmissionManager(jppfClient, workManager);
		jppfClient.setSubmissionManager(submissionManager);
		try
		{
			workManager.scheduleWork((JcaClassServerDelegate) jppfClient.getDelegate());
		}
		catch(WorkException e)
		{
			log.error(e.getMessage(), e);
		}
		try
		{
			workManager.scheduleWork(new JPPFJcaJob(jppfClient.getInitialWorkList(), 1000));
		}
		catch(WorkException e)
		{
			log.error(e.getMessage(), e);
		}
		try
		{
			workManager.scheduleWork(submissionManager);
		}
		catch(WorkException e)
		{
			log.error(e.getMessage(), e);
		}
		log.info("JPPF resource adapter started");
	}

	/**
	 * Stop this resource adapter.
	 * @see javax.resource.spi.ResourceAdapter#stop()
	 */
	public void stop()
	{
		if (jppfClient != null) jppfClient.close();
	}

	/**
	 * Not supported.
	 * @param arg0 not used.
	 * @param arg1 not used.
	 * @throws ResourceException always.
	 * @see javax.resource.spi.ResourceAdapter#endpointActivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
	 */
	public void endpointActivation(MessageEndpointFactory arg0, ActivationSpec arg1) throws ResourceException
	{
		throw new NotSupportedException("Method not supported");
	}

	/**
	 * This method does nothing.
	 * @param arg0 not used.
	 * @param arg1 not used.
	 * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
	 */
	public void endpointDeactivation(MessageEndpointFactory arg0, ActivationSpec arg1) 
	{
	}

	/**
	 * This method does nothing.
	 * @param arg0 not used.
	 * @return null
	 * @throws ResourceException .
	 * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
	 */
	public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException
	{
		return null;
	}

	/**
	 * Get the host name or IP address for the JPPF driver.
	 * @return the host as a string. 
	 */
	public String getServerHost()
	{
		return serverHost;
	}

	/**
	 * Set the host name or IP address for the JPPF driver.
	 * @param serverHost the host as a string. 
	 */
	public void setServerHost(String serverHost)
	{
		this.serverHost = serverHost;
	}

	/**
	 * Get the port for the class server in the driver.
	 * @return the port number as an int. 
	 */
	public Integer getClassServerPort()
	{
		return classServerPort;
	}

	/**
	 * Set the port for the class server in the driver.
	 * @param classServerPort the port number as an int. 
	 */
	public void setClassServerPort(Integer classServerPort)
	{
		this.classServerPort = classServerPort;
	}

	/**
	 * Get the port for the client application server in the driver.
	 * @return the port number as an int. 
	 */
	public Integer getAppServerPort()
	{
		return appServerPort;
	}

	/**
	 * Get the port for the client application server in the driver.
	 * @param appServerPort the port number as an int.
	 */
	public void setAppServerPort(Integer appServerPort)
	{
		this.appServerPort = appServerPort;
	}

	/**
	 * Get the number of JPPF driver connections.
	 * @return the number of connections as an int.
	 */
	public Integer getConnectionPoolSize()
	{
		return connectionPoolSize;
	}

	/**
	 * Set the number of JPPF driver connections.
	 * @param connectionPoolSize the number of connections as an int.
	 */
	public void setConnectionPoolSize(Integer connectionPoolSize)
	{
		this.connectionPoolSize = connectionPoolSize;
	}
}
