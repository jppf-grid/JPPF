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

package org.jppf.jca.work;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.*;

import org.jppf.JPPFError;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.SocketInitializer;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFJcaClientConnection extends AbstractJPPFClientConnection
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFJcaClientConnection.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param info the connection properties for this connection.
	 * @param client the JPPF client that owns this connection.
	 */
	public JPPFJcaClientConnection(String uuid, String name, JPPFConnectionInformation info, JPPFJcaClient client)
	{
		configure(uuid, name, info.host, info.serverPorts[0], info.serverPorts[0], 0);
		status.set(DISCONNECTED);
		classServerPort = info.serverPorts[0];
		this.client = client;
	}

	/**
	 * Initialize this connection
	 * @see org.jppf.client.JPPFClientConnection#init()
	 */
	@Override
	public void init()
	{
		try
		{
			delegate = new JcaClassServerDelegate(name, uuid, host, classServerPort, (JPPFJcaClient) client);
			delegate.addClientConnectionStatusListener(new ClientConnectionStatusListener()
			{
				public void statusChanged(ClientConnectionStatusEvent event)
				{
					delegateStatusChanged(event);
				}
			});
			taskServerConnection.addClientConnectionStatusListener(new ClientConnectionStatusListener()
			{
				@Override
				public void statusChanged(ClientConnectionStatusEvent event)
				{
					taskServerConnectionStatusChanged(event);
				}
			});
			((JcaClassServerDelegate) delegate).performConnection();
			if (!delegate.isClosed())
			{
				Thread t = new Thread(delegate);
				t.setName("[" + delegate.getName() + " : class delegate]");
				t.start();
				taskServerConnection.init();
			}
		}
		catch(Exception e)
		{
			log.debug(e.getMessage());
		}
		catch(JPPFError e)
		{
			throw e;
		}
	}

	/**
	 * Send tasks to the server for execution.
	 * @param cl classloader used for serialization.
	 * @param header the task bundle to send to the driver.
	 * @param job the job to execute remotely.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void sendTasks(ClassLoader cl, JPPFTaskBundle header, JPPFJob job) throws Exception
	{
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		try
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(cl);
			sendTasks(header, job);
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			throw e;
		}
		catch(Error e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			throw e;
		}
		finally
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(oldCl);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendTasks(JPPFTaskBundle header, JPPFJob job) throws Exception
	{
		header.setRequestUuid(job.getJobUuid());
		if (debugEnabled) log.debug("sending tasks bundle with requestUuid=" + header.getRequestUuid());
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		ClassLoader cl = (!job.getTasks().isEmpty()) ? job.getTasks().get(0).getClass().getClassLoader() : null;
		try
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(cl);
			super.sendTasks(header, job);
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			throw e;
		}
		catch(Error e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			throw e;
		}
		finally
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(oldCl);
		}
	}

	/**
	 * Submit a JPPFJob for execution.
	 * @param job the job to execute.
	 * @throws Exception if an error occurs while sending the job for execution.
	 * @see org.jppf.client.JPPFClientConnection#submit(org.jppf.client.JPPFJob)
	 */
	@Override
	public void submit(JPPFJob job) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the name of the serialization helper implementation class name to use.
	 * @return the fully qualified class name of a <code>SerializationHelper</code> implementation.
	 * @see org.jppf.client.AbstractJPPFClientConnection#getSerializationHelperClassName()
	 */
	@Override
	protected String getSerializationHelperClassName()
	{
		return "org.jppf.jca.serialization.JcaSerializationHelperImpl";
	}


	/**
	 * Shutdown this client and retrieve all pending executions for resubmission.
	 * @return a list of <code>JPPFJob</code> instances to resubmit.
	 * @see org.jppf.client.JPPFClientConnection#close()
	 */
	@Override
	public List<JPPFJob> close()
	{
		if (!isShutdown)
		{
			isShutdown = true;
			try
			{
				if (taskServerConnection != null) taskServerConnection.close();
				if (delegate != null) delegate.close();
			}
			catch(Exception e)
			{
				log.error('[' + name + "] "+ e.getMessage(), e);
			}
			List<JPPFJob> result = new ArrayList<JPPFJob>();
			if (job != null) result.add(job);
			return result;
		}
		return null;
	}

	/**
	 * Get the name assigned tothis client connection.
	 * @return the name as a string.
	 * @see org.jppf.client.JPPFClientConnection#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * Get a string representation of this client connection.
	 * @return a string representing this connection.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return name + " : " + status;
	}

	/**
	 * Create a socket initializer.
	 * @return an instance of <code>SocketInitializerImpl</code>.
	 * @see org.jppf.client.AbstractJPPFClientConnection#createSocketInitializer()
	 */
	@Override
	protected SocketInitializer createSocketInitializer()
	{
		return new JcaSocketInitializer();
	}

	/**
	 * Get the JPPF client that manages connections to the JPPF drivers.
	 * @return a <code>JPPFJcaClient</code> instance.
	 */
	@Override
	public JPPFJcaClient getClient()
	{
		return (JPPFJcaClient) client;
	}
}
