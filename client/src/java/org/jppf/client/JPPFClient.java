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
package org.jppf.client;

import java.util.Map;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.BundleParameter;
import org.jppf.utils.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient extends AbstractJPPFClient
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFClient.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The pool of threads used for submitting execution requests.
	 */
	private ExecutorService executor = null;

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFClient()
	{
		super();
		initPools();
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public JPPFClient(String uuid)
	{
		super(uuid);
		initPools();
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	public void initPools()
	{
		System.out.println("in initPool()");
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
				String name = "".equals(s) ? "default" : s;
				JPPFClientConnection c = new JPPFClientConnectionImpl(uuid, name, props);
				int priority = c.getPriority();
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
	 * Send a request to get the statistics collected by the JPPF server.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if an error occurred while trying to get the server statistics.
	 */
	public JPPFStats requestStatistics() throws Exception
	{
		JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) getClientConnection(); 
		return conn.requestStatistics();
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
	public String submitAdminRequest(String password, String newPassword, BundleParameter command, Map<BundleParameter, Object> parameters)
			throws Exception
	{
		JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) getClientConnection(); 
		return conn.submitAdminRequest(password, newPassword, command, parameters);
	}

	/**
	 * Close this client and release all the resources it is using.
	 */
	public void close()
	{
		super.close();
		if (executor != null) executor.shutdownNow();
	}

	/**
	 * Wrapper class for the initialization of a client connection.
	 */
	private static class ConnectionInitializer implements Runnable
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
}
