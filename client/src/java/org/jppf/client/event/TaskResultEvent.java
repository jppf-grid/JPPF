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
package org.jppf.client.event;

import java.util.*;

import org.jppf.server.protocol.JPPFTask;

/**
 * Event object used to notify interested listeners that a list of task results
 * have been received from the server.
 * @author Laurent Cohen
 */
public class TaskResultEvent extends EventObject
{
	/**
	 * Index of the first task in the list, relative to the initial execution request.
	 */
	private int startIndex = -1;

	/**
	 * Initialize this event with a specified list of tasks and start index.
	 * @param taskList the list of tasks whose results have been received from the server.
	 * @param startIndex index of the first task in the list, relative to the initial execution
	 * request. Used to enable proper ordering of the results.
	 */
	public TaskResultEvent(List<JPPFTask> taskList, int startIndex)
	{
		super(taskList);
		this.startIndex = startIndex;
	}

	/**
	 * Get the list of tasks whose results have been received from the server.
	 * @return a list of <code>JPPFTask</code> instances.
	 */
	@SuppressWarnings("unchecked")
	public List<JPPFTask> getTaskList()
	{
		return (List<JPPFTask>) getSource();
	}

	/**
	 * Get the index of the first task in the list, relative to the initial execution request.
	 * @return the index as an int value.
	 */
	public int getStartIndex()
	{
		return startIndex;
	}
}
