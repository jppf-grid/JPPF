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

package org.jppf.scheduling;

import java.io.Serializable;
import java.text.*;
import java.util.Date;

/**
 * Instances of this class contain data used to setup a schedule.
 * This includes duration, date, date format.
 * @author Laurent Cohen
 */
public class JPPFSchedule implements Serializable
{
	/**
	 * Time in milliseconds, after which this task will be aborted.<br>
	 * A value of 0 or less indicates this task never times out.
	 */
	private long duration = 0L;
	/**
	 * Schedule date as a string.
	 */
	private String date = null;
	/**
	 * Format describing the schedule date.
	 */
	private SimpleDateFormat dateFormat = null;

	/**
	 * Initialize this schedule configuration with the specified duration.
	 * @param duration the duration in milliseconds.
	 */
	public JPPFSchedule(long duration)
	{
		this.duration = duration;
	}

	/**
	 * Initialize this schedule configuration with the specified duration.
	 * @param date the schedule date provided as a string.
	 * @param dateFormat the format in which the date is expressed (including locale and time zone information).
	 */
	public JPPFSchedule(String date, SimpleDateFormat dateFormat)
	{
		this.date = date;
		this.dateFormat = dateFormat;
	}

	/**
	 * Get the duration for this configuration.
	 * The time at which the duration starts dependends on who is using it.
	 * For instance, for scheduling a job, it starts when the job is inserted into the job queue by the server. 
	 * @return the timeout in milliseconds.
	 */
	public long getDuration()
	{
		return duration;
	}

	/**
	 * Get the scheduled date for this configuration.
	 * @return the date in string format.
	 */
	public String getDate()
	{
		return date;
	}

	/**
	 * Get the format of timeout date for this task.
	 * @return a <code>SimpleDateFormat</code> instance.
	 */
	public SimpleDateFormat getDateFormat()
	{
		return dateFormat;
	}

	/**
	 * Convert this schedule to a {@link Date} object.
	 * @param startDate the starting date to use if the schedule is expressed as a duration.
	 * @return this schedule expressed as a {@link Date}.
	 * @throws ParseException if parsing using the simple date format fails.
	 */
	public Date toDate(long startDate) throws ParseException
	{
		Date dt = null;
		if ((date == null) || (dateFormat == null))
		{
			dt = new Date(startDate + duration);
		}
		else
		{
			dt = dateFormat.parse(date);
		}
		return dt;
	}

	/**
	 * Convert this schedule to a long value.
	 * @param startDate the starting date to use if the schedule is expressed as a duration.
	 * @return this schedule expressed as a long.
	 * @throws ParseException if parsing using the simple date format fails.
	 */
	public long toLong(long startDate) throws ParseException
	{
		long result = 0L;
		if ((date == null) || (dateFormat == null))
		{
			result = startDate + duration;
		}
		else
		{
			Date dt = dateFormat.parse(date);
			result = dt.getTime();
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder("schedule[");
		if (date != null) sb.append("date=").append(date).append(", format=").append(dateFormat == null ? "null" : dateFormat.toPattern());
		else sb.append("delay=").append(duration);
		sb.append("]");
		return sb.toString();
	}
}
