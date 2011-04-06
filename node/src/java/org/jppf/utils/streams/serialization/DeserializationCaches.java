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

package org.jppf.utils.streams.serialization;

import java.util.*;

import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class DeserializationCaches
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(DeserializationCaches.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Mapping of handles to corresponding class descriptors. 
	 */
	Map<Integer, ClassDescriptor> handleToDescriptorMap = new HashMap<Integer, ClassDescriptor>();
	/**
	 * Mapping of handles to corresponding objects. 
	 */
	Map<Integer, Object> handleToObjectMap = new HashMap<Integer, Object>();
	/**
	 * Holds the set of missing object references in the object graph.
	 */
	Map<Integer, List<PendingReference>> pendingRefMap = new HashMap<Integer, List<PendingReference>>();

	/**
	 * Default constructor.
	 */
	DeserializationCaches()
	{
	}

	/**
	 * Get the class dezscriptor assocateed with the specified handfe. 
	 * @param handle the handle to lookup.
	 * @return a {@link ClassDescriptor} instance.
	 */
	ClassDescriptor getDescriptor(int handle)
	{
		return handleToDescriptorMap.get(handle);
	}

	/**
	 * Add a pending reference.
	 * @param handle the handle of the reference to set.
	 * @param ref the reference to add.
	 */
	void addPendingReference(int handle, PendingReference ref)
	{
		if (debugEnabled) log.debug("adding pending reference for handle=" + handle + " : " + ref);
		List<PendingReference> list = pendingRefMap.get(handle);
		if (list == null)
		{
			list = new ArrayList<PendingReference>();
			pendingRefMap.put(handle, list);
		}
		list.add(ref);
	}
}
