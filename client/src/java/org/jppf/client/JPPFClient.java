/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;
import org.jppf.JPPFError;
import org.jppf.client.event.*;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient implements ClientConnectionStatusListener
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFClient.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The pool of threads used for submitting execution requests.
	 */
	private ExecutorService executor = null;
	/**
	 * Security credentials associated with the application.
	 */
	JPPFSecurityContext credentials = null;
	/**
	 * Total count of the tasks submitted by this client.
	 */
	private int totalTaskCount = 0;
	/**
	 * Contains all the connections pools in ascending priority order.
	 */
	private TreeMap<Integer, ClientPool> pools = new TreeMap<Integer, ClientPool>(new DescendingIntegerComparator());
	/**
	 * Unique universal identifier for this JPPF client.
	 */
	private String uuid = null;
	/**
	 * A list of all the connections initially created. 
	 */
	private List<JPPFClientConnection> allConnections = new ArrayList<JPPFClientConnection>();

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFClient()
	{
		this(new JPPFUuid().toString());
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public JPPFClient(String uuid)
	{
		this.uuid = uuid;
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
			TypedProperties props = JPPFConfiguration.getProperties();
			String driverNames = props.getString("jppf.drivers");
			if (debugEnabled) log.debug("list of drivers: "+driverNames);
			String[] names = null;
			// if config file is still used as with single client version
			if ((driverNames == null) || "".equals(driverNames.trim()))
			{
				names = new String[] { "" };
			}
			else
			{
				names = driverNames.split("\\s");
			}
			for (String s: names)
			{
				String prefix = "".equals(s) ? "" : s + ".";
				String name = "".equals(s) ? "default" : s;
				String host = props.getString(prefix + "jppf.server.host", "localhost");
				int driverPort = props.getInt(prefix + "app.server.port", 11112);
				int classServerPort = props.getInt(prefix + "class.server.port", 11111);
				int priority = props.getInt(prefix + "priority", 0);
				JPPFClientConnection c = new JPPFClientConnection(uuid, name, host, driverPort, classServerPort, priority);
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
			executor = Executors.newFixedThreadPool(Math.max(1, allConnections.size()));
			for (Integer priority: pools.keySet())
			{
				ClientPool pool = pools.get(priority);
				for (JPPFClientConnection c: pool.clientList)
				{
					executor.submit(new ConnectionInitializer(c));
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Get all the client connections handled by this JPPFClient. 
	 * @return a list of <code>JPPFClientConnection</code> instances.
	 */
	public List<JPPFClientConnection> getAllConnections()
	{
		return allConnections;
	}

	/**
	 * Get an available connection with the highest possible priority.
	 * @return a <code>JPPFClientConnection</code> with the highest possible priority.
	 */
	private JPPFClientConnection getClientConnection()
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
		List<JPPFTask> result = null;
		while ((result == null) && !pools.isEmpty())
		{
			try
			{
				result = getClientConnection().submit(taskList, dataProvider);
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
		getClientConnection().submitNonBlocking(taskList, dataProvider, listener);
	}

	/**
	 * Send a request to get the statistics collected by the JPPF server.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if an error occurred while trying to get the server statistics.
	 */
	public JPPFStats requestStatistics() throws Exception
	{
		return getClientConnection().requestStatistics();
	}

	/**
	 * Submit an admin request with the specified command name and parameters.
	 * @param password the current admin password.
	 * @param newPassword the new password if the password is to be changed, can be null.
	 * @param command the name of the command to submit.
	 * @param parameters the parameters of the command to submit, may be null.
	 * @return the reponse message from the server.
	 * @throws Exception if an error occurred while trying to send or execute the command.
	 */
	public String submitAdminRequest(String password, String newPassword, String command, Map<String, Object> parameters)
			throws Exception
	{
		return getClientConnection().submitAdminRequest(password, newPassword, command, parameters);
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
			log.info("Connection [" + c.name + "] failed");
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
				log.info("Connection [" + c.name + "] : resubmitting " +
					taskCount + "tasks for " + execCount + " executions");
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
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Instances of this class manage a list of client connections with the same priority.
	 */
	private class ClientPool
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
	private class DescendingIntegerComparator implements Comparator<Integer>
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
			//return o1.compareTo(o2);
		}
	}

	/**
	 * Wrapper class for the initialization of a client connection.
	 */
	private class ConnectionInitializer implements Runnable
	{
		/**
		 * The client connection to initialize.
		 */
		private JPPFClientConnection c = null;
		/**
		 * Instantiate this connection initializer with the specified client connection.
		 * @param c the client connection to initialize.
		 */
		public ConnectionInitializer(JPPFClientConnection c)
		{
			this.c = c;
		}

		/**
		 * Perform the initialization of a client connection.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			if (debugEnabled) log.debug("initializing driver connection '"+c+"'");
			c.init();
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
				log.error(e.getMessage(), e);
			}
		}
		if (executor != null) executor.shutdownNow();
	}
}
