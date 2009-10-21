/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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
import java.text.SimpleDateFormat;

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
	 * Set the duration for this configuration.
	 * Calling this setter will reset the date and date format values, as duration and date are mutually exclusive.
	 * @param duration the timeout in milliseconds.
	 */
	public void setDuration(long duration)
	{
		this.duration = duration;
		this.date = null;
		this.dateFormat = null;
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
	 * Set the date and date format for this configuration.<br>
	 * Calling this method will reset the duration, as duration and date are mutually exclusive.
	 * @param date the date to set in string representation.
	 * @param dateFormat the format of of the date to set.
	 * @see java.text.SimpleDateFormat
	 */
	public void setDate(String date, SimpleDateFormat dateFormat)
	{
		this.duration = 0L;
		this.date = date;
		this.dateFormat = dateFormat;
	}
}
