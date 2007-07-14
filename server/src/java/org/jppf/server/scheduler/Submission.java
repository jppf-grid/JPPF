/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
package org.jppf.server.scheduler;

import java.util.List;

import org.jppf.server.protocol.TaskCompletionListener;
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
