/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server;

import java.io.Serializable;

/**
 * Convenience class for collecting time statistics.
 */
public class TimeSnapshot implements Serializable
{
	/**
	 * Title for this snapshot, used in the {@link #toString()} method.
	 */
	public String title = "";
	/**
	 * The total cumulated time.
	 */
	public long totalTime = 0L;
	/**
	 * The most recent time.
	 */
	public long latestTime = 0L;
	/**
	 * The minimum time.
	 */
	public long minTime = Long.MAX_VALUE;
	/**
	 * The maximum task execution time.
	 */
	public long maxTime = 0L;
	/**
	 * Average time (computed elsewhere).
	 */
	public double avgTime = 0d;
	
	/**
	 * Initialize this time snapshot with a specified title.
	 * @param title the title for this snapshot.
	 */
	public TimeSnapshot(String title)
	{
		this.title = title;
	}

	/**
	 * Called when a new time has been collected.
	 * @param time the new time used to compute the new statistics of this time snapshot.
	 */
	public void newTime(long time)
	{
		totalTime += time;
		latestTime = time;
		if (time > maxTime) maxTime = time;
		if (time < minTime) minTime = time;
	}
	
	/**
	 * Make a copy of this time snapshot object.
	 * @return a <code>TimeSnapshot</code> instance.
	 */
	public TimeSnapshot makeCopy()
	{
		TimeSnapshot ts = new TimeSnapshot(title);
		ts.totalTime = totalTime;
		ts.latestTime = latestTime;
		ts.minTime = minTime;
		ts.maxTime = maxTime;
		ts.avgTime = avgTime;
		return ts;
	}

	/**
	 * Get a string representation of this stats object.
	 * @return a string display the various stats values.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(title).append(" total time : ").append(totalTime).append("\n");
		sb.append(title).append(" latest time : ").append(latestTime).append("\n");
		sb.append(title).append(" min time : ").append(minTime).append("\n");
		sb.append(title).append(" max time : ").append(maxTime).append("\n");
		sb.append(title).append(" avg time : ").append(avgTime).append("\n");
		return sb.toString();
	}
}