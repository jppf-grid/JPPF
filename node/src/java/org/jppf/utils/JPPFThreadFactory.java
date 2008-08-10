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

package org.jppf.utils;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom thread factory used mostly to specifiy th enames of created threads. 
 * @author Laurent Cohen
 */
public class JPPFThreadFactory implements ThreadFactory
{
	/**
	 * The name used as prefix for the constructed threads name.
	 */
	private String name = null;
	/**
	 * Count of created threads.
	 */
	private AtomicInteger count = new AtomicInteger(0);
	/**
	 * Determines whether the threads created by this factory can be monitored.
	 */
	private boolean monitoringEnabled = false;
	/**
	 * List of monitored thread IDs.
	 */
	private List<Long> threadIDs = null;

	/**
	 * Initialize this thread factory with the specified name.
	 * @param name the name used as prefix for the constructed threads name.
	 */
	public JPPFThreadFactory(String name)
	{
		this.name = name == null ? "JPPFThreadFactory" : name;
	}

	/**
	 * Initialize this thread factory with the specified name.
	 * @param name the name used as prefix for the constructed threads name.
	 * @param monitoringEnabled determines whether the threads created by this factory can be monitored.
	 */
	public JPPFThreadFactory(String name, boolean monitoringEnabled)
	{
		this(name);
		this.monitoringEnabled = monitoringEnabled;
		if (monitoringEnabled) threadIDs = new ArrayList<Long>();
	}

	/**
	 * Constructs a new Thread.
	 * @param r a runnable to be executed by the new thread instance.
	 * @return the constructed thread.
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	public Thread newThread(Runnable r)
	{
		Thread thread = new Thread(r, name + "-thread-" + incrementCount());
		if (monitoringEnabled) threadIDs.add(thread.getId());
		return thread;
	}

	/**
	 * Get the ids of the monitored threads.
	 * @return a list of long values.
	 */
	public List<Long> getThreadIDs()
	{
		if (!monitoringEnabled) return null;
		return Collections.unmodifiableList(threadIDs);
	}

	/**
	 * Increment and return the vreated thread count.
	 * @return the created thread count.
	 */
	private int incrementCount()
	{
		return count.incrementAndGet();
	}

	/**
	 * 
	 */
	private class JPPFThread extends Thread
	{
		/**
		 * Initialize this thread with the specified target and name. 
		 * @param target this thread's runnable target.
		 * @param name the name of this thread.
		 */
		public JPPFThread(Runnable target, String name)
		{
			super(target, name);
		}
	}
}
