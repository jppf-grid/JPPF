/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

package org.jppf.client;

import java.util.List;

import org.jppf.client.event.TaskResultListener;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;

/**
 * 
 * @author Laurent Cohen
 */
public class ClientExecution
{
	/**
	 * List of tasks for this execution.
	 */
	public List<JPPFTask> tasks = null;
	/**
	 * Data provider for this execution.
	 */
	public DataProvider dataProvider = null;
	/**
	 * Flag to determine whether the execution is blocking or not.
	 */
	public boolean isBlocking = true;
	/**
	 * Listener to notify whenever a set of results have been received.
	 */
	public TaskResultListener listener = null;

	/**
	 * Initialize this execution with the specified parameters.
	 * @param tasks list of tasks for this execution.
	 * @param dataProvider data provider for this execution.
	 * @param isBlocking flag to determine whether the execution is blocking or not.
	 * @param listener listener to notify whenever a set of results have been received.
	 */
	public ClientExecution(List<JPPFTask> tasks, DataProvider dataProvider, boolean isBlocking, TaskResultListener listener)
	{
		this.tasks = tasks;
		this.dataProvider = dataProvider;
		this.isBlocking = isBlocking;
		this.listener = listener;
	}
}
