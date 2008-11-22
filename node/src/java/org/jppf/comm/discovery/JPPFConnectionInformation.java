/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.comm.discovery;

import java.io.*;

import org.jppf.utils.StringUtils;

/**
 * This class encapsulates the connection information for a JPPF driver.
 * The information includes the host, class server, application and node server ports. 
 * @author Laurent Cohen
 */
public class JPPFConnectionInformation implements Serializable
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
	 * Get a string representation of this connection information object.
	 * @return a string describing this object.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "host = " + host +
			", class server = " + StringUtils.buildString(classServerPorts) +
			", node server = " + StringUtils.buildString(nodeServerPorts) +
			", app server = " + StringUtils.buildString(applicationServerPorts);
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
