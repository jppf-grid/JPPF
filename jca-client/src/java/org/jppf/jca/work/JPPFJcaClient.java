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
package org.jppf.jca.work;

import java.io.ByteArrayInputStream;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.event.ClientConnectionStatusEvent;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.jca.work.submission.JPPFSubmissionManager;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.TypedProperties;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFJcaClient extends AbstractGenericClient
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFJcaClient.class);
	/**
	 * Keeps a list of the valid connections not currently executring tasks.
	 */
	private Vector<JPPFClientConnection> availableConnections;
	/**
	 * Manages asynchronous work submission to the JPPF driver.
	 */
	private JPPFSubmissionManager submissionManager = null;

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param configuration the object holding the JPPF configuration.
	 */
	public JPPFJcaClient(String uuid, String configuration)
	{
		super(uuid, configuration);
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 * @deprecated this method is deprecated, {@link #submit(JPPFJob) submit(JPPFJob)} should be used instead. 
	 */
	public List<JPPFTask> submit(List<JPPFTask> taskList, DataProvider dataProvider) throws Exception
	{
		JPPFJob job = new JPPFJob(dataProvider);
		for (JPPFTask task: taskList) job.addTask(task);
		return submit(job);
		/*
		JPPFSubmissionResult collector = new JPPFSubmissionResult(taskList.size());
		List<JPPFTask> result = null;
		while ((result == null) && !pools.isEmpty())
		{
			getClientConnection().submit(taskList, dataProvider, collector);
			result = collector.getResults();
		}
		return result;
		*/
	}

	/**
	 * Submit a JPPFJob for execution.
	 * @param job the job to execute.
	 * @return the results of the tasks' execution, as a list of <code>JPPFTask</code> instances for a blocking job, or null if the job is non-blocking.
	 * @throws Exception if an error occurs while sending the job for execution.
	 * @see org.jppf.client.AbstractJPPFClient#submit(org.jppf.client.JPPFJob)
	 */
	public List<JPPFTask> submit(JPPFJob job) throws Exception
	{
		return null;
	}

	/**
	 * Close this client and release all the resources it is using.
	 */
	public void close()
	{
		super.close();
	}

	/**
	 * Invoked when the status of a client connection has changed.
	 * @param event the event to notify of.
	 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
	 */
	public void statusChanged(ClientConnectionStatusEvent event)
	{
		super.statusChanged(event);
		JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
		if (log.isDebugEnabled()) log.debug("connection=" + c + ", availableConnections=" + availableConnections);
		switch(c.getStatus())
		{
			case ACTIVE:
				getAvailableConnections().add(c);
				break;
			default:
				getAvailableConnections().remove(c);
				break;
		}
		if (submissionManager != null) submissionManager.wakeUp();
	}

	/**
	 * Determine whether there is a client connection available for execution.
	 * @return true if at least one ocnnection is available, false otherwise.
	 */
	public boolean hasAvailableConnection()
	{
		return !getAvailableConnections().isEmpty();
	}

	/**
	 * Get the submission manager for thsi JPPF client.
	 * @return a <code>JPPFSubmissionManager</code> instance.
	 */
	public JPPFSubmissionManager getSubmissionManager()
	{
		return submissionManager;
	}

	/**
	 * Set the submission manager for thsi JPPF client.
	 * @param submissionManager a <code>JPPFSubmissionManager</code> instance.
	 */
	public void setSubmissionManager(JPPFSubmissionManager submissionManager)
	{
		this.submissionManager = submissionManager;
	}

	/**
	 * {@inheritDoc}
	 */
	protected AbstractJPPFClientConnection createConnection(String uuid, String name, JPPFConnectionInformation info)
	{
		return new JPPFJcaClientConnection(uuid, name, info, this);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void initConfig(Object configuration)
	{
		if (log.isDebugEnabled()) log.debug("initializing configuration:\n" + configuration);
		try
		{
			TypedProperties props = new TypedProperties();
			ByteArrayInputStream bais = new ByteArrayInputStream(((String) configuration).getBytes());
			props.load(bais);
			bais.close();
			config = props;
		}
		catch(Exception e)
		{
			log.error("Error while initializing the JPPF client configuration", e);
		}
		if (log.isDebugEnabled()) log.debug("config properties: " + config);
	}

	/**
	 * Get the list of available connections.
	 * @return a vector of connections instances.
	 */
	private Vector<JPPFClientConnection> getAvailableConnections()
	{
		if (availableConnections == null) availableConnections = new Vector<JPPFClientConnection>();
		return availableConnections;
	}
}
