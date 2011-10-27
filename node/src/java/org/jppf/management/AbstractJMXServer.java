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

package org.jppf.management;

import java.util.concurrent.locks.*;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;

import org.slf4j.*;

/**
 * This class is a wrapper around a JMX management server.
 * It is used essentially to hide the details of the remote management protocol used.
 * @author Laurent Cohen
 */
public abstract class AbstractJMXServer implements JMXServer
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AbstractJMXServer.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Used to synchronize lookup for available port.
	 */
	protected static Lock lock = new ReentrantLock();
	/**
	 * The mbean server.
	 */
	protected MBeanServer server = null;
	/**
	 * The JMX connector server.
	 */
	protected JMXConnectorServer connectorServer = null;
	/**
	 * Determines whether this JMX server is stopped.
	 */
	protected boolean stopped = true;
	/**
	 * This server's unique id.
	 */
	protected String id;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract void start(ClassLoader cl) throws Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() throws Exception
	{
		stopped = true;
		connectorServer.stop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MBeanServer getServer()
	{
		return server;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isStopped()
	{
		return stopped;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return id;
	}
}
