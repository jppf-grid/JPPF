/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import java.time.*;
import java.util.Date;

/**
 * Instances of this class contain data used to setup a schedule.
 * This includes duration, date, date format.
 * @author Laurent Cohen
 */
public class JPPFSchedule implements Serializable {
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
  private final String format;
  /**
   * Format describing the schedule date.
   */
  private transient SimpleDateFormat dateFormat;
  /**
   * A date expressed as a {@link ZonedDateTime} instance.
   */
  private final ZonedDateTime zonedDateTime;
  /**
   * A duration expressed as a {@link Duration} instance.
   */
  private final Duration durationObject;

  /**
   * Initialize this schedule configuration with the specified duration.
   * @param duration the duration in milliseconds. If the value is zero or less, then this schedule will expire immediately.
   */
  public JPPFSchedule(final long duration) {
    this(duration, null, null, null, null);
  }

  /**
   * Initialize this schedule configuration with the specified fixed date annd date format.
   * @param date the schedule date provided as a string. If the date is equal to or before the creation time of this schedule, then this schedule will expire immediately.
   * @param format the format in which the date is expressed (including locale and time zone information),
   * as specified in the description of {@link SimpleDateFormat}.
   */
  public JPPFSchedule(final String date, final String format) {
    this(0L, date, format, null, null);
  }

  /**
   * Initialize this schedule configuration with the specified date.
   * @param zonedDateTime the schedule date. If the date is equal to or before the creation time of this schedule, then this schedule will expire immediately.
   */
  public JPPFSchedule(final ZonedDateTime zonedDateTime) {
    this(0L, null, null, zonedDateTime, null);
  }

  /**
   * Initialize this schedule configuration with the specified duration.
   * @param duration the duration before this schedule expires or times out. If duaration resolves to a value of zero or less, then this schedule will expire immediately.
   */
  public JPPFSchedule(final Duration duration) {
    this(0L, null, null, null, duration);
  }

  /**
   * Genric constructor invoked by all other constructors.
   * @param duration the duration in milliseconds, or a value <= 0.
   * @param date the schedule date provided as a string, or {@code null}.
   * @param format the format in which the date is expressed, or {@code null}.
   * @param zonedDateTime the schedule date and time, or {@code null}.
   * @param durationObject the duration before this schedule expires or times out, or {@code null}.
   */
  private JPPFSchedule(final long duration, final String date, final String format, final ZonedDateTime zonedDateTime, final Duration durationObject) {
    this.duration = duration;
    this.date = date;
    this.format = format;
    this.zonedDateTime = zonedDateTime;
    this.durationObject = durationObject;
  }

  /**
   * Get the duration for this configuration.
   * The time at which the duration starts dependants on who is using it.
   * For instance, for scheduling a job, it starts when the job is inserted into the job queue by the server.
   * @return the timeout in milliseconds.
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Get the scheduled date for this configuration.
   * @return the date in string format.
   */
  public String getDate() {
    return date;
  }

  /**
   * Get the format of timeout date for this task.
   * @return the date format as a string pattern.
   */
  public String getFormat() {
    return format;
  }

  /**
   * Get the schedule date expressed as a {@link ZonedDateTime} instance.
   * @return a {@link ZonedDateTime} if this schedule was constructed with a {@link ZonedDateTime}, otherwise {@code null}.
   */
  public ZonedDateTime getZonedDateTime() {
    return zonedDateTime;
  }

  /**
   * Get the schedule duration expressed as a {@link Duration} instance.
   * @return a {@link Duration} if this schedule was constructed with a a {@link Duration}, otherwise {@code null}.
   */
  public Duration getDurationObject() {
    return durationObject;
  }

  /**
   * Convert this schedule to a {@link Date} object.
   * @param startDate the starting date to use if the schedule is expressed as a duration.
   * @return this schedule expressed as a {@link Date}.
   * @throws ParseException if parsing using the simple date format fails.
   * @deprecated use {@link #toLong(long) toLong(long startDate)} instead.
   */
  public Date toDate(final long startDate) throws ParseException {
    Date dt = null;
    if ((date == null) || (format == null)) dt = new Date(startDate + duration);
    else {
      if (dateFormat == null) dateFormat = new SimpleDateFormat(format);
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
  public long toLong(final long startDate) throws ParseException {
    long result = 0L;
    if (duration > 0L) result = startDate + duration;
    else if ((date != null) && (format != null)) {
      if (dateFormat == null) dateFormat = new SimpleDateFormat(format);
      final Date dt = dateFormat.parse(date);
      result = dt.getTime();
    }
    else if (zonedDateTime != null) result = zonedDateTime.toInstant().toEpochMilli();
    else if (durationObject != null) result = startDate + durationObject.toMillis();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    if (date != null) sb.append("date=").append(date).append(", format=").append(format == null ? "null" : format);
    else if (duration > 0L) sb.append("delay=").append(duration);
    else if (zonedDateTime != null) sb.append("date=").append(zonedDateTime);
    else if (durationObject != null) sb.append("delay=").append(durationObject);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Determine whether this schedule was initialized with a {@link Date}.
   * @return {@code true} if a {@link Date} was specified when constructing this schedule, {@code false} otherwise.
   */
  public boolean hasDate() {
    return (date != null) && (format != null);
  }

  /**
   * Determine whether this schedule was initialized with a duration.
   * @return {@code true} if a {@code long} duration was specified when constructing this schedule, {@code false} otherwise.
   */
  public boolean hasDuration() {
    return duration > 0;
  }

  /**
   * Determine whether this schedule was initialized with a {@link ZonedDateTime}.
   * @return {@code true} if a {@link ZonedDateTime} was specified when constructing this schedule, {@code false} otherwise.
   */
  public boolean hasZonedDateTime() {
    return zonedDateTime != null;
  }

  /**
   * Determine whether this schedule was initialized with a {@link Duration}.
   * @return {@code true} if a {@link Duration} was specified when constructing this schedule, {@code false} otherwise.
   */
  public boolean hasDurationObject() {
    return durationObject != null;
  }
}
