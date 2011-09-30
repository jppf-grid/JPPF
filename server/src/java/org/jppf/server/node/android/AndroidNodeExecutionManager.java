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

package org.jppf.server.node.android;

import java.util.List;

import org.jppf.server.node.*;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 */
public class AndroidNodeExecutionManager extends NodeExecutionManagerImpl
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AndroidNodeExecutionManager.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The node that uses this excecution manager.
	 */
	private AbstractJPPFAndroidNode node = null;

	/**
	 * Initialize this execution manager with the specified node.
	 * @param node the node that uses this excecution manager.
	 */
	public AndroidNodeExecutionManager(AbstractJPPFAndroidNode node)
	{
		super(null);
		this.node = node;
	}

	/**
	 * Execute the specified tasks of the specified tasks bundle.
	 * @param bundle the bundle to which the tasks are associated.
	 * @param taskList the list of tasks to execute.
	 * @throws Exception if the execution failed.
	 */
	@Override
    public void execute(JPPFTaskBundle bundle, List<JPPFTask> taskList) throws Exception
	{
		if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
		NodeExecutionInfo info = null;
		if (isCpuTimeEnabled()) info = computeExecutionInfo();
		setup(bundle, taskList);
		for (JPPFTask task : taskList) performTask(task);
		waitForResults();
		cleanup();
	}
}
