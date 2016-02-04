/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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


/**
 * Represents a pattern used for IP addresses include or exclude lists.<br/>
 * A pattern represents a single value or a range of values for each component of an IP address.<br/>
 * <p>Examples:
 * <ul>
 * <li>192.168.1.10 represents a single IP address</li>
 * <li>192.168.1 represents all IP addresses in the range 192.168.1.0 - 192.168.1.255</li>
 * <li>192.168 represents all IP addresses in the range 192.168.0.0 - 192.168.255.255</li>
 * <li>192.168-170.10 represents all IP addresses where the first component is equal to 192, the second in the range 168 - 170, the third equal to 10, and the fourth in the range 0 - 255
 * (equivalent to 192.168-170.10.0-255)</li>
 * <li>192.168-170.10.1-32 represents all IP addresses where the first component is equal to 192, the second in the range 12 - 120, the third equal to 10, and the fourth in the range 1 - 32</li>
 * </ul>
 * <p>Syntax rules:
 * <p>1. An empty component is considered as a 0-255 range. Examples:
 * <ul>
 * <li>.2.3.4 is equivalent to 0-255.2.3.4</li>
 * <li>1.2..4 is equivalent to 1.2.0-255.4</li>
 * <li>1.2.3. is equivalent to 1.2.3 and to 1.2.3.0-255</li>
 * </ul>
 * <p>2. Ranges with missing bounds but still including the &quot;-&quot; sign are interpreted as a range with the lower bound
 * equal to zero for a missing lower bound, and an upper bound equal to 255 if the upper bound is missing. Examples:
 * <ul>
 * <li>-128 is equivalent to 0-128</li>
 * <li>12- is equivalent to 12-255</li>
 * <li>- is equivalent to an empty value and to 0-255</li>
 * </ul>
 * <p>3. Valid values for range bounds and single values are positive integers in the range 0 ... 255. A pattern containing any invalid value will be ignored.
 * <p>4. A pattern describing more than 4 components or containing characters other than decimal digits, '-', '.' or spaces will be ignored.
 * @author Laurent Cohen
 */
public class IPv4AddressPattern extends AbstractIPAddressPattern
{
  /**
   * Initialize this object with the specified string pattern.
   * @param source the source pattern as a string.
   * @throws IllegalArgumentException if the pattern is null or invalid.
   */
  public IPv4AddressPattern(final String source) throws IllegalArgumentException
  {
    super(source, PatternConfiguration.IPV4_CONFIGURATION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matches(final InetAddress ip)
  {
    if (!(ip instanceof Inet4Address)) return false;
    return super.matches(ip);
  }

  /**
   * Main method.
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    System.out.println("***** IP v4 *****");
    String[] ipv4patterns = {
        "192.168.1.10", "192.168.1.11", "192.168.1", "192.168", "192.168-170.1", "192.168-170.1.1-32",
        ".2.3.4", "1.2..4", "1.2.3.", "1.2.3.4-", "1.2.3.-4", " 1. 2 .  3. 4 - 8 ", "1.-.3.", "1.2.3.-",
    };
    //String[] patterns = { " 1. 2 .  3. 4 - 8 " };
    String ip = "192.168.1.11";
    for (int i=0; i<ipv4patterns.length; i++)
    {
      try
      {
        IPv4AddressPattern p = new IPv4AddressPattern(ipv4patterns[i]);
        InetAddress addr = InetAddress.getByName(ip);
        System.out.println("pattern " + i + " for source '" + ipv4patterns[i] + "' = '" + p + "', ip match = " + p.matches(addr));
      }
      catch (Exception e)
      {
        System.out.println("#" + i + " : " + e.getMessage());
      }
    }
  }
}
