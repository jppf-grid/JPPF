/*
 * Java Parallel Processing Framework.
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

package org.jppf.client;

import java.util.List;

import org.jppf.client.event.ClientConnectionStatusHandler;

/**
 * Interface for a client connection to a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFClientConnection extends ClientConnectionStatusHandler
{
	/**
	 * Initialize this client connection.
	 */
	void init();

	/**
	 * Submit the request to the server.
	 * @param job the job to execute remotely.
	 * @throws Exception if an error occurs while sending the request.
	 */
	void submit(JPPFJob job) throws Exception;

	/**
	 * Get the priority assigned to this connection.
	 * @return a priority as an int value.
	 */
	int getPriority();

	/**
	 * Shutdown this client and retrieve all pending executions for resubmission.
	 * @return a list of <code>JPPFJob</code> instances to resubmit.
	 */
	List<JPPFJob> close();

	/**
	 * Get the name assigned tothis client connection.
	 * @return the name as a string.
	 */
	String getName();
}
