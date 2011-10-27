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

package org.jppf.server.node;

import java.security.*;
import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.classloader.*;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * Instances of this class manage the node's class loader and associated operations.
 * @author Laurent Cohen
 */
public abstract class AbstractClassLoaderManager
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AbstractClassLoaderManager.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Maximum number of containers kept by this node's cache.
	 */
	private static int maxContainers = JPPFConfiguration.getProperties().getInt("jppf.classloader.cache.size", 50);
	/**
	 * Class loader used for dynamic loading and updating of client classes.
	 */
	protected AbstractJPPFClassLoader classLoader = null;
	/**
	 * Mapping of containers to their corresponding application uuid.
	 */
	protected Map<String, JPPFContainer> containerMap = new HashMap<String, JPPFContainer>();
	/**
	 * A list retaining the container in chronological order of their creation.
	 */
	protected LinkedList<JPPFContainer> containerList = new LinkedList<JPPFContainer>();
	/**
	 * The callback used to create the class loader in each {@link JPPFContainer}.
	 */
	private Callable<JPPFClassLoader> classLoaderCreator = null;

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	public AbstractJPPFClassLoader getClassLoader()
	{
		if (classLoader == null) classLoader = createClassLoader();
		return classLoader;
	}

	/**
	 * Create the class loader for this node.
	 * @return a {@link JPPFClassLoader} instance.
	 */
	protected abstract AbstractJPPFClassLoader createClassLoader();

	/**
	 * Set the main classloader for the node.
	 * @param cl the class loader to set.
	 */
	public void setClassLoader(final JPPFClassLoader cl)
	{
		classLoader = cl;
	}

	/**
	 * Get a reference to the JPPF container associated with an application uuid.
	 * @param uuidPath the uuid path containing the key to the container.
	 * @return a <code>JPPFContainer</code> instance.
	 * @throws Exception if an error occcurs while getting the container.
	 */
	public JPPFContainer getContainer(final List<String> uuidPath) throws Exception
	{
		String uuid = uuidPath.get(0);
		JPPFContainer container = null;
		synchronized(this)
		{
			container = containerMap.get(uuid);
			if (container == null)
			{
				if (debugEnabled) log.debug("Creating new container for appuuid=" + uuid);
				AbstractJPPFClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<AbstractJPPFClassLoader>()
						{
					@Override
					public AbstractJPPFClassLoader run()
					{
						try
						{
							return newClassLoaderCreator(uuidPath).call();
						}
						catch(Exception e)
						{
							log.error(e.getMessage(), e);
						}
						return null;
					}
						});
				container = newJPPFContainer(uuidPath, cl);
				if (containerList.size() >= maxContainers)
				{
					JPPFContainer toRemove = containerList.removeFirst();
					containerMap.remove(toRemove.getAppUuid());
				}
				containerList.add(container);
				containerMap.put(uuid, container);
			}
		}
		return container;
	}

	/**
	 * Create a new container based on the uuid path and class loader.
	 * @param uuidPath uuid path for the corresponding client.
	 * @param cl the class loader to use.
	 * @return a {@link JPPFContainer} instance.
	 * @throws Exception if any error occurs
	 */
	protected abstract JPPFContainer newJPPFContainer(List<String> uuidPath, AbstractJPPFClassLoader cl) throws Exception;

	/**
	 * Instantiate the callback used to create the class loader in each {@link JPPFContainer}.
	 * @param uuidPath the uuid path containing the key to the container.
	 * @return a {@link Callable} instance.
	 */
	protected abstract Callable<AbstractJPPFClassLoader> newClassLoaderCreator(List<String> uuidPath);

	/**
	 * Get a mapping of containers to their corresponding application uuid.
	 * @return a mapping of <code>String</code> keys to <code>JPPFContainer</code> values.
	 */
	public Map<String, JPPFContainer> getContainerMap()
	{
		return containerMap;
	}

	/**
	 * Get the list retaining the container in chronological order of their creation.
	 * @return a list of <code>JPPFContainer</code> instances.
	 */
	public LinkedList<JPPFContainer> getContainerList()
	{
		return containerList;
	}
}
