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

package org.jppf.server.nio.message;

import java.util.*;

import org.jppf.server.protocol.JPPFTaskBundle;

/**
 * 
 * @author Laurent Cohen
 */
public class BundleWrapper
{
	/**
	 * The underlying task bundle.
	 */
	private JPPFTaskBundle bundle = null;
	/**
	 * The location of the data provider.
	 */
	private DataLocation dataProvider = null;
	/**
	 * The list of locations of the tasks.
	 */
	private List<DataLocation> tasks = new ArrayList<DataLocation>();

	/**
	 * Default constructor.
	 */
	public BundleWrapper()
	{
	}

	/**
	 * Initialize this bundle wrapper with the specified task bundle.
	 * @param bundle the underlying task bundle for this wrapper.
	 */
	public BundleWrapper(JPPFTaskBundle bundle)
	{
		this.bundle = bundle;
	}

	/**
	 * Get the underlying task bundle.
	 * @return a <code>JPPFTaskBundle</code> instance.
	 */
	public JPPFTaskBundle getBundle()
	{
		return bundle;
	}

	/**
	 * Set the underlying task bundle.
	 * @param bundle a <code>JPPFTaskBundle</code> instance.
	 */
	public void setBundle(JPPFTaskBundle bundle)
	{
		this.bundle = bundle;
	}

	/**
	 * Get the location of the data provider.
	 * @return a <code>JPPFTaskBundle</code> instance.
	 */
	public DataLocation getDataProvider()
	{
		return dataProvider;
	}

	/**
	 * Get the location of the data provider.
	 * @param dataProvider a <code>JPPFTaskBundle</code> instance.
	 */
	public void setDataProvider(DataLocation dataProvider)
	{
		this.dataProvider = dataProvider;
	}

	/**
	 * Add a task to this bundle wrapper.
	 * @param task the task to add.
	 */
	public void addTask(DataLocation task)
	{
		tasks.add(task);
	}

	/**
	 * Get the list of locations of the tasks.
	 * @return a list of <code>DataLocation</code> instances.
	 */
	public List<DataLocation> getTasks()
	{
		return tasks;
	}
}
