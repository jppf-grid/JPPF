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

package org.jppf.server.protocol;

import java.util.*;

import org.jppf.io.DataLocation;

/**
 * This class wraps a task bundle to express it in terms of {@link org.jppf.io.DataLocation DataLocation}.
 * This allows the tasks data to be processed with the same semantics no matter where it is stored, comes from or goes to. 
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

	/**
	 * Set the list of locations of the tasks.
	 * @param tasks a list of <code>DataLocation</code> instances.
	 */
	public void setTasks(List<DataLocation> tasks)
	{
		this.tasks = tasks;
	}

	/**
	 * Make a copy of this bundle wrapper containing only the first nbTasks tasks it contains.
	 * @param nbTasks the number of tasks to include in the copy.
	 * @return a new <code>BundleWrapper</code> instance.
	 */
	public BundleWrapper copy(int nbTasks)
	{
		BundleWrapper wrapper = null;
		synchronized(this)
		{
			wrapper = new BundleWrapper(bundle.copy(nbTasks));
			for (int i=0; i<nbTasks; i++) wrapper.addTask(tasks.remove(0));
		}
		wrapper.setDataProvider(dataProvider.copy());
		return wrapper;
	}

	/**
	 * Merge this bundle wrapper with another.
	 * @param other the wrapper to merge with.
	 * @param after determines whether the tasks from other should be added first or last.
	 */
	public void merge(BundleWrapper other, boolean after)
	{
		int n = other.getBundle().getTaskCount();
		bundle.setTaskCount(bundle.getTaskCount() + n);
		bundle.getJobSLA().setSuspended(other.getBundle().getJobSLA().isSuspended());
		if (after)
		{
			for (DataLocation task: other.getTasks()) tasks.add(task);
		}
		else
		{
			for (int i=n-1; i>=0; i--) tasks.add(0, other.getTasks().get(i));
		}
	}
}
