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
package org.jppf.client;

import java.io.Serializable;
import java.util.*;

import org.jppf.client.event.*;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

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
	private static Logger log = LoggerFactory.getLogger(AbstractJPPFClient.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
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
	 * List of listeners to this JPPF client.
	 */
	protected List<ClientListener> listeners = new ArrayList<ClientListener>();

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	protected AbstractJPPFClient()
	{
		this(new JPPFUuid().toString());
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	protected AbstractJPPFClient(String uuid)
	{
		this.uuid = uuid;
		if (debugEnabled) log.debug("Instantiating JPPF client with uuid=" + uuid);
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	protected abstract void initPools();

	/**
	 * Get all the client connections handled by this JPPFClient. 
	 * @return a list of <code>JPPFClientConnection</code> instances.
	 */
	public List<JPPFClientConnection> getAllConnections()
	{
		return new ArrayList<JPPFClientConnection>(allConnections);
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
	public JPPFClientConnection getClientConnection()
	{
		return getClientConnection(false);
	}

	/**
	 * Get an available connection with the highest possible priority.
	 * @param oneAttempt determines whether this method should wait until a connection
	 * becomes available (ACTIVE status) or fail immeditately if no available connection is found.<br>
	 * This enables the excution to be performed locally if the client is not connected to a server.
	 * @return a <code>JPPFClientConnection</code> with the highest possible priority.
	 */
	public JPPFClientConnection getClientConnection(boolean oneAttempt)
	{
		JPPFClientConnection connection = null;
		synchronized(pools)
		{
			while ((connection == null) && !pools.isEmpty())
			{
				Set<Integer> toRemove = new HashSet<Integer>();
				Iterator<Integer> poolIterator = pools.keySet().iterator();
				while ((connection == null) && poolIterator.hasNext())
				{
					int priority = poolIterator.next();
					ClientPool pool = pools.get(priority);
					int size = pool.clientList.size();
					int count = 0;
					while ((connection == null) && (count < pool.size()))
					{
						JPPFClientConnection c = pool.nextClient();
						if (c == null) break;
						switch(c.getStatus())
						{
							case ACTIVE:
								connection = c;
								break;
							case FAILED:
								pool.clientList.remove(c);
								size--;
								if (pool.getLastUsedIndex() >= size) pool.setLastUsedIndex(pool.getLastUsedIndex() - 1);
								if (pool.clientList.isEmpty()) toRemove.add(priority);
								break;
						}
						count++;
					}
				}
				for (Integer n: toRemove) pools.remove(n);
				if (pools.isEmpty())
				{
					//throw new JPPFError("FATAL ERROR: No more driver connection available for this client");
					log.warn("No more driver connection available for this client");
				}
				if (oneAttempt) break;
			}
		}
		if (debugEnabled && (connection != null)) log.debug("found client connection \"" + connection + "\"");
		return connection;
	}

	/**
	 * Initialize this client's security credentials.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initCredentials() throws Exception
	{
	}

	/**
	 * Submit a JPPFJob for execution.
	 * @param job the job to execute.
	 * @return the results of the tasks' execution, as a list of <code>JPPFTask</code> instances for a blocking job, or null if the job is non-blocking.
	 * @throws Exception if an error occurs while sending the job for execution.
	 */
	public abstract List<JPPFTask> submit(JPPFJob job) throws Exception;

	/**
	 * Invoked when the status of a client connection has changed.
	 * @param event the event to notify of.
	 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
	 */
	public void statusChanged(ClientConnectionStatusEvent event)
	{
		JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
		if (c.getStatus().equals(JPPFClientConnectionStatus.FAILED)) connectionFailed(c);
	}

	/**
	 * Invoked when the status of a connection has changed to <code>JPPFClientConnectionStatus.FAILED</code>.
	 * @param c the connection that failed.
	 */
	protected void connectionFailed(JPPFClientConnection c)
	{
		log.info("Connection [" + c.getName() + "] failed");
		c.removeClientConnectionStatusListener(this);
		int priority = c.getPriority();
		ClientPool pool = pools.get(priority);
		boolean emptyPools = false;
		if (pool != null)
		{
			pool.clientList.remove(c);
			if (pool.clientList.isEmpty()) pools.remove(priority);
			//if (pools.isEmpty()) throw new JPPFError("FATAL ERROR: No more driver connection available for this client");
			if (pools.isEmpty())
			{
				log.error("FATAL ERROR: No more driver connection available for this client");
				emptyPools = true;
			}
			synchronized(allConnections)
			{
				allConnections.remove(c);
			}
		}
		synchronized(listeners)
		{
			for (ClientListener listener: listeners)
			{
				listener.connectionFailed(new ClientEvent(c));
			}
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
	}

	/**
	 * Add a listener to the list of listeners to this client.
	 * @param listener the listener to add.
	 */
	public void addClientListener(ClientListener listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list of listeners to this client.
	 * @param listener the listener to remove.
	 */
	public void removeClientListener(ClientListener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}

	/**
	 * Notify all listeners that a new connection was created.
	 * @param c the connection that was created.
	 */
	public void newConnection(JPPFClientConnection c)
	{
		synchronized(listeners)
		{
			for (ClientListener listener: listeners)
			{
				listener.newConnection(new ClientEvent(c));
			}
		}
	}

	/**
	 * Get the unique universal identifier for this JPPF client.
	 * @return the uuid as a string.
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * This comparator defines a decending value order for integers.
	 */
	static class DescendingIntegerComparator implements Comparator<Integer>, Serializable
	{
		/**
		 * Explicit serialVersionUID.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Compare two integers. This comparator defines a descending order for integers.
		 * @param o1 first integer to compare.
		 * @param o2 second integer to compare.
		 * @return -1 if o1 > o2, 0 if o1 == o2, 1 if o1 < o2
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Integer o1, Integer o2)
		{
			return o2 - o1; 
		}
	}
}
