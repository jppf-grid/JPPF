/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.utils;

/**
 * Instances of this class represent a TCP host:port combination.
 * @author Laurent Cohen
 */
public class HostPort extends Pair<String, Integer>
{
	/**
	 * Initialize this HostPort with the specified host name and port number.
	 * @param host the host to use.
	 * @param port the port number.
	 */
	public HostPort(String host, Integer port)
	{
		super(host, port);
	}

	/**
	 * Get the host name.
	 * @return the host as a string.
	 */
	public String host()
	{
		return first();
	}

	/**
	 * Get the port number.
	 * @return the port as an int.
	 */
	public int port()
	{
		return second();
	}

	/**
	 * Get a string representation of this object.
	 * @return a string formatted as host:port.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "" + host() + ":" + port();
	}
}
