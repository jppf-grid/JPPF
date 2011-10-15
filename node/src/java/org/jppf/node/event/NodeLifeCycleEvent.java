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

package org.jppf.node.event;

import java.util.*;

import org.jppf.node.NodeExecutionManager;
import org.jppf.node.protocol.*;

/**
 * Instances of this class represent node life cycle events
 * @author Laurent Cohen
 */
public class NodeLifeCycleEvent extends EventObject
{
	/**
	 * Initialize this event with the specified execution manager.
	 * @param executionManager the execution that handles the execution of tasks by a node.
	 */
	public NodeLifeCycleEvent(NodeExecutionManager executionManager)
	{
		super(executionManager);
	}

	/**
	 * Get the job currently being executed.
	 * @return a {@link JPPFDistributedJob} instance, or null if no job is being executed.
	 */
	public JPPFDistributedJob getJob()
	{
		return ((NodeExecutionManager) getSource()).getCurrentJob();
	}

	/**
	 * Get the tasks currently being executed.
	 * @return a list of {@link JPPFTask} instances, or null if the node is idle.
	 */
	public List<Task> getTasks()
	{
		return ((NodeExecutionManager) getSource()).getTasks();
	}
}
