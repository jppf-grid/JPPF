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

package org.jppf.server.protocol.commandline;

import java.io.Serializable;

/**
 * Instances of this class represent the location of an artifact, generally a file or the data found at a url.
 * @author Laurent Cohen
 */
public class Location implements Serializable
{
	/**
	 * This type of location indicates that the path is a file path.
	 */
	public static final int FILE = 0;
	/**
	 * This type of location indicates that the path is a url path.
	 */
	public static final int URL = 1;
	/**
	 * The type of this location.
	 */
	private int type = 0;
	/**
	 * The path for this location.
	 */
	private String path = null;

	/**
	 * Initialize this location with the specified type and path.
	 * @param type the type of this location.
	 * @param path the path for this location.
	 */
	public Location(int type, String path)
	{
		this.type = type;
		this.path = path;
	}

	/**
	 * Get the type of this location.
	 * @return  the type as an int value, either FILE or URL.
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Get the path for this location.
	 * @return the path as a string.
	 */
	public String getPath()
	{
		return path;
	}
}
