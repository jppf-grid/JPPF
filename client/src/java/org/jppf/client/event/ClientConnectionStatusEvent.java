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

package org.jppf.client.event;

import java.util.EventObject;

/**
 * Event sent to notify of a status chnage for a client connection.
 * @author Laurent Cohen
 */
public class ClientConnectionStatusEvent extends EventObject
{
	/**
	 * Initialize this event with a client connection as source.
	 * @param source the event source.
	 */
	public ClientConnectionStatusEvent(ClientConnectionStatusHandler source)
	{
		super(source);
	}

	/**
	 * Get the source of this event.
	 * @return the event as a <code>JPPFClientConnection</code> instance.
	 */
	public ClientConnectionStatusHandler getClientConnectionStatusHandler()
	{
		return (ClientConnectionStatusHandler) getSource();
	}
}
