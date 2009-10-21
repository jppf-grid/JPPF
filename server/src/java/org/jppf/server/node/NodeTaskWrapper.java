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
package org.jppf.server.node;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.management.JPPFNodeAdmin;
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
	 * The execution manager.
	 */
	private NodeExecutionManager execManager = null;

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
		this.execManager = node.getExecutionManager();
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
		long cpuTime = 0L;
		long elapsedTime = 0L;
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
			long id = Thread.currentThread().getId();
			long startTime = System.currentTimeMillis();
			long startCpuTime = execManager.getCpuTime(id);
			task.run();
			// convert cpu time from nanoseconds to milliseconds
			cpuTime = (execManager.getCpuTime(id) - startCpuTime) / 1000000L;
			elapsedTime = System.currentTimeMillis() - startTime;
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
			execManager.taskEnded(number, cpuTime, elapsedTime, task.getException() != null);
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
