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

package org.jppf.comm.recovery;

import java.util.EventListener;

/**
 * This interface should be implemented buy objects that wish to be notified
 * of a broken connection with the server on the remote peer side.
 * @author Laurent Cohen
 */
public interface ClientConnectionListener extends EventListener
{
	/**
	 * Called when the remote peer detects its connection with the server is broken. 
	 * @param event an event wrapping the connection and its status.
	 */
	void clientConnectionFailed(ClientConnectionEvent event);
}
