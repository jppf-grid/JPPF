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
 * Enumeration of the possible states for a class server channel.
 * @author Laurent Cohen
 */
public enum ChannelState
{
	/**
	 * State of determining the type of a channel.
	 */
	DEFINING_TYPE,
	/**
	 * State of sending the initial information to a node classloader.
	 */
	SENDING_INITIAL_RESPONSE,
	/**
	 * State of waiting for a request from a node classloader.
	 */
	WAITING_NODE_REQUEST,
	/**
	 * State of waiting for a response to a node classloader.
	 */
	SENDING_NODE_RESPONSE,
	/**
	 * State of waiting for a response form a resource provider.
	 */
	WAITING_PROVIDER_RESPONSE,
	/**
	 * State of waiting for a response from a resource provider.
	 */
	SENDING_PROVIDER_REQUEST
}