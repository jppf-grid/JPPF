/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
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

package org.jppf.management;

import java.io.Serializable;


/**
 * Instances of this class encapsulate the information required to access
 * the JMX server of a node.
 * @author Laurent Cohen
 */
public class NodeManagementInfo implements Serializable, Comparable<NodeManagementInfo>
{
	/**
	 * The host on which the node is running.
	 */
	private String host = null;
	/**
	 * The port on which the node's JMX server is listening.
	 */
	private int port = 11198;
	/**
	 * Unique id for the node's mbean server. 
	 */
	private String id = null;
	/**
	 * The system information associated with the node at the time of the initial connection.
	 */
	private transient JPPFSystemInformation systemInfo = null;

	/**
	 * Initialize this information with the specified parameters.
	 * @param host the host on which the node is running.
	 * @param port he port on which the node's JMX server is listening.
	 * @param id unique id for the node's mbean server.
	 */
	public NodeManagementInfo(String host, int port, String id)
	{
		this.host = host;
		this.port = port;
		this.id = id;
	}

	/**
	 * Get the host on which the node is running.
	 * @return the host as a string.
	 */
	public synchronized String getHost()
	{
		return host;
	}

	/**
	 * Get the port on which the node's JMX server is listening.
	 * @return the port as an int.
	 */
	public synchronized int getPort()
	{
		return port;
	}

	/**
	 * Get the hashcode for this instance.
	 * @return the hashcode as an int.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * Compare this object with another for equality.
	 * @param obj the object to compare to.
	 * @return true if the two objects are equal, false otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final NodeManagementInfo other = (NodeManagementInfo) obj;
		if (host == null)
		{
			if (other.host != null) return false;
		}
		else if (!host.equals(other.host)) return false;
		if (id == null)
		{
			if (other.id != null) return false;
		}
		else if (!id.equals(other.id)) return false;
		if (port != other.port) return false;
		return true;
	}

	/**
	 * Compare this object with an other.
	 * @param o the other object to compare to.
	 * @return a negative number if this object is less than the other, 0 if they are equal, a positive number otherwise.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(NodeManagementInfo o)
	{
		if (o == null) return 1;
		if (this.equals(o)) return 0;
		// we want ascending alphabetical order
		int n = -1 * host.compareTo(o.getHost());
		if (n != 0) return n;
		return port - o.getPort();
	}

	/**
	 * Get a string representation of this node information.
	 * @return a string with the host:port format.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return host + ":" + port;
	}

	/**
	 * Get the system information associated with the node at the time of the initial connection.
	 * @return a <code>JPPFSystemInformation</code> instance.
	 */
	public synchronized JPPFSystemInformation getSystemInfo()
	{
		return systemInfo;
	}

	/**
	 * Set the system information associated with the node at the time of the initial connection.
	 * @param systemInfo a <code>JPPFSystemInformation</code> instance.
	 */
	public synchronized void setSystemInfo(JPPFSystemInformation systemInfo)
	{
		this.systemInfo = systemInfo;
	}
}
