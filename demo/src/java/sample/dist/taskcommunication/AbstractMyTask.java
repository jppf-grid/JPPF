/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package sample.dist.taskcommunication;

import java.util.*;

import org.jppf.node.NodeRunner;
import org.jppf.server.protocol.JPPFTask;

import com.hazelcast.core.Hazelcast;

/**
 * Common abstract super class for the tasks in the sample. It provides a simple API to initialize and access a distributed Map.
 * @author Laurent Cohen
 */
public abstract class AbstractMyTask extends JPPFTask
{
	/**
	 * Initialize this task.
	 * @param id - the task id.
	 */
	public AbstractMyTask(String id)
	{
		setId(id);
	}

	/**
	 * Wait for the specified time.
	 * @param time - the time to wait in milliseconds.  
	 */
	protected void doWait(long time)
	{
		try
		{
			Thread.sleep(time);
		}
		catch(InterruptedException e)
		{
			setException(e);
		}
	}

	/**
	 * Get the distributed map, and lazily initialize it if required.
	 * @return an <code>IMap</code> instance.
	 */
	protected Map<String, String> getMap()
	{
		String key = "taskMap";
		Map<String, String> map = (Map<String, String>) NodeRunner.getPersistentData(key);
		if (map == null)
		{
			map = Hazelcast.getMap(key);
			NodeRunner.setPersistentData(key, map);
		}
		return map;
	}

	/**
	 * Check that the distributed queue is empty.
	 * This method lazily initializes the queue if required.
	 * @return true if the distributed queue is empty, false otherwise.
	 */
	public boolean checkQueue()
	{
		String key = "MyDistyributedQueue";
		Queue<Object> queue = (Queue<Object>) NodeRunner.getPersistentData(key);
		if (queue == null)
		{
			queue = Hazelcast.getQueue(key);
			NodeRunner.setPersistentData(key, queue);
		}
		return queue.isEmpty();
	}
}
