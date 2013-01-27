/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.net;

import java.net.InetAddress;

import org.jppf.utils.NetworkUtils;

/**
 * Represents a pattern used for IP addresses inclusion or exclusion lists.<br/>
 * A pattern represents a single value or a range of values for each component of an IP address.<br/>
 * @author Laurent Cohen
 */
public abstract class AbstractIPAddressPattern extends RangePattern
{
  /**
   * Initialize this object with the specified string pattern.
   * @param source the source pattern as a string.
   * @param config the configuration used for this pattern.
   * @throws IllegalArgumentException if the pattern is null or invalid.
   */
  public AbstractIPAddressPattern(final String source, final PatternConfiguration config) throws IllegalArgumentException
  {
    super(source, config);
  }

  /**
   * Determine whether the specified IP address matches this pattern.
   * No check is made to verify that the IP address is valid.
   * @param ip the ip to match as a string.
   * @return true if the address matches this pattern, false otherwise.
   */
  public boolean matches(final InetAddress ip)
  {
    return matches(NetworkUtils.toIntArray(ip));
  }
}
