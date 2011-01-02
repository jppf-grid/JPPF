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

import java.util.List;

import javax.resource.spi.work.Work;

import org.jppf.JPPFException;
import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.client.AbstractClassServerDelegate;
import org.jppf.comm.socket.SocketInitializer;
import org.slf4j.*;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents (or bytecode) to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. They enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 */
public class JcaClassServerDelegate extends AbstractClassServerDelegate implements Work
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JcaClassServerDelegate.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The JPPF client that owns this class server delegate.
	 */
	private JPPFJcaClient client = null;

	/**
	 * Initialize class server delegate with a spceified application uuid.
	 * @param name the name given to this this delegate.
	 * @param uuid the unique identifier for the local JPPF client.
	 * @param host the name or IP address of the host the class server is running on.
	 * @param port the TCP port the class server is listening to.
	 * @param client the JPPF client that owns this class server delegate.
	 * @throws Exception if the connection could not be opended.
	 */
	public JcaClassServerDelegate(String name, String uuid, String host, int port, JPPFJcaClient client) throws Exception
	{
		super(null);
		this.appUuid = uuid;
		this.host = host;
		this.port = port;
		this.client = client;
		setName(name);
		socketInitializer.setName("[" + getName() + " - delegate] ");
	}

	/**
	 * Initialize this delegate's resources.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.client.ClassServerDelegate#init()
	 */
	public final void init() throws Exception
	{
		try
		{
			setStatus(CONNECTING);
			if (socketClient == null) initSocketClient();
			if (debugEnabled) log.debug("[client: "+getName()+"] Attempting connection to the class server");
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isClosed())
			{
				if (socketInitializer.isSuccessfull())
				{
					log.info("[client: "+getName()+"] Reconnected to the class server");
					setStatus(ACTIVE);
				}
				else
				{
					throw new JPPFException("["+getName()+"] Could not reconnect to the class server");
				}
			}
			else
			{
				setStatus(FAILED);
				close();
			}
		}
		catch(Exception e)
		{
			if (!closed) setStatus(DISCONNECTED);
			throw e;
		}
	}

	/**
	 * Main processing loop of this delegate.
	 * @see org.jppf.client.ClassServerDelegate#run()
	 */
	public void run()
	{
		try
		{
			while (!stop)
			{
				try
				{
					if (getStatus().equals(DISCONNECTED)) performConnection();
					if (getStatus().equals(ACTIVE))
					{
						boolean found = true;
						JPPFResourceWrapper resource = readResource();
						String name = resource.getName();
						if  (debugEnabled) log.debug("["+this.getName()+"] resource requested: " + name);

						String requestUuid = resource.getRequestUuid();
						ClassLoader cl = getClassLoader(requestUuid);
						if (debugEnabled) log.debug("attempting resource lookup using classloader=" + cl + " for request uuid = " + requestUuid);
						if (resource.getData("multiple") == null)
						{
							byte[] b = null;
							byte[] callable = resource.getCallable();
							if (callable != null) b = resourceProvider.computeCallable(callable);
							else
							{
								if (resource.isAsResource()) b = resourceProvider.getResource(name, cl);
								else b = resourceProvider.getResourceAsBytes(name, cl);
							}
							if (b == null) found = false;
							if (callable == null) resource.setDefinition(b);
							else resource.setCallable(b);
							if (debugEnabled)
							{
								if (found) log.debug("["+this.getName()+"] sent resource: " + name + " (" + b.length + " bytes)");
								else log.debug("["+this.getName()+"] resource not found: " + name);
							}
						}
						else
						{
							List<byte[]> list = resourceProvider.getMultipleResourcesAsBytes(name, cl);
							if (list != null) resource.setData("resource_list", list);
						}
						resource.setState(JPPFResourceWrapper.State.PROVIDER_RESPONSE);
						writeResource(resource);
					}
					else
					{
						Thread.sleep(100);
					}
				}
				catch(Exception e)
				{
					if (!closed)
					{
						if (debugEnabled) log.debug("["+getName()+"] caught " + e + ", will re-initialise ...", e);
						setStatus(DISCONNECTED);
						//init();
					}
				}
			}
		}
		catch (Exception e)
		{
			log.error("["+getName()+"] "+e.getMessage(), e);
			close();
		}
	}

	/**
	 * Establish a connection and perform the inital shakedown with the JPPF driver.
	 * @throws Exception if the conenction could not be established.
	 */
	public void performConnection() throws Exception
	{
		try
		{
			init();
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.PROVIDER_INITIATION);
			resource.addUuid(appUuid);
			writeResource(resource);
			// receive the initial response from the server.
			readResource();
		}
		finally
		{
			if (getStatus().equals(DISCONNECTED))
			{
				Thread.sleep(100);
			}
		}
	}

	/**
	 * Close the socket connection.
	 * @see org.jppf.client.ClassServerDelegate#close()
	 */
	public void close()
	{
		if (!closed)
		{
			closed = true;
			stop = true;

			try
			{
				socketInitializer.close();
				socketClient.close();
			}
			catch (Exception e)
			{
				log.error("["+getName()+"] "+e.getMessage(), e);
			}
		}
	}

	/**
	 * Create a socket initializer for this delegate.
	 * @return a <code>SocketInitializer</code> instance.
	 */
	protected SocketInitializer createSocketInitializer()
	{
		return new JcaSocketInitializer();
	}

	/**
	 * This method does nothing.
	 * @see javax.resource.spi.work.Work#release()
	 */
	public void release()
	{
	}

	/**
	 * Retrieve the class laoder to use form the submission manager.
	 * @param uuid the uuid of the request from which the class loader was obtained.
	 * @return a <code>ClassLoader</code> instance, or null if none could be found.
	 */
	private ClassLoader getClassLoader(String uuid)
	{
		return client.getRequestClassLoader(uuid);
	}
}
