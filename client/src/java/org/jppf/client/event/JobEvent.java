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

package org.jppf.client.event;

import java.util.EventObject;

import org.jppf.client.JPPFJob;

/**
 * Event emiitted by a job when its execution qstart or completes.
 * @author Laurent Cohen
 */
public class JobEvent extends EventObject
{
	/**
	 * The tyype of event.
	 */
	public enum Type
	{
		/**
		 * The job started.
		 */
		JOB_START,
		/**
		 * The job ended.
		 */
		JOB_END
	}

	/**
	 * Initialize this event with the specified job as its source.
	 * @param source the source of this event.
	 */
	public JobEvent(JPPFJob source)
	{
		super(source);
	}

	/**
	 * Get the source of this event.
	 * @return the source as a {@link JPPFJob} object.
	 */
	public JPPFJob getJob()
	{
		return (JPPFJob) getSource();
	}
}
