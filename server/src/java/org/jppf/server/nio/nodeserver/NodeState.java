/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

package org.jppf.server.nio.nodeserver;


/**
 * Enumeration of the possible states for a class server channel.
 * @author Laurent Cohen
 */
public enum NodeState
{
	/**
	 * State of sending the initial bundle to the node.
	 */
	SEND_INITIAL_BUNDLE, 
	/**
	 * State of waiting for the initial bundle from the node.
	 */
	WAIT_INITIAL_BUNDLE, 
	/**
	 * State of waiting for something to do / sending a task bundle to the node.
	 */
	SENDING_BUNDLE, 
	/**
	 * State of waiting for the execution result of a task bundle.
	 */
	WAITING_RESULTS;
}
