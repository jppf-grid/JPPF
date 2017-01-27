/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.location;

import java.util.EventObject;

/**
 * Instances of this class represent events occurring when performing I/O operations between
 * {@link Location} instances.
 * @see java.util.EventObject
 * @author Laurent Cohen
 */
public class LocationEvent extends EventObject
{
  /**
   * The number of bytes that were transferred to another location.
   */
  private long n = 0;

  /**
   * Initialize this event with its source location.
   * @param source - the location on which the event is occurring.
   * @param n - the number of bytes that were transferred to another location.
   */
  public LocationEvent(final Location<?> source, final long n)
  {
    super(source);
    this.n = n;
  }

  /**
   * Get the number of bytes transferred during this event.
   * @return the number of bytes as an int.
   */
  public long getTransferredBytes()
  {
    return n ;
  }

  /**
   * Get the source location, on which the event occurred.
   * @return the source as a <code>Location</code> instance.
   */
  public Location<?> getLocation()
  {
    return (Location<?>) getSource();
  }
}
