/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
package org.jppf.jca.work;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.event.ClientConnectionStatusEvent;
import org.jppf.jca.work.submission.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFJcaClient extends AbstractJPPFClient
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFJcaClient.class);
	/**
	 * The number of connections in the pool.
	 */
	private int poolSize = 1;
	/**
	 * The host hostname of the JPPF driver.
	 */
	private String host = "localhost";
	/**
	 * The clasPort port used by the JPPF class loader.
	 */
	private int classPort = 11111;
	/**
	 * The poolSize port on which tasks are sent.
	 */
	private int appPort = 11112;
	/**
	 * Initial list of connections to initialize.
	 */
	private List<Runnable> initialWorkList = new ArrayList<Runnable>();
	/**
	 * The unique class server delegate for all connections.
	 */
	private ClassServerDelegate delegate = null;
	/**
	 * Keeps a list of the valid connections not currently executring tasks.
	 */
	private Vector<JPPFClientConnection> availableConnections = new Vector<JPPFClientConnection>();
	/**
	 * Manages asynchronous work submission to the JPPF driver.
	 */
	private JPPFSubmissionManager submissionManager = null;

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFJcaClient()
	{
		super();
		initPools();
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param poolSize number of connections in the pool.
	 * @param host hostname of the JPPF driver.
	 * @param classPort port used by the JPPF class loader.
	 * @param appPort port on which tasks are sent.
	 */
	public JPPFJcaClient(String uuid, int poolSize, String host, int classPort, int appPort)
	{
		super(uuid);
		this.poolSize = poolSize;
		this.host = host;
		this.appPort = appPort;
		initPools();
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	public void initPools()
	{
		try
		{
			delegate = new JcaClassServerDelegate("ra_driver", uuid, host, classPort, this);
			if (poolSize <= 0) poolSize = 1;
			String[] names = new String[poolSize];
			for (int i=0; i<poolSize; i++) names[i] = "driver_" + (i + 1);
			// if config file is still used as with single client version
			for (String name: names)
			{
				int priority = 0;
				JPPFClientConnection c = new JPPFJcaClientConnection(uuid, name, host, appPort, classPort, priority, this);
				c.addClientConnectionStatusListener(this);
				ClientPool pool = pools.get(priority);
				if (pool == null)
				{
					pool = new ClientPool();
					pool.priority = priority;
					pools.put(priority, pool);
				}
				pool.clientList.add(c);
			}
			for (int priority: pools.keySet())
			{
				ClientPool pool = pools.get(priority);
				for (JPPFClientConnection c: pool.clientList) allConnections.add(c);
			}
			for (Integer priority: pools.keySet())
			{
				ClientPool pool = pools.get(priority);
				for (JPPFClientConnection c: pool.clientList)
				{
					initialWorkList.add(new ConnectionInitializerTask(c));
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public List<JPPFTask> submit(List<JPPFTask> taskList, DataProvider dataProvider) throws Exception
	{
		JPPFSubmissionResult collector = new JPPFSubmissionResult(taskList.size());
		List<JPPFTask> result = null;
		while ((result == null) && !pools.isEmpty())
		{
			getClientConnection().submit(taskList, dataProvider, collector);
			result = collector.getResults();
		}
		return result;
	}

	/**
	 * Close this client and release all the resources it is using.
	 */
	public void close()
	{
		super.close();
	}

	/**
	 * Get the initial list of connections to initialize.
	 * @return a list of <code>Work</code> instances.
	 */
	public List<Runnable> getInitialWorkList()
	{
		return initialWorkList;
	}

	/**
	 * Return the class server delegate aoosicated with this JPPF client.
	 * @return a <code>ClassServerDelegate</code> instance. 
	 */
	public ClassServerDelegate getDelegate()
	{
		return delegate;
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
		switch(c.getStatus())
		{
			case ACTIVE:
				availableConnections.add(c);
				break;
			default:
				availableConnections.remove(c);
				break;
		}
	}

	/**
	 * Determine whether there is a client connection available for execution.
	 * @return true if at least one ocnnection is available, false otherwise.
	 */
	public boolean hasAvailableConnection()
	{
		return !availableConnections.isEmpty();
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
}
