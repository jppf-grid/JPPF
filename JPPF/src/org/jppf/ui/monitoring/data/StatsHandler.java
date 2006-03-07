/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring.data;

import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.server.JPPFStats;
import org.jppf.server.app.JPPFClient;
import org.jppf.ui.monitoring.event.*;
import org.jppf.utils.JPPFConfiguration;

/**
 * This class provides a convenient access to the statistics obtained from the JPPF server.
 * @author Laurent Cohen
 */
public class StatsHandler implements StatsConstants
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(StatsHandler.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * The object holding the current statistics values.
	 */
	private JPPFStats stats = new JPPFStats();
	/**
	 * List of listeners registered wioth this stats formatter.
	 */
	private List<StatsHandlerListener> listeners = new ArrayList<StatsHandlerListener>();
	/**
	 * Timer used to query the stats from the server. 
	 */
	private Timer timer = null;
	/**
	 * Interval, in milliseconds, between refreshes from the server.
	 */
	private long refreshInterval = 2000L;
	/**
	 * Number of data snapshots kept in memeory.
	 */
	private int rolloverPosition = 50;
	/**
	 * The lsit of all snapshots kept in memory. the size of this list is alway equal to or less than
	 * the rollover position.
	 */
	private List<JPPFStats> dataList = new Vector<JPPFStats>();
	/**
	 * Cache of the data snapashots fields maps to their corresponding string values. 
	 */
	private List<Map<String, String>> stringValuesMaps = new Vector<Map<String, String>>();
	/**
	 * Cache of the data snapashots fields maps to their corresponding double values. 
	 */
	private List<Map<String, Double>> doubleValuesMaps = new Vector<Map<String, Double>>();
	/**
	 * Number of data updates so far.
	 */
	private int tickCount = 0;

	/**
	 * Initialize this formatter.
	 */
	public StatsHandler()
	{
		refreshInterval = JPPFConfiguration.getProperties().getLong("default.refresh.interval", 2000L);
		update(stats);
	}

	/**
	 * Start the automatic refresh of the stats through a timer.
	 */
	public void stopRefreshTimer()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * Start the automatic refresh of the stats through a timer.
	 */
	public void startRefreshTimer()
	{
		if (jppfClient == null) jppfClient = new JPPFClient();
		if (refreshInterval <= 0L) return;
		timer = new Timer("Update Timer");
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				requestUpdate();
			}
		};
		timer.schedule(task, 1000L, refreshInterval);
	}

	/**
	 * Get the interval between refreshes from the server.
	 * @return the interval in milliseconds.
	 */
	public long getRefreshInterval()
	{
		return refreshInterval;
	}

	/**
	 * Set the interval between refreshes from the server.
	 * @param refreshInterval the interval in milliseconds.
	 */
	public void setRefreshInterval(long refreshInterval)
	{
		this.refreshInterval = refreshInterval;
	}

	/**
	 * Request an udate from the server.
	 */
	public void requestUpdate()
	{
		try
		{
			update(jppfClient.requestStatistics());
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Shutdown, and eventually restart, the server.
	 * @param command the command to perform.
	 * @param shutdownDelay the delay before shutting down.
	 * @param restartDelay the delay, starting after shutdown, before restarting.
	 */
	public void requestShutdownRestart(String command, long shutdownDelay, long restartDelay)
	{
		try
		{
			jppfClient.submitAdminRequest(command, shutdownDelay, restartDelay);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Update the current statistics with new values obtained from the server.
	 * @param stats the object holding the new statistics values.
	 */
	public void update(JPPFStats stats)
	{
		tickCount++;
		stats.execution.avgTime = (stats.totalTasksExecuted > 0)
			? (double) stats.execution.totalTime / (double) stats.totalTasksExecuted : 0d;
		stats.transport.avgTime = (stats.totalTasksExecuted > 0)
			? (double) stats.transport.totalTime / (double) stats.totalTasksExecuted : 0d;
		stats.nodeExecution.avgTime = (stats.totalTasksExecuted > 0)
			? (double) stats.nodeExecution.totalTime / (double) stats.totalTasksExecuted : 0d;
		stats.queue.avgTime = (stats.totalQueued > 0)
			? (double) stats.queue.totalTime / (double) stats.totalQueued : 0d;
		dataList.add(stats);
		stringValuesMaps.add(StatsFormatter.getStringValuesMap(stats));
		doubleValuesMaps.add(StatsFormatter.getDoubleValuesMap(stats));
		int diff = dataList.size() - rolloverPosition;
		for (int i=0; i<diff; i++)
		{
			dataList.remove(0);
			stringValuesMaps.remove(0);
			doubleValuesMaps.remove(0);
		}
		fireStatsHandlerEvent();
	}

	/**
	 * Register a <code>StatsHandlerListener</code> with this stats formatter.
	 * @param listener the listener to register.
	 */
	public void addStatsHandlerListener(StatsHandlerListener listener)
	{
		if (listener != null) listeners.add(listener);
	}

	/**
	 * Unregister a <code>StatsHandlerListener</code> from this stats formatter.
	 * @param listener the listener to register.
	 */
	public void removeStatsHandlerListener(StatsHandlerListener listener)
	{
		if (listener != null) listeners.remove(listener);
	}

	/**
	 * Notify all listeners of a change in this stats formatter.
	 */
	public void fireStatsHandlerEvent()
	{
		StatsHandlerEvent event = new StatsHandlerEvent(this);
		for (StatsHandlerListener listener: listeners) listener.dataUpdated(event);
	}

	/**
	 * Get the number of data snapshots kept in memory.
	 * @return the rollover position as an int value.
	 */
	public int getRolloverPosition()
	{
		return rolloverPosition;
	}

	/**
	 * Set the number of data snapshots kept in memory. If the value if less than the former values, the corresponding,
	 * older, data snapshots will be deleted. 
	 * @param rolloverPosition the rollover position as an int value.
	 */
	public void setRolloverPosition(int rolloverPosition)
	{
		if (rolloverPosition <= 0) throw new IllegalArgumentException("zero or less not accepted: "+rolloverPosition);
		int diff = dataList.size() - rolloverPosition;
		for (int i=0; i<diff; i++)
		{
			dataList.remove(0);
			stringValuesMaps.remove(0);
			doubleValuesMaps.remove(0);
		}
		this.rolloverPosition = rolloverPosition;
	}

	/**
	 * Get the current number of data snapshots.
	 * @return the number of snapshots as an int.
	 */
	public int getStatsCount()
	{
		return dataList.size();
	}

	/**
	 * Get the data snapshot at a specified position.
	 * @param position the position to get the data at.
	 * @return a <code>JPPFStats</code> instance.
	 */
	public JPPFStats getStats(int position)
	{
		return dataList.get(position);
	}

	/**
	 * Get the latest data snapshot.
	 * @return a <code>JPPFStats</code> instance.
	 */
	public JPPFStats getLatestStats()
	{
		return dataList.get(getStatsCount() - 1);
	}

	/**
	 * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding string values.
	 * @param position the position to get the data at.
	 * @return a map of field names to their values represented as strings.
	 */
	public Map<String, String> getStringValues(int position)
	{
		return stringValuesMaps.get(position);
	}

	/**
	 * Get the mapping of the most recent data snapshot's fields to their corresponding string values.
	 * @return a map of field names to their values represented as strings.
	 */
	public Map<String, String> getLatestStringValues()
	{
		return stringValuesMaps.get(getStatsCount() - 1);
	}

	/**
	 * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding double values.
	 * @param position the position to get the data at.
	 * @return a map of field names to their values represented as double values.
	 */
	public Map<String, Double> getDoubleValues(int position)
	{
		return doubleValuesMaps.get(position);
	}

	/**
	 * Get the mapping of the most recent data snapshot's fields to their corresponding double values.
	 * @return a map of field names to their values represented as double values.
	 */
	public Map<String, Double> getLatestDoubleValues()
	{
		return doubleValuesMaps.get(getStatsCount() - 1);
	}

	/**
	 * Get the number of data updates so far.
	 * @return the nuber of updates as an int.
	 */
	public int getTickCount()
	{
		return tickCount;
	}
}
