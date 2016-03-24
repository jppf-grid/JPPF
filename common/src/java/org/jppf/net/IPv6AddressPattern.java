/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.net.*;

import org.jppf.utils.Range;


/**
 * Represents a pattern used for IPv6 addresses inclusion and exclusion lists.<br/>
 * A pattern represents a single value or a range of values for each component of an IPv6 address.<br/>
 * <p>Examples:
 * <ul>
 * <li>1080:0:0:0:8:800:200C:417A represents a single IPv6 address</li>
 * <li>1080:0:0:0:8:800:200C represents all IPv6 addresses in the range 1080:0:0:0:8:800:200C:0 - 1080:0:0:0:8:800:200C:FFFF</li>
 * <li>1080:0:0:0:8:800 represents all IPv6 addresses in the range 1080:0:0:0:8:800:0:0 - 1080:0:0:0:8:800:FFFF:FFFF</li>
 * <li>1080:0:0:0:8:800:200C-20FF represents all IP addresses where the first component is equal to 1080, the second to 0, ..., the 7th component in the range 200C - 200FF, and the 8th in the range 0 - FFFF
 * (equivalent to 1080:0:0:0:8:800:200C-20FF:0-FFFF)</li>
 * </ul>
 * <p>Syntax rules:
 * <p>1. An empty component is considered as a 0-FFFF range. Examples:
 * <ul>
 * <li>:2:3:4:5:6:7:8 is equivalent to 0-FFFF:2:3:4:5:6:7:8</li>
 * <li>1:2:3:4:5::7:8 is equivalent to 1:2:3:4:5:0-FFFF:7:8</li>
 * <li>1:2:3:4:5:6:7: is equivalent to 1:2:3:4:5:6:7 and to 1:2:3:4:5:6:7:0-FFFF</li>
 * </ul>
 * <p>2. Ranges with missing bounds but still including the &quot;-&quot; sign are interpreted as a range with the lower bound
 * equal to zero for a missing lower bound, and an upper bound equal to 0xFFFF if the upper bound is missing. Examples:
 * <ul>
 * <li>-128 is equivalent to 0-128</li>
 * <li>12- is equivalent to 12-FFFF</li>
 * <li>- is equivalent to an empty value and to 0-FFFF</li>
 * </ul>
 * <p>3. Valid values for range bounds and single values are positive integers in the range 0 ... 0xFFFF. A pattern containing any invalid value will be ignored.
 * <p>4. A pattern describing more than 8 components or containing characters other than hexadecimal digits, '-', ':' or spaces will be ignored.
 * @author Laurent Cohen
 */
public class IPv6AddressPattern extends AbstractIPAddressPattern
{
  /**
   * Initialize this object with the specified string pattern.
   * @param source the source pattern as a string.
   * @throws IllegalArgumentException if the pattern is null or invalid.
   */
  public IPv6AddressPattern(final String source) throws IllegalArgumentException
  {
    super(source, PatternConfiguration.IPV6_CONFIGURATION);
  }

  @Override
  public boolean matches(final InetAddress ip)
  {
    if (!(ip instanceof Inet6Address)) return false;
    return super.matches(ip);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<ranges.size(); i++)
    {
      if (i > 0) sb.append(config.getCompSeparator());
      Range<Integer> r = ranges.get(i);
      sb.append(Integer.toHexString(r.getLower()));
      if (!r.getLower().equals(r.getUpper())) sb.append('-').append(Integer.toHexString(r.getUpper()));
    }
    return sb.toString();
  }

  /**
   * Main method.
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    System.out.println("***** IP v6 *****");
    String[] ipv6patterns = { "1080:0:0:0:8:800:200C:417A", ":0::::::", "0:0:aa-bbcc:0:0:0:0:0", "1:2:3:4:5-:6:7:8", };
    String ip = "1080:0:0:0:8:800:200C:417A";
    for (int i=0; i<ipv6patterns.length; i++)
    {
      try
      {
        IPv6AddressPattern p = new IPv6AddressPattern(ipv6patterns[i]);
        InetAddress addr = InetAddress.getByName(ip);
        System.out.println("pattern " + i + " for source '" + ipv6patterns[i] + "' = '" + p + "', ip match = " + p.matches(addr));
      }
      catch (Exception e)
      {
        System.out.println("#" + i + " : " + e.getMessage());
      }
    }
  }
}
