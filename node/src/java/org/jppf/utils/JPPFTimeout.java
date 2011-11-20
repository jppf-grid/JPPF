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

package org.jppf.utils;

import java.util.concurrent.TimeUnit;

/**
 * Convenience class used to express a timeout in various units of time.
 * @see java.util.concurrent.TimeUnit
 * @author Laurent Cohen
 */
public class JPPFTimeout extends Pair<TimeUnit, Long>
{
  /**
   * Initialize this timeout with the specified time unit and value.
   * @param unit the time unit to use to measure the time.
   * @param value the value expressed in <code>timeUnit</code> units.
   * @throws IllegalArgumentException if the time unit is null, the value is null or the value is less than zero.
   */
  public JPPFTimeout(final TimeUnit unit, final Long value)
  {
    super(unit, value);
    if (unit == null) throw new IllegalArgumentException("Time unit cannot be null");
    if (value == null) throw new IllegalArgumentException("Value cannot be null");
    if (value < 0L) throw new IllegalArgumentException("Time value cannot be negative");
  }

  /**
   * Get the time unit used to measure the time.
   * @return a {@link TimeUnit} enum value.
   */
  public TimeUnit unit()
  {
    return first();
  }

  /**
   * Get the value expressed in <code>getUnit()</code> units.
   * @return the timeout value as a <code>Long</code>.
   */
  public Long value()
  {
    return second();
  }
}
