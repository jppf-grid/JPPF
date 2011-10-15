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

package org.jppf.node;

import java.util.List;

import org.jppf.node.protocol.*;

/**
 * Instances of this interface manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 */
public interface NodeExecutionManager
{
	/**
	 * Get the job currently being executed.
	 * @return a {@link JPPFDistributedJob} instance, or null if no job is being executed.
	 */
	JPPFDistributedJob getCurrentJob();

	/**
	 * Get the list of tasks currently being executed.
	 * @return a list of {@link JPPFTask} instances, or null if the node is idle.
	 */
	List<Task> getTasks();

	/**
	 * Get the id of the job currently being executed.
	 * @return the job id as a string, or null if no job is being executed.
	 */
	String getCurrentJobId();
}
