/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.node;

import java.io.Serializable;
import org.jppf.utils.TraversalList;

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
	 * Keeps and manages the uuid path list and the current position in it.
	 */
	private TraversalList<String> uuidPath = new TraversalList<String>();
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
	 * The uuid sent by a node when it first contacts a resource provider.
	 */
	private String providerUuid = null;
	/**
	 * Determines whether the resource is to be loaded using <code>ClassLoader.getResource()</code>.
	 */
	private boolean asResource = false;

	/**
	 * Add a uuid to the uuid path of this resource wrapper. 
	 * @param uuid the identifier as a string.
	 */
	public void addUuid(String uuid)
	{
		uuidPath.add(uuid);
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

	/**
	 * Get a reference to the traversal list that Keeps and manages the uuid path list
	 * and the current position in it.
	 * @return a traversal list of string elements.
	 */
	public TraversalList<String> getUuidPath()
	{
		return uuidPath;
	}

	/**
	 * Set the reference to the traversal list that Keeps and manages the uuid path list
	 * and the current position in it.
	 * @param uuidPath a traversal list of string elements.
	 */
	public void setUuidPath(TraversalList<String> uuidPath)
	{
		this.uuidPath = uuidPath;
	}

	/**
	 * Get the uuid sent by a node when it first contacts a resource provider.
	 * @return the uuid as a string.
	 */
	public String getProviderUuid()
	{
		return providerUuid;
	}

	/**
	 * Set the uuid sent by a node when it first contacts a resource provider.
	 * @param providerUuid the uuid as a string.
	 */
	public void setProviderUuid(String providerUuid)
	{
		this.providerUuid = providerUuid;
	}

	/**
	 * Determine whether the resource is to be loaded using <code>ClassLoader.getResource()</code>.
	 * @return true if the resource is loaded using getResource(), false otherwise.
	 */
	public boolean isAsResource()
	{
		return asResource;
	}

	/**
	 * Set whether the resource is to be loaded using <code>ClassLoader.getResource()</code>.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 */
	public void setAsResource(boolean asResource)
	{
		this.asResource = asResource;
	}
}
