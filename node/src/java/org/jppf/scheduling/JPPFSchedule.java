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
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Time in milliseconds, after which this task will be aborted.<br>
	 * A value of 0 or less indicates this task never times out.
	 */
	private final long duration;
	/**
	 * Schedule date as a string.
	 */
	private final String date;
	/**
	 * Schedule date format as a string.
	 */
	private String format;
	/**
	 * Format describing the schedule date.
	 */
	private final transient SimpleDateFormat dateFormat;

	/**
	 * Initialize this schedule configuration with the specified duration.
	 * @param duration the duration in milliseconds.
	 */
	public JPPFSchedule(long duration)
	{
		this.duration = duration;
		this.date = null;
		this.format = null;
		this.dateFormat = null;
	}

	/**
	 * Initialize this schedule configuration with the specified duration.
	 * @param date the schedule date provided as a string.
	 * @param format the format in which the date is expressed (including locale and time zone information),
	 * as specified in the description of {@link SimpleDateFormat}.
	 */
	public JPPFSchedule(String date, String format)
	{
		this.duration = 0L;
		this.date = date;
		this.format = format;
		this.dateFormat = new SimpleDateFormat(format);
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
	 * @return the date format as a string pattern.
	 */
	public String getFormat()
	{
		return format;
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
		if ((date == null) || (format == null)) dt = new Date(startDate + duration);
		else dt = dateFormat.parse(date);
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
		if ((date == null) || (format == null)) result = startDate + duration;
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
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("schedule[");
		if (date != null) sb.append("date=").append(date).append(", format=").append(dateFormat == null ? "null" : dateFormat.toPattern());
		else sb.append("delay=").append(duration);
		sb.append(']');
		return sb.toString();
	}

	/**
	 * Determine whether this schedule was initialized with a date or not.
	 * @return true if a date was specified when constructing this schedule, false otherwise.
	 */
	public boolean hasDate()
	{
		return (date != null) && (format != null);
	}

	/**
	 * Determine whether this schedule was initialized with a duration or not.
	 * @return true if a duration (or timeout) was specified when constructing this schedule, false otherwise.
	 */
	public boolean hasDuration()
	{
		return duration > 0;
	}
}
