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

package org.jppf.comm.recovery;

import java.util.EventObject;

/**
 * Event emitted when a remote peer detects the connection with the server is broken.
 * @author Laurent Cohen
 */
public class ClientConnectionEvent extends EventObject
{
	/**
	 * Initialize this event with the specified client-side connection.
	 * @param connection the connection to which the event applies.
	 */
	public ClientConnectionEvent(final ClientConnection connection)
	{
		super(connection);
	}

	/**
	 * Get the connection from which the event originated.
	 * @return a {@link ClientConnection} instance.
	 */
	public ClientConnection getConnection()
	{
		return (ClientConnection) getSource();
	}
}
