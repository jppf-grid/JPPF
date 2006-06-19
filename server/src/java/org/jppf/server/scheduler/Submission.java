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
package org.jppf.server.scheduler;

import java.util.List;

import org.jppf.server.event.TaskCompletionListener;
import org.jppf.server.scheduler.bundle.Bundler;

/**
 * This class represents a submission to JPPF Driver. Each submission has a
 * bunch of independent tasks, with certain priority.
 * 
 * @author Domingos Creado
 * 
 */
public final class Submission {

	/**
	 * The tasks to be executed by the node.
	 */
	List<byte[]> tasks = null;

	/**
	 * The priority of this task bundle.
	 */
	int priority = 0;

	/**
	 * The task completion listener to notify, once the execution of this task
	 * has completed.
	 */
	TaskCompletionListener completionListener = null;

	/**
	 * The unique identifier for this task bundle.
	 */
	String uuid = null;

	/**
	 * The unique identifier for the request this task is a part of.
	 */
	String requestUuid = null;

	/**
	 * The unique identifier for the submitting application.
	 */
	String appUuid = null;

	/**
	 * The shared data provider for this task bundle.
	 */
	byte[] dataProvider = null;
	
	/**
	 * the algorithm that dynamically computes the task bundle size
	 */
	Bundler bundler;

	/**
	 * COMMENT.
	 * @param uuid COMMENT.
	 * @param requestUuid COMMENT.
	 * @param appUuid COMMENT.
	 * @param dataProvider COMMENT.
	 * @param priority COMMENT.
	 * @param completionListener COMMENT.
	 * @param tasks COMMENT.
	 * @param bundler COMMENT.
	 */
	public Submission(String uuid, String requestUuid, String appUuid,
			byte[] dataProvider, int priority,
			TaskCompletionListener completionListener, List<byte[]> tasks, Bundler bundler) {
		super();
		this.tasks = tasks;
		this.priority = priority;
		this.completionListener = completionListener;
		this.uuid = uuid;
		this.requestUuid = requestUuid;
		this.appUuid = appUuid;
		this.dataProvider = dataProvider;
		this.bundler = bundler;
	}

	/**
	 * Get the priority of this task bundle.
	 * 
	 * @return the priority as an int.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Set the priority of this task bundle.
	 * 
	 * @param priority
	 *            the priority as an int.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}


}
