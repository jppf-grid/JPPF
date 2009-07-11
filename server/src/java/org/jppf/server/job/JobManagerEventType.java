/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.job;

/**
 * This enum describes the types of events emitted by a JPPFJobManager.
 * @author Laurent Cohen
 */
public enum JobManagerEventType
{
	/**
	 * A new job was submmitted to the JPPF driver queue.
	 */
	JOB_QUEUED,
	/**
	 * A job was completed and sent back to the client.
	 */
	JOB_ENDED,
	/**
	 * A part of all of a job was dispatched to a node.
	 */
	JOB_DISPATCHED,
	/**
	 * A new job was sent back to the client.
	 */
	JOB_RETURNED
}
