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

package org.jppf.server.node;

import java.util.*;

import org.jppf.node.NodeExecutionManager;
import org.jppf.node.event.*;
import org.jppf.utils.ServiceFinder;
import org.slf4j.*;

/**
 * This class handles the firing of node life cycle events and the listeners that subscribe to these events.
 * @author Laurent Cohen
 */
public class LifeCycleEventHandler
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(LifeCycleEventHandler.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * The list of listeners to this object's events.
	 */
	private List<NodeLifeCycleListener> listeners = new ArrayList<NodeLifeCycleListener>();

	/**
	 * The object that manages the job executions for the node.
	 */
	private NodeExecutionManager executionManager = null;

	/**
	 * Initialize this event handler witht he specified execution manager.
	 * @param executionManager the object that manages the job executions for the node.
	 */
	public LifeCycleEventHandler(NodeExecutionManager executionManager)
	{
		this.executionManager = executionManager;
	}

	/**
	 * Add a listener to the list of listeners.
	 * @param listener the listener to add.
	 */
	public void addNodeLifeCycleListener(NodeLifeCycleListener listener)
	{
		if (listener == null) return;
		synchronized (listeners)
		{
			listeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list of listeners.
	 * @param listener the listener to remove.
	 */
	public void removeNodeLifeCycleListener(NodeLifeCycleListener listener)
	{
		if (listener == null) return;
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}

	/**
	 * Notify all listeners that the node is starting.
	 */
	public void fireNodeStarting()
	{
		NodeLifeCycleEvent event = new NodeLifeCycleEvent(executionManager);
		synchronized (listeners)
		{
			for (NodeLifeCycleListener listener : listeners) listener.nodeStarting(event);
		}
	}

	/**
	 * Notify all listeners that the node is terminating.
	 */
	public void fireNodeEnding()
	{
		NodeLifeCycleEvent event = new NodeLifeCycleEvent(executionManager);
		synchronized (listeners)
		{
			for (NodeLifeCycleListener listener : listeners) listener.nodeEnding(event);
		}
	}

	/**
	 * Notify all listeners that the node is starting a job.
	 */
	public void fireJobStarting()
	{
		NodeLifeCycleEvent event = new NodeLifeCycleEvent(executionManager);
		synchronized (listeners)
		{
			for (NodeLifeCycleListener listener : listeners) listener.jobStarting(event);
		}
	}

	/**
	 * Notify all listeners that the node is completing a job.
	 */
	public void fireJobEnding()
	{
		NodeLifeCycleEvent event = new NodeLifeCycleEvent(executionManager);
		synchronized (listeners)
		{
			for (NodeLifeCycleListener listener : listeners) listener.jobEnding(event);
		}
	}

	/**
	 * Load all listener instances found in the class path via a service definition.
	 */
	public void loadListeners()
	{
		Iterator<NodeLifeCycleListener> it = ServiceFinder.lookupProviders(NodeLifeCycleListener.class);
		while (it.hasNext())
		{
			try
			{
				NodeLifeCycleListener listener = it.next();
				addNodeLifeCycleListener(listener);
				if (debugEnabled) log.debug("successfully added node life cycle listener " + listener.getClass().getName());
			}
			catch(Error e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}
}
