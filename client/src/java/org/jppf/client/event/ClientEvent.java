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

import org.jppf.client.JPPFClientConnection;

/**
 * Instances of this class are events sent to notify interested listeners
 * that a new connection to a JPPF driver was created.
 * @author Laurent Cohen
 */
public class ClientEvent extends EventObject
{
	/**
	 * Initialize this event with the specified client connection.
	 * @param c the client connection on which the event occurs.
	 */
	public ClientEvent(final JPPFClientConnection c)
	{
		super(c);
	}

	/**
	 * Get the client connection on which the event occured.
	 * @return a <code>JPPFClientConnection</code> instance.
	 */
	public JPPFClientConnection getConnection()
	{
		return (JPPFClientConnection) getSource();
	}
}
