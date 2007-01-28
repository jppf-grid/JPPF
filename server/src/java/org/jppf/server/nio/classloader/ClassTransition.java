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
package org.jppf.server.nio.classloader;

/**
 * Enumeration of the possible state transitions for a class server channel.
 * @author Laurent Cohen
 */
public enum ClassTransition
{
	/**
	 * Transition to the DEFINING_TYPE state.
	 */
	TO_DEFINING_TYPE,
	/**
	 * Transition to the DEFINING_TYPE state.
	 */
	TO_SENDING_INITIAL_RESPONSE,
	/**
	 * Transition to the WAITING_NODE_REQUEST state.
	 */
	TO_WAITING_NODE_REQUEST,
	/**
	 * Transition to the TO_SENDING_NODE_RESPONSE state.
	 */
	TO_SENDING_NODE_RESPONSE,
	/**
	 * Transition to the TO_SENDING_NODE_RESPONSE state.
	 */
	TO_SENDING_PROVIDER_REQUEST,
	/**
	 * Transition to the WAITING_PROVIDER_RESPONSE state.
	 */
	TO_WAITING_PROVIDER_RESPONSE,
	/**
	 * Transition to the TO_SENDING_NODE_RESPONSE state in idle mode.
	 */
	TO_IDLE_NODE,
	/**
	 * Transition to the SENDING_PROVIDER_REQUEST state in idle mode.
	 */
	TO_IDLE_PROVIDER;
}