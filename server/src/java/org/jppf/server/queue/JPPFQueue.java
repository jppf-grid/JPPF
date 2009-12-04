/*
 * JPPF.
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

package org.jppf.server.queue;

import org.jppf.io.BundleWrapper;

/**
 * Implementation of a generic non-blocking queue, to allow asynchronous access from a large number of threads.
 * @author Laurent Cohen
 */
public interface JPPFQueue extends Iterable<BundleWrapper>
{
	/**
	 * Add an object to the queue, and notify all listeners about it.
	 * @param bundleWrapper the object to add to the queue.
	 */
	void addBundle(BundleWrapper bundleWrapper);

	/**
	 * Get the next object in the queue.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 */
	BundleWrapper nextBundle(int nbTasks);

	/**
	 * Get the next object in the queue.
	 * @param bundleWrapper the bundle to either remove or extract a sub-bundle from.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 */
	BundleWrapper nextBundle(BundleWrapper bundleWrapper, int nbTasks);

	/**
	 * Add a listener to the current list of listeners to this queue.
	 * @param listener the listener to add.
	 */
	void addListener(QueueListener listener);

	/**
	 * Remove a listener from the current list of listeners to this queue.
	 * @param listener the listener to remove.
	 */
	void removeListener(QueueListener listener);

	/**
	 * Determine whether the queue is empty or not.
	 * @return true if the queue is empty, false otherwise.
	 */
	boolean isEmpty();

	/**
	 * Get the maximum bundle size for the bundles present in the queue.
	 * @return the bundle size as an int.
	 */
	int getMaxBundleSize();
}
