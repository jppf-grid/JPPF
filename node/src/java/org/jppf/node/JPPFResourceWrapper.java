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

import java.io.Serializable;

/**
 * Instances of this class encapsulate the necessary information used by the network classloader,
 * for sending class definition requests as well as receiving the class definitions. 
 * @author Laurent Cohen
 */
public class JPPFResourceWrapper implements Serializable
{
	/**
	 * Enumeration of the possible states for this resource wrapper.
	 */
	public enum State
	{
		/**
		 * State for a node first contacting the class server.
		 */
		NODE_INITIATION,
		/**
		 * State for a node requesting a resource from the class server.
		 */
		NODE_REQUEST,
		/**
		 * State for a node receiving a resource from the class server.
		 */
		NODE_RESPONSE,
		/**
		 * State for a resource provider first contacting the class server.
		 */
		PROVIDER_INITIATION,
		/**
		 * State for the class server requesting a resource from a resource provider.
		 */
		PROVIDER_REQUEST,
		/**
		 * State for the class server receiving a resource from a resource provider.
		 */
		PROVIDER_RESPONSE
	}

	/**
	 * The unique identifier of the application in whose classpath the class definition should be found. 
	 */
	private String appUuid = null;
	/**
	 * The name of the class whose definition is requested.
	 */
	private String name = null;
	/**
	 * The actual definition of the requested class.
	 */
	private byte[] definition = null;
	/**
	 * Determines whether the class should be loaded through the network classloader.
	 */
	private boolean dynamic = false;
	/**
	 * The state associated with this resource wrapper.
	 */
	private State state = null;

	/**
	 * Get the unique identifier of the application in whose classpath the class definition should be found. 
	 * @return the identifier as a string.
	 */
	public String getAppUuid()
	{
		return appUuid;
	}

	/**
	 * Set the unique identifier of the application in whose classpath the class definition should be found. 
	 * @param appUuid the identifier as a string.
	 */
	public void setAppUuid(String appUuid)
	{
		this.appUuid = appUuid;
	}

	/**
	 * Get the name of the class whose definition is requested.
	 * @return the class name as a string.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of the class whose definition is requested.
	 * @param name the class name as a string.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Get the actual definition of the requested class.
	 * @return the class definition as an array of bytes.
	 */
	public byte[] getDefinition()
	{
		return definition;
	}

	/**
	 * Set the actual definition of the requested class.
	 * @param definition the class definition as an array of bytes.
	 */
	public void setDefinition(byte[] definition)
	{
		this.definition = definition;
	}

	/**
	 * Determine whether the class should be loaded through the network classloader.
	 * @return true if the class should be loaded via the network classloader, false otherwise.
	 */
	public boolean isDynamic()
	{
		return dynamic;
	}

	/**
	 * Set whether the class should be loaded through the network classloader.
	 * @param dynamic true if the class should be loaded via the network classloader, false otherwise.
	 */
	public void setDynamic(boolean dynamic)
	{
		this.dynamic = dynamic;
	}

	/**
	 * Get the state associated with this resource wrapper.
	 * @return a <code>State</code> typesafe enumerated value.
	 */
	public State getState()
	{
		return state;
	}

	/**
	 * Set the state associated with this resource wrapper.
	 * @param state a <code>State</code> typesafe enumerated value.
	 */
	public void setState(State state)
	{
		this.state = state;
	}
}
