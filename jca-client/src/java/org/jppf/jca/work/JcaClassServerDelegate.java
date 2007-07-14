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
package org.jppf.jca.work;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.comm.socket.*;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.utils.CompressionUtils;

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
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(JcaClassServerDelegate.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Count of times this delegate has attempted to connect tothe JPPF server.
	 */
	private int connectionAttempts = 0;

	/**
	 * Default instantiation of this class is not permitted.
	 */
	private JcaClassServerDelegate()
	{
	}

	/**
	 * Initialize class server delegate with a spceified application uuid.
	 * @param name the name given to this this delegate.
	 * @param uuid the unique identifier for the local JPPF client.
	 * @param host the name or IP address of the host the class server is running on.
	 * @param port the TCP port the class server is listening to.
	 * @throws Exception if the connection could not be opended.
	 */
	public JcaClassServerDelegate(String name, String uuid, String host, int port) throws Exception
	{
		this.appUuid = uuid;
		this.host = host;
		this.port = port;
		setName(name);
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
			socketInitializer.setName("[" + getName() + " - delegate] ");
			//owner.setStatus(JPPFClientConnectionStatus.CONNECTING);
			if (socketClient == null) initSocketClient();
			System.out.println("[client: "+getName()+"] ClassServerDelegate.init(): Attempting connection to the class server");
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isSuccessfull() && !socketInitializer.isClosed())
			{
				throw new JPPFException("["+getName()+"] Could not reconnect to the class server");
			}
			if (!socketInitializer.isClosed())
			{
				System.out.println("[client: "+getName()+"] ClassServerDelegate.init(): Reconnected to the class server");
				//owner.setStatus(JPPFClientConnectionStatus.ACTIVE);
			}
		}
		catch(Exception e)
		{
			//owner.setStatus(JPPFClientConnectionStatus.FAILED);
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
			init();
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.PROVIDER_INITIATION);
			resource.addUuid(appUuid);
			socketClient.send(resource);
			while (!stop)
			{
				try
				{
					boolean found = true;
					resource = (JPPFResourceWrapper) socketClient.receive();
					String name = resource.getName();
					//if  (debugEnabled) log.debug("["+this.getName()+"] resource requested: " + name);
					byte[] b = null;
					if (resource.isAsResource()) b = resourceProvider.getResource(name);
					else b = resourceProvider.getResourceAsBytes(name);
					if (b == null) found = false;
					resource.setState(JPPFResourceWrapper.State.PROVIDER_RESPONSE);
					//resource.setDefinition(b);
					if (b != null) resource.setDefinition(CompressionUtils.zip(b, 0, b.length));
					else resource.setDefinition(null);
					socketClient.send(resource);
					if  (debugEnabled)
					{
						if (found) log.debug("["+this.getName()+"] sent resource: " + name + " (" + b.length + " bytes)");
						else log.debug("["+this.getName()+"] resource not found: " + name);
					}
				}
				catch(Exception e)
				{
					if (!closed)
					{
						//System.err.println("["+getName()+"] caught " + e + ", will re-initialise ...");
						//e.printStackTrace();
						init();
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
}
