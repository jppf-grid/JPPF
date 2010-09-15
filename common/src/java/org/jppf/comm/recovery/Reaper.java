/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.comm.recovery;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class checks, at regular intervals, the recovery-specific connections to remote peers,
 * and detects whether the corresponding peer is dead.
 * @author Laurent Cohen
 */
public class Reaper
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(Reaper.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Size of the reaper thread pool.
	 */
	private int poolSize = 1;
	/**
	 * The interval between two runs of the reaper.
	 */
	private long runInterval = 60000L;
	/**
	 * Reaper thread pool.
	 */
	private ExecutorService threadPool = null;
	/**
	 * The server that handles the connections to the remote peers.
	 */
	private RecoveryServer server = null;
	/**
	 * The timer that performs scheduled connections checks at regular intervals.
	 */
	private Timer timer  = null;
	/**
	 * The list of listeners to this object's events.
	 */
	private List<Pair<ReaperListener, ReaperEventFilter>> listeners = new ArrayList<Pair<ReaperListener, ReaperEventFilter>>();

	/**
	 * Initialize this reaper with the specified recovery server.
	 * @param server the server that handles the connections to the remote peers.
	 * @param poolSize this reaper's thread pool size.
	 * @param runInterval the interval between two runs of this reaper.
	 */
	public Reaper(RecoveryServer server, int poolSize, long runInterval)
	{
		this.server = server;
		this.poolSize = poolSize;
		this.runInterval = runInterval;
		threadPool = Executors.newFixedThreadPool(poolSize, new JPPFThreadFactory("Reaper"));
		timer = new Timer();
		timer.schedule(new ReaperTask(), 0L, runInterval);
	}

	/**
	 * Add a listener to the list of listeners.
	 * @param listener the listener to add.
	 */
	public void addReaperListener(ReaperListener listener)
	{
		addReaperListener(listener, null);
	}

	/**
	 * Add a listener / filter pair to the list of listeners.
	 * @param listener the listener to add.
	 * @param filter the filter to apply to the listener.
	 */
	public void addReaperListener(ReaperListener listener, ReaperEventFilter filter)
	{
		if (listener == null) return;
		synchronized (listeners)
		{
			listeners.add(new Pair<ReaperListener, ReaperEventFilter>(listener, filter));
		}
	}

	/**
	 * Remove a listener from the list of listeners.
	 * @param listener the listener to remove.
	 */
	public void removeReaperListener(ReaperListener listener)
	{
		if (listener == null) return;
		synchronized (listeners)
		{
			listeners.remove(listener);
		}
	}

	/**
	 * Remove a listener / filter pair from the list of listeners.
	 * @param listener the listener to remove.
	 * @param filter the filter applied to the listener.
	 */
	public void removeReaperListener(ReaperListener listener, ReaperEventFilter filter)
	{
		if (listener == null) return;
		synchronized (listeners)
		{
			listeners.remove(new Pair<ReaperListener, ReaperEventFilter>(listener, filter));
		}
	}

	/**
	 * Notify all listeners that a connection has failed.
	 * @param connection the server-side connection that failed.
	 */
	private void fireReaperEvent(ServerConnection connection)
	{
		ReaperEvent event = new ReaperEvent(connection);
		synchronized (listeners)
		{
			for (Pair<ReaperListener, ReaperEventFilter> pair: listeners)
			{
				ReaperEventFilter filter = pair.second();
				if ((filter == null) || filter.isEventEnabled(event)) pair.first().connectionFailed(event);
			}
		}
	}

	/**
	 * The timer task that submits the connection checks to the the executor.
	 */
	private class ReaperTask extends TimerTask
	{
		/**
		 * {@inheritDoc}
		 */
		public void run()
		{
			ServerConnection[] connections = server.connections();
			List<Future<?>> futures = new ArrayList<Future<?>>();
			for (ServerConnection c: connections) futures.add(threadPool.submit(c));
			for (Future<?> f: futures)
			{
				try
				{
					f.get();
				}
				catch (Exception e)
				{
					if (debugEnabled) log.debug(e.getMessage(), e);
				}
			}
			for (ServerConnection c: connections) 
			{
				if (!c.isOk())
				{
					fireReaperEvent(c);
				}
				else if (!c.isInitialized())
				{
					fireReaperEvent(c);
					c.setInitialized(true);
				}
			}
		}
	}
}
