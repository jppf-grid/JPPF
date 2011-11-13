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

import java.util.*;

/**
 * Generic abstract super class for class that wish to emit events.
 * @param <S> the type of event listeners handled by this event emitter.
 * @author Laurent Cohen
 */
public abstract class EventEmitter<S extends EventListener>
{
	/**
	 * The list of registered listeners.
	 */
	protected List<S> eventListeners = new ArrayList<S>();

	/**
	 * Add a listener to the list of listeners.
	 * @param listener the listener to add to the list.
	 */
	public void addListener(final S listener)
	{
		synchronized(eventListeners)
		{
			eventListeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list of listeners.
	 * @param listener the listener to remove from the list.
	 */
	public void removeListener(final S listener)
	{
		synchronized(eventListeners)
		{
			eventListeners.remove(listener);
		}
	}

	/**
	 * return a list of all the registered listeners.
	 * This list is not thread safe and must be manually synchronized against concurrent modifications.
	 * @return a list of listener instances.
	 */
	public List<S> getListeners()
	{
		return eventListeners;
	}
}
