/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Set of characters used to compose a uuid, including more than alphanumeric characters.
	 */
	public static final String[] ALPHABET_SUPERSET =
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
		"m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H",
		"I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "'", "!", "@", "#",
		"$", "%", "^", "&", "*", "(", ")", "_", "+", "|", "{", "}", "[", "]", "-", "=", "/", ",", ".", "?", ":", ";"
	};
	/**
	 * Set of characters used to compose a uuid, including only alphanumeric characters.
	 */
	public static final String[] ALPHA_NUM =
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
		"m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H",
		"I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
	};
	/**
	 * Set of characters used to compose a uuid, including only hexadecimal digits.
	 */
	public static final String[] HEXADECIMAL =
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"
	};
	/**
	 * Set of characters used to compose a uuid, including only decimal digits.
	 */
	public static final String[] DECIMAL =
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	/**
	 * Random number generator, static to ensure generated uuid are unique.
	 */
	private static Random rand = new Random(System.nanoTime());
	/**
	 * String holding a generated unique identifier.
	 */
	private String uuid = null;
	/**
	 * The set of codes from which to choose randomly to build the uuid.
	 */
	private String[] codes = ALPHABET_SUPERSET;
	/**
	 * Number of codes to use to build the uuid.
	 */
	private int length = 16;

	/**
	 * Instanciate this JPPFUuid with a generated unique identifier.
	 */
	public JPPFUuid()
	{
		this(ALPHABET_SUPERSET, 16);
	}

	/**
	 * Instanciate this JPPFUuid with a generated unique identifier.
	 * @param codes the set of codes from which to choose randomly to build the uuid.
	 * @param length number of codes to use to build the uuid.
	 */
	public JPPFUuid(final String[] codes, final int length)
	{
		if ((codes != null) && (codes.length > 0)) this.codes = codes;
		if (length > 0) this.length = length;
		uuid = generateUuid();
	}

	/**
	 * Generate a unique uuid.
	 * @return the uuid as a string.
	 */
	private String generateUuid()
	{
		int len = codes.length;
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<length; i++) sb.append(codes[rand.nextInt(len)]);
		return sb.toString();
	}

	/**
	 * Get a string representation of the generated unique identifier.
	 * @return a string containing the uuid.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return uuid;
	}
}
