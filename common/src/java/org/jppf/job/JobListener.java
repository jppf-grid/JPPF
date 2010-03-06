/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.job;

import java.util.EventListener;


/**
 * Listener interface for job manager events.
 * @author Laurent Cohen
 */
public interface JobListener extends EventListener
{
	/**
	 * Called when a new job is put in the job queue.
	 * @param event - encapsulates the information about the event.
	 */
	void jobQueued(JobNotification event);
	/**
	 * Called when a job is complete and has been sent back to the client.
	 * @param event - encapsulates the information about the event.
	 */
	void jobEnded(JobNotification event);
	/**
	 * Called when the current number of tasks in a job was updated.
	 * @param event - encapsulates the information about the event.
	 */
	void jobUpdated(JobNotification event);
	/**
	 * Called when all or part of a job is is sent to a node for execution.
	 * @param event - encapsulates the information about the event.
	 */
	void jobDispatched(JobNotification event);
	/**
	 * Called when all or part of a job has returned from irs execution on a node.
	 * @param event - encapsulates the information about the event.
	 */
	void jobReturned(JobNotification event);
}
