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
package org.jppf.server.protocol;

import java.io.Serializable;
import org.jppf.utils.JPPFUuid;

/**
 * Header for a request to the framework.<br>
 * A request is an array of bytes made of the following elements:
 * <ul>
 * <li>an int value indicating the length of this header in serialized format</li>
 * <li>this header, including a task count</li>
 * <li>for each task, an int value indicating the length of the serialized task</li>
 * <li>following each task length and the task in serialized format</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFRequestHeader implements Serializable
{
	/**
	 * Value for an execution request type.
	 */
	public static final String EXECUTION = "execution";
	/**
	 * Value for a non-blocking execution request type.
	 */
	public static final String NON_BLOCKING_EXECUTION = "nb.execution";
	/**
	 * Value for an administration request type.
	 */
	public static final String ADMIN = "admin";
	/**
	 * Value for a statistics collection type of request.
	 */
	public static final String STATISTICS = "statistics";
	/**
	 * The unique identifier for the submitting application.
	 */
	private String appUuid = null;
	/**
	 * The unique identifier for this request.
	 */
	private String uuid = null;
	/**
	 * The type of this request, ie either {@link #EXECUTION EXECUTION} or {@link #ADMIN ADMIN}.
	 */
	private String requestType = EXECUTION;
	/**
	 * The number of tasks in this request.
	 */
	private int taskCount = 0;

	/**
	 * Initialize this request header.
	 */
	public JPPFRequestHeader()
	{
		uuid = new JPPFUuid().toString();
	}

	/**
	 * Get the unique identifier for this request.
	 * @return the request uuid as a string.
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * Get the number of tasks in this request.
	 * @return the number of tasks as an int.
	 */
	public int getTaskCount()
	{
		return taskCount;
	}

	/**
	 * Set the number of tasks in this request.
	 * @param taskCount the number of tasks as an int.
	 */
	public void setTaskCount(int taskCount)
	{
		this.taskCount = taskCount;
	}

	/**
	 * Get the unique identifier for the submitting application.
	 * @return the application uuid as a string.
	 */
	public String getAppUuid()
	{
		return appUuid;
	}

	/**
	 * Set the unique identifier for the submitting application.
	 * @param appUuid the application uuid as a string.
	 */
	public void setAppUuid(String appUuid)
	{
		this.appUuid = appUuid;
	}

	/**
	 * Get the type of this request, ie either {@link #EXECUTION EXECUTION}, {@link #STATISTICS STATISTICS} or {@link #ADMIN ADMIN}.
	 * @return the type as a string.
	 */
	public String getRequestType()
	{
		return requestType;
	}

	/**
	 * Set the type of this request.
	 * @param requestType the type as a string, either {@link #EXECUTION EXECUTION}, {@link #STATISTICS STATISTICS} or {@link #ADMIN ADMIN}.
	 */
	public void setRequestType(String requestType)
	{
		this.requestType = requestType;
	}
}
