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
 * Enumeration of the possible state transitions for a Node server channel.
 * @author Laurent Cohen
 */
public enum NodeTransition
{
	/**
	 * Transition from a state to SENDING_BUNDLE.
	 */
	TO_SENDING, 
	/**
	 * Transition from a state to WAITING_RESULTS.
	 */
	TO_WAITING,
	/**
	 * Transition from a state to SEND_INITIAL_BUNDLE.
	 */
	TO_SEND_INITIAL,
	/**
	 * Transition from a state to WAIT_INITIAL_BUNDLE.
	 */
	TO_WAIT_INITIAL,
	/**
	 * Transition from a state to SENDING_BUNDLE in idle mode.
	 */
	TO_IDLE;
}
