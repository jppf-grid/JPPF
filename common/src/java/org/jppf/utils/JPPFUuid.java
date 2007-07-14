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
package org.jppf.utils;

import java.io.Serializable;
import java.util.Random;

/**
 * Instances of this class serve as unique identifiers for messages sent to and from
 * remote execution services.
 * The identifier is generated as a string with the following elements:
 * <ul>
 * <li>sender host IP address</li>
 * <li>current system time in milliseconds</li>
 * <li>a random integer value between 0 and {@link java.lang.Integer#MAX_VALUE Integer.MAX_VALUE}</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFUuid implements Serializable
{
	/**
	 * Set of characters used to compose a uuid.
	 */
	private static final String[] ALPHABET =
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
	  "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H",
	  "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "'", "!", "@", "#",
	  "$", "%", "^", "&", "*", "(", ")", "_", "+", "|", "{", "}", "[", "]", "-", "=", "/", ",", ".", "?", ":", ";"
	};
  /**
	 * Random number generator, static to ensure generated uuid are unique.
	 */
	private static Random rand = new Random(System.currentTimeMillis());
	/**
	 * The IP address of the host the JVM is running on.
	 */
	private static String ipAddress = obtainIpAddress();
	/**
	 * String holding a generated unique identifier.
	 */
	private String uuid = null;

	/**
	 * Instanciate this JPPFUuid with a generated unique identifier.
	 */
	public JPPFUuid()
	{
		uuid = generateUuid();
	}

	/**
	 * Generate a unique uuid.
	 * @return the uuid as a string.
	 */
	private static String generateUuid()
	{
		int len = ALPHABET.length;
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<16; i++) sb.append(ALPHABET[rand.nextInt(len)]);
		return sb.toString();
	}
	
	/**
	 * Generate a unique uuid.
	 * @return the uuid as a string.
	 */
	private static String generateUuid2()
	{
		int n = 2;
		StringBuilder sb = new StringBuilder();
		sb.append(ipAddress);
		sb.append(StringUtils.padLeft("" + System.currentTimeMillis(), '0', 15));
		synchronized(rand)
		{
			for (int i=0; i<n; i++)
			{
				sb.append(StringUtils.padLeft("" + rand.nextInt(Integer.MAX_VALUE), '0', 10));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Get the IP address of the current host.<br>
	 * The address is formatted as <i>aaabbbcccddd</i>, where <i>aaa</i> is the first component of the address,
	 * formatted on 3 characters and padded with zeroes on the left if required, <i>bbb</i> is the second component, etc...
	 * @return the IP address as a string.
	 */
	private static String obtainIpAddress()
	{
		String ip = VersionUtils.getLocalIpAddress();
		if (ip == null) return null;
		String[] tokens = ip.split("\\.");
		StringBuilder sb = new StringBuilder();
		for (String token: tokens)
		{
			sb.append(StringUtils.padLeft(token, '0', 3));
		}
		return sb.toString();
	}

	/**
	 * Get a string representaiton of the generated unique identifier.
	 * @return a string containing the uuid.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return uuid;
	}
}
