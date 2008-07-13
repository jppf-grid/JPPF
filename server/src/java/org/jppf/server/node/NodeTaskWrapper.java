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
package org.jppf.server.node;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.server.protocol.JPPFTask;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 */
class NodeTaskWrapper implements Runnable
{
	/**
	 * The JPPF node that runs this task.
	 */
	private final JPPFNode node;
	/**
	 * The task to execute within a try/catch block.
	 */
	private JPPFTask task = null;
	/**
	 * The key to the JPPFContainer for the task's classloader.
	 */
	private List<String> uuidPath = null;
	/**
	 * The number identifying the task.
	 */
	private long number = 0L;

	/**
	 * Initialize this task wrapper with a specified JPPF task.
	 * @param node the JPPF node that runs this task.
	 * @param task the task to execute within a try/catch block.
	 * @param uuidPath the key to the JPPFContainer for the task's classloader.
	 * @param number the internal number identifying the task for the thread pool.
	 */
	public NodeTaskWrapper(JPPFNode node, JPPFTask task, List<String> uuidPath, long number)
	{
		this.node = node;
		this.task = task;
		this.uuidPath = uuidPath;
		this.number = number;
	}

	/**
	 * Execute the task within a try/catch block.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		JPPFNodeAdmin nodeAdmin = null;
		try
		{
			if (node.isNotifying()) node.incrementExecutingCount();
			if (node.isJmxEnabled())
			{
				nodeAdmin = node.getNodeAdmin();
				if (nodeAdmin != null)
				{
					nodeAdmin.taskStarted(task.getId());
					task.addJPPFTaskListener(nodeAdmin);
				}
			}
			Thread.currentThread().setContextClassLoader(node.getContainer(uuidPath).getClassLoader());
			task.run();
		}
		catch(Throwable t)
		{
			if (t instanceof Exception) task.setException((Exception) t);
			else task.setException(new JPPFException(t));
		}
		finally
		{
			if (nodeAdmin != null) nodeAdmin.taskEnded(task.getId());
			task.removeJPPFTaskListener(nodeAdmin);
			if (node.isNotifying()) node.decrementExecutingCount();
			node.getExecutionManager().taskEnded(number);
		}
	}

	/**
	 * Get the task this wrapper executes within a try/catch block.
	 * @return the task as a <code>JPPFTask</code> instance.
	 */
	public JPPFTask getTask()
	{
		return task;
	}
}
