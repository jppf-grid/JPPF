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

package org.jppf.client;

import java.util.*;

/**
 * Instances of this class manage a list of client connections with the same priority.
 */
class ClientPool
{
	/**
	 * The priority associated with this pool.
	 */
	private int priority = 0;
	/**
	 * Index of the last used connection in this pool.
	 */
	private int lastUsedIndex = 0;
	/**
	 * List of <code>JPPFClientConnection</code> instances with the same priority.
	 */
	public List<JPPFClientConnection> clientList = new ArrayList<JPPFClientConnection>();

	/**
	 * Get the next client connection.
	 * @return a <code>JPPFClientConnection</code> instances.
	 */
	public JPPFClientConnection nextClient()
	{
		if (clientList.isEmpty()) return null;
		lastUsedIndex = ++lastUsedIndex % clientList.size();
		return clientList.get(getLastUsedIndex());
	}

	/**
	 * Get the current size of this pool.
	 * @return the size as an int.
	 */
	public int size()
	{
		return clientList.size();
	}

	/**
	 * Get the priority associated with this pool.
	 * @return the priority as an int.
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Set the priority associated with this pool.
	 * @param priority the priority as an int.
	 */
	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	/**
	 * Get the index of the last used connection in this pool.
	 * @return the last used index as an int.
	 */
	public int getLastUsedIndex()
	{
		return lastUsedIndex;
	}

	/**
	 * Set the index of the last used connection in this pool.
	 * @param lastUsedIndex the last used index as an int.
	 */
	public void setLastUsedIndex(int lastUsedIndex)
	{
		this.lastUsedIndex = lastUsedIndex;
	}
}