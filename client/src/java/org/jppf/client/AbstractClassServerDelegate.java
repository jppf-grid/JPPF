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

import org.jppf.classloader.ResourceProvider;
import org.jppf.comm.socket.SocketClient;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractClassServerDelegate extends AbstractClientConnectionHandler implements ClassServerDelegate
{
	/**
	 * Indicates whether this socket handler should be terminated and stop processing.
	 */
	protected boolean stop = false;
	/**
	 * Indicates whether this socket handler is closed, which means it can't handle requests anymore.
	 */
	protected boolean closed = false;
	/**
	 * Reads resource files from the classpath.
	 */
	protected ResourceProvider resourceProvider = new ResourceProvider();
	/**
	 * Unique identifier for this class server delegate, obtained from the local JPPF client.
	 */
	protected String appUuid = null;

	/**
	 * Default instantiation of this class is not permitted.
	 * @param owner the client connection which owns this connection delegate.
	 */
	protected AbstractClassServerDelegate(JPPFClientConnection owner)
	{
		super(owner);
	}

	/**
	 * Determine whether the socket connection is closed
	 * @return true if the socket connection is closed, false otherwise
	 * @see org.jppf.client.ClassServerDelegate#isClosed()
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Get the name of this delegate.
	 * @return the name as a string.
	 * @see org.jppf.client.ClassServerDelegate#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this delegate.
	 * @param name the name as a string.
	 * @see org.jppf.client.ClassServerDelegate#setName(java.lang.String)
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Initialize this delegate's resources.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.client.ClassServerDelegate#initSocketClient()
	 */
	public void initSocketClient() throws Exception
	{
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}
}
