/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFError;
import org.jppf.client.event.*;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFUuid;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClient implements ClientConnectionStatusListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractJPPFClient.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Security credentials associated with the application.
	 */
	protected JPPFSecurityContext credentials = null;
	/**
	 * Total count of the tasks submitted by this client.
	 */
	protected int totalTaskCount = 0;
	/**
	 * Contains all the connections pools in ascending priority order.
	 */
	protected TreeMap<Integer, ClientPool> pools = new TreeMap<Integer, ClientPool>(new DescendingIntegerComparator());
	/**
	 * Unique universal identifier for this JPPF client.
	 */
	protected String uuid = null;
	/**
	 * A list of all the connections initially created. 
	 */
	protected List<JPPFClientConnection> allConnections = new ArrayList<JPPFClientConnection>();

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public AbstractJPPFClient()
	{
		this(new JPPFUuid().toString());
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public AbstractJPPFClient(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	public abstract void initPools();

	/**
	 * Get all the client connections handled by this JPPFClient. 
	 * @return a list of <code>JPPFClientConnection</code> instances.
	 */
	public List<JPPFClientConnection> getAllConnections()
	{
		return allConnections;
	}

	/**
	 * Get the names of all the client connections handled by this JPPFClient. 
	 * @return a list of connection names as strings.
	 */
	public List<String> getAllConnectionNames()
	{
		List<String> names = new ArrayList<String>();
		for (JPPFClientConnection c: allConnections) names.add(c.getName());
		return names;
	}

	/**
	 * Get a connection given its name.
	 * @param name the name of the connection to find.
	 * @return a <code>JPPFClientConnection</code> with the highest possible priority.
	 */
	public JPPFClientConnection getClientConnection(String name)
	{
		for (JPPFClientConnection c: allConnections)
		{
			if (c.getName().equals(name)) return c;
		}
		return null;
	}

	/**
	 * Get an available connection with the highest possible priority.
	 * @return a <code>JPPFClientConnection</code> with the highest possible priority.
	 */
	protected JPPFClientConnection getClientConnection()
	{
		JPPFClientConnection client = null;
		while ((client == null) && !pools.isEmpty())
		{
			Iterator<Integer> poolIterator = pools.keySet().iterator();
			while (poolIterator.hasNext())
			{
				int priority = poolIterator.next();
				ClientPool pool = pools.get(priority);
				int size = pool.clientList.size();
				int count = 0;
				while (count < size)
				{
					JPPFClientConnection c = pool.nextClient();
					if (ACTIVE.equals(c.getStatus()))
					{
						client = c;
						break;
					}
					else if (FAILED.equals(c.getStatus()))
					{
						pool.clientList.remove(c);
						size--;
						if (pool.lastUsedIndex >= size) pool.lastUsedIndex--;
						if (pool.clientList.isEmpty())
						{
							poolIterator.remove();
						}
					}
					else if (CONNECTING.equals(c.getStatus()))
					{
						// nothing to do, just continue to the next connection or next pool
						// with a lower priority.
					}
					count++;
				}
			}
			if (pools.isEmpty())
			{
				throw new JPPFError("FATAL ERROR: No more driver connection available for this client");
			}
		}
		if (debugEnabled) log.debug("found client connection \"" + client + "\"");
		return client;
	}

	/**
	 * Initialize this client's security credentials.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initCredentials() throws Exception
	{
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
		JPPFResultCollector collector = new JPPFResultCollector(taskList.size());
		List<JPPFTask> result = null;
		while ((result == null) && !pools.isEmpty())
		{
			try
			{
				getClientConnection().submit(taskList, dataProvider, collector);
				result = collector.waitForResults();
			}
			catch(Exception e)
			{
				if (pools.isEmpty()) throw e;
			}
		}
		return result;
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param listener listener to notify whenever a set of results have been received.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void submitNonBlocking(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener)
		throws Exception
	{
		getClientConnection().submit(taskList, dataProvider, listener);
	}

	/**
	 * Invoked the status of a client connection has changed.
	 * @param event the event to notify of.
	 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
	 */
	public void statusChanged(ClientConnectionStatusEvent event)
	{
		JPPFClientConnection c = event.getJPPFClientConnection();
		if (c.getStatus().equals(JPPFClientConnectionStatus.FAILED))
		{
			log.info("Connection [" + c.getName() + "] failed");
			c.removeClientConnectionStatusListener(this);
			int priority = c.getPriority();
			ClientPool pool = pools.get(priority);
			if (pool != null)
			{
				pool.clientList.remove(c);
				if (pool.clientList.isEmpty())
				{
					pools.remove(priority);
				}
				if (pools.isEmpty())
				{
					throw new JPPFError("FATAL ERROR: No more driver connection available for this client");
				}
			}
			List<ClientExecution> toResubmit = c.close();
			int taskCount = 0;
			int execCount = toResubmit.size();
			for (ClientExecution exec: toResubmit)
			{
				if (exec.tasks != null) taskCount += exec.tasks.size();
			}
			if (taskCount > 0)
			{
				log.info("Connection [" + c.getName() + "] : resubmitting " + taskCount + "tasks for " + execCount + " executions");
			}
			if (c != null)
			{
				try
				{
					for (ClientExecution execution: toResubmit)
					{
						if (execution.isBlocking)
						{
							submit(execution.tasks, execution.dataProvider);
						}
						else
						{
							submitNonBlocking(execution.tasks, execution.dataProvider, execution.listener);
						}
					}
				}
				catch(Exception e)
				{
					log.error(e);
				}
			}
		}
	}

	/**
	 * Instances of this class manage a list of client connections with the same priority.
	 */
	public class ClientPool
	{
		/**
		 * The priority associated with this client pool.
		 */
		public int priority = 0;
		/**
		 * Index of the last used client in the pool.
		 */
		public int lastUsedIndex = 0;
		/**
		 * List of <code>JPPFClientConnection</code> instances with the same priority.
		 */
		public List<JPPFClientConnection> clientList = new ArrayList<JPPFClientConnection>();

		/**
		 * Get the next client connection.
		 * @return a <code>JPPFClientConnection</code> instances.
		 */
		public JPPFClientConnection nextClient()
		{
			lastUsedIndex = ++lastUsedIndex % clientList.size();
			return clientList.get(lastUsedIndex);
		}
	}

	/**
	 * This comparator defines a decending value order for integers.
	 */
	protected class DescendingIntegerComparator implements Comparator<Integer>
	{
		/**
		 * Compare two integers.
		 * @param o1 first integer to compare.
		 * @param o2 second integrto compare.
		 * @return -1 if o1 > o2, 0 if o1 == o2, 1 if o1 < o2
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Integer o1, Integer o2)
		{
			return o2 - o1; 
		}
	}

	/**
	 * Close this client and release all the resources it is using.
	 */
	public void close()
	{
		List<JPPFClientConnection> list = getAllConnections();
		for (JPPFClientConnection c: list)
		{
			try
			{
				c.close();
			}
			catch(Exception e)
			{
				log.error(e);
			}
		}
	}
}
