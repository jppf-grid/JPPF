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

package org.jppf.server.node.local;

import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.classloader.*;
import org.jppf.server.nio.nodeserver.LocalNodeChannel;
import org.jppf.server.node.*;

/**
 * Local (in-VM) node implementation.
 * @author Laurent Cohen
 */
public class JPPFLocalNode extends JPPFNode
{
	/**
	 * The I/O handler for this node.
	 */
	private LocalNodeChannel channel = null;
	/**
	 * The I/O handler for the class loader.
	 */
	private LocalClassLoaderChannel classLoaderHandler = null;

	/**
	 * Initialize this local node with the specfied I/O handler.
	 * @param handler the I/O handler for this node.
	 * @param classLoaderHandler the I/O handler for the class loader.
	 */
	public JPPFLocalNode(LocalNodeChannel handler, LocalClassLoaderChannel classLoaderHandler)
	{
		this.channel = handler;
		this.classLoaderHandler = classLoaderHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void initDataChannel() throws Exception
	{
		nodeIO = new LocalNodeIO(this);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void closeDataChannel() throws Exception
	{
	}

	/**
	 * {@inheritDoc}
	 */
	protected AbstractJPPFClassLoader createClassLoader()
	{
		if (classLoader == null) classLoader = new JPPFLocalClassLoader(classLoaderHandler, this.getClass().getClassLoader());
		return classLoader;
	}

	/**
	 * @param uuidPath the uuid path containing the key to the container.
	 * Instatiate the callback used to create the class loader in each {@link JPPFLocalContainer}.
	 * @return a {@link Callable} instance.
	 */
	protected Callable<AbstractJPPFClassLoader> newClassLoaderCreator(final List<String> uuidPath)
	{
		return new Callable<AbstractJPPFClassLoader>()
		{
			public AbstractJPPFClassLoader call()
			{
				return new JPPFLocalClassLoader(getClassLoader(), uuidPath);
			}
		};
	}

	/**
	 * Get the I/O handler for this node.
	 * @return a {@link LocalNodeChannel} instance.
	 */
	public LocalNodeChannel getChannel()
	{
		return channel;
	}

	/**
	 * {@inheritDoc}
	 */
	protected JPPFContainer newJPPFContainer(List<String> uuidPath, AbstractJPPFClassLoader cl) throws Exception
	{
		return new JPPFLocalContainer(channel, uuidPath, cl);
	}
}
