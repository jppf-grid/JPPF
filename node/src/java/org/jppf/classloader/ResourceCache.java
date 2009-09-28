/*
 * Java Parallel Processing Framework.
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

package org.jppf.classloader;

import java.io.*;
import java.security.*;
import java.util.*;

import org.apache.commons.logging.*;

/**
 * Instances of this class are used as cache for resources downloaded from a driver or client, using the JPPF class loader APIs.
 * @author Laurent Cohen
 */
public class ResourceCache
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ResourceCache.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Map of resource names to temporary file names to which their content is stored.
	 */
	private Map<String, List<String>> cache = new Hashtable<String, List<String>>();

	/**
	 * Get the list of locations for the resource with the specified name.
	 * @param name - the name of the resource to lookup.
	 * @return a list of file paths, or null if the resource is not found in the cache.
	 */
	public List<String> getResourcesLocations(String name)
	{
		return cache.get(name);
	}

	/**
	 * Get a location for the resource with the specified name.
	 * @param name - the name of the resource to lookup.
	 * @return a file path, or null if the resource is not found in the cache.
	 */
	public String getResourceLocation(String name)
	{
		List<String> locations = cache.get(name);
		if ((locations == null) || locations.isEmpty()) return null;
		return locations.get(0);
	}

	/**
	 * Set the list of locations for the resource with the specified name.
	 * @param name - the name of the resource to lookup.
	 * @param locations - a list of file paths.
	 */
	public void setResourcesLocations(String name, List<String> locations)
	{
		cache.put(name, locations);
	}

	/**
	 * Set the location for the resource with the specified name.
	 * @param name - the name of the resource to lookup.
	 * @param location - a file path.
	 */
	public void setResourceLocation(String name, String location)
	{
		List<String> list = new ArrayList<String>();
		list.add(location);
		cache.put(name, list);
	}

	/**
	 * Save the definitions for a resource to temporary files, and register their location with this cache.
	 * @param name - the name of the resource to register.
	 * @param definitions - a list of byte array definitions.
	 * @throws Exception if any I/O error occurs.
	 */
	public void registerResources(String name, List<byte[]> definitions) throws Exception
	{
		List<String> locations = new ArrayList<String>();
		for (byte[] def: definitions) locations.add(saveToTempFile(def));
		setResourcesLocations(name, locations);
	}

	/**
	 * Get the content of all resources with the specified name as arrays of bytes. 
	 * @param name - the name of the resources to lookup.
	 * @return a list of byte arrays, or null if no resource with the specified name could be found.
	 * @throws Exception if any I/O error occurs.
	 */
	public List<byte[]> getResourcesAsBytes(String name) throws Exception
	{
		return null;
	}

	/**
	 * Save the specified reosurce definition to a temporary file.
	 * @param definition - the definition to save, specified as a byte array.
	 * @return the path to the created file.
	 * @throws Exception if any I/O error occurs.
	 */
	private String saveToTempFile(final byte[] definition) throws Exception
	{
		SaveFileAction action = new SaveFileAction(definition);
		File file = (File) AccessController.doPrivileged(action);
		if (action.getException() != null) throw action.getException();
		return file.getCanonicalPath();
	}
}
