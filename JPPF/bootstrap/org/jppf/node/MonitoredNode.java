/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.node;

import org.jppf.node.event.NodeListener;

/**
 * Interface for a node that provides information about its activity.
 * @author Laurent Cohen
 */
public interface MonitoredNode extends Runnable
{
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
	 * @param eventType the type of the event as a string.
	 */
	void fireNodeEvent(String eventType);
	/**
	 * Stop this node and release the resources it is using.
	 */
	void stopNode();
}
