/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.node;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.node.event.*;

/**
 * Interface for a node that provides information about its activity.
 * @author Laurent Cohen
 */
public interface MonitoredNode extends Runnable
{
	/**
	 * Get the underlying socket used by this node.
	 * @return a SocketWrapper instance.
	 */
	SocketWrapper getSocketWrapper();
	/**
	 * Set the underlying socket to be used by this node.
	 * @param socketWrapper a SocketWrapper instance.
	 */
	void setSocketWrapper(SocketWrapper socketWrapper);
	/**
	 * Add a listener to the list of listener for this node.
	 * @param listener the listener to add.
	 */
	void addNodeListener(NodeListener listener);
	/**
	 * Remove a listener from the list of listener for this node.
	 * @param listener the listener to remove.
	 */
	void removeNodeListener(NodeListener listener);
	/**
	 * Notify all listeners that an event has occurred.
	 * @param eventType the type of the event as an enumerated value.
	 */
	void fireNodeEvent(NodeEventType eventType);
	/**
	 * Create an event for the execution of a specified number of tasks.
	 * @param nbTasks the number of tasks as an int.
	 */
	void fireNodeEvent(int nbTasks);
	/**
	 * Stop this node and release the resources it is using.
	 * @param closeSocket determines whether the underlying socket should be closed.
	 */
	void stopNode(boolean closeSocket);
}
