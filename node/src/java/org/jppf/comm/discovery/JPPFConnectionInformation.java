/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.comm.discovery;

import java.io.*;

import org.jppf.utils.StringUtils;

/**
 * This class encapsulates the connection information for a JPPF driver.
 * The information includes the host, class server, application and node server ports. 
 * @author Laurent Cohen
 */
public class JPPFConnectionInformation implements Serializable, Comparable<JPPFConnectionInformation>
{
	/**
	 * The driver host name.
	 */
	public String host = null;
	/**
	 * The ports on which the class server is listening. 
	 */
	public int[] classServerPorts = null;
	/**
	 * The ports on which the node server is listening. 
	 */
	public int[] nodeServerPorts = null;
	/**
	 * The ports on which the application server is listening. 
	 */
	public int[] applicationServerPorts = null;
	/**
	 * Port number used for JMX management and monitoring.
	 */
	public int managementPort = -1;
	/**
	 * Host address used for JMX management and monitoring.
	 */
	public transient String managementHost = null;
	/**
	 * Identifier for this object.
	 */
	public String uuid = null;

	/**
	 * Default constructor.
	 */
	public JPPFConnectionInformation()
	{
	}

	/**
	 * Compare this connection information with another.
	 * @param ci the other object to compare to.
	 * @return -1 if this connection information is less than the other, 1 if it is greater, 0 if they are equal.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(JPPFConnectionInformation ci)
	{
		if ((ci == null) || (ci.uuid == null)) return 1;
		if (uuid == null) return -1;
		return uuid.compareTo(ci.uuid);
	}

	/**
	 * COmpute the hashcode of this object.
	 * @return the hashcode as an int.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return (uuid == null) ? 0 : uuid.hashCode();
	}

	/**
	 * Determine whether this object is equal to another.
	 * @param obj the object to compare to.
	 * @return true if the 2 objects are equal, false otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		JPPFConnectionInformation other = (JPPFConnectionInformation) obj;
		if (uuid == null) return other.uuid == null;
		return uuid.equals(other.uuid);
	}

	/**
	 * Get a string representation of this connection information object.
	 * @return a string describing this object.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("host = ").append(host);
		sb.append(", class server = ").append(StringUtils.buildString(classServerPorts));
		sb.append(", node server = ").append(StringUtils.buildString(nodeServerPorts));
		sb.append(", app server = ").append(StringUtils.buildString(applicationServerPorts));
		sb.append(", management = ").append(managementPort);
		sb.append(", uuid = ").append(uuid);
		return sb.toString();
	}

	/**
	 * Deserialize a DriverConnectionInformation object from an array of bytes.
	 * @param bytes the array of bytes to deserialize from.
	 * @return a <code>DriverConnectionInformation</code> instance.
	 * @throws Exception if an error is raised while deserializing.
	 */
	public static JPPFConnectionInformation fromBytes(byte[] bytes) throws Exception
	{
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
		JPPFConnectionInformation info = (JPPFConnectionInformation) ois.readObject();
		ois.close();
		return info;
	}

	/**
	 * Serialize a DriverConnectionInformation object to an array of bytes.
	 * @param info the <code>DriverConnectionInformation</code> object to serialize to.
	 * @return an array of bytes.
	 * @throws Exception if an error is raised while serializing.
	 */
	public static byte[] toBytes(JPPFConnectionInformation info) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(info);
		oos.close();
		return baos.toByteArray();
	}
}
