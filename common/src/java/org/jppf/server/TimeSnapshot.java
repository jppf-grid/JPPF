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
	 * @param count the unit count to which the time applies.
	 * @param totalCount the total unit count to which the time applies.
	 */
	public void newTime(long time, int count, int totalCount)
	{
		totalTime += time;
		if (count > 0)
		{
			latestTime = time/count;
			if (latestTime > maxTime) maxTime = latestTime;
			if (latestTime < minTime) minTime = latestTime;
			if (totalCount > 0) avgTime = (double) totalTime / (double) totalCount;
		}
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
