/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
	 * Initialize this task wrapper with a specified JPPF task.
	 * @param node the JPPF node that runs this task.
	 * @param task the task to execute within a try/catch block.
	 * @param uuidPath the key to the JPPFContainer for the task's classloader.
	 */
	public NodeTaskWrapper(JPPFNode node, JPPFTask task, List<String> uuidPath)
	{
		this.node = node;
		this.task = task;
		this.uuidPath = uuidPath;
	}

	/**
	 * Execute the task within a try/catch block.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		if (node.isNotifying()) node.incrementExecutingCount();
		try
		{
			Thread.currentThread().setContextClassLoader(node.getContainer(uuidPath).getClassLoader());
			task.run();
		}
		catch(Throwable t)
		{
			if (t instanceof Exception) task.setException((Exception) t);
			else task.setException(new JPPFException(t));
		}
		if (node.isNotifying()) node.decrementExecutingCount();
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