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

package org.jppf.server.node.remote;

import org.jppf.comm.socket.*;

/**
 * An IOHandler implementation that delegates I/O to a {@link SocketWrapper}.
 * @author Laurent Cohen
 */
public class RemoteIOHandler extends BootstrapSocketIOHandler
{
	/**
	 * Initialize this handler with the specified socket wrapper.
	 * @param socketWrapper the socket wrapper that handles the I/O.
	 */
	public RemoteIOHandler(SocketWrapper socketWrapper)
	{
		super(socketWrapper);
	}
}
