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

package org.jppf.server.queue;

import java.util.EventObject;

import org.jppf.io.BundleWrapper;

/**
 * Instances of this class represent <code>JPPFQueue</code> events.
 * @author Laurent Cohen
 */
public class QueueEvent extends EventObject
{
	/**
	 * Represents part or the totality of a job that was submitted.
	 */
	private BundleWrapper bundleWrapper = null;
	/**
	 * Determines if the event is a requeued bundle, following a node failure for instance.
	 */
	private boolean requeue = false;

	/**
	 * Initialize this event with the specified queue and bundle.
	 * @param queue - the queue this event originates from. 
	 * @param bundleWrapper - represents part or the totality of a job that was submitted.
	 */
	public QueueEvent(JPPFQueue queue, BundleWrapper bundleWrapper)
	{
		this(queue, bundleWrapper, false);
	}

	/**
	 * Initialize this event with the specified queue and bundle.
	 * @param queue - the queue this event originates from. 
	 * @param bundleWrapper - represents part or the totality of a job that was submitted.
	 * @param requeue - determines if the event is a requeued bundle, following a node failure for instance.
	 */
	public QueueEvent(JPPFQueue queue, BundleWrapper bundleWrapper, boolean requeue)
	{
		super(queue);
		this.bundleWrapper = bundleWrapper;
		this.requeue = requeue;
	}

	/**
	 * Get the queue this event originates from.
	 * @return an instance of <code>JPPFQueue</code>.
	 */
	public JPPFQueue getQueue()
	{
		return (JPPFQueue) getSource();
	}

	/**
	 * Get the task bundle that is the cause of the event.
	 * @return  an instance of <code>BundleWrapper</code>.
	 */
	public BundleWrapper getBundleWrapper()
	{
		return bundleWrapper;
	}

	/**
	 * Determine if this event is a requeued bundle, following a node failure for instance.
	 * @return true if a bundle was requeued, false otherwise.
	 */
	public boolean isRequeue()
	{
		return requeue;
	}
}
