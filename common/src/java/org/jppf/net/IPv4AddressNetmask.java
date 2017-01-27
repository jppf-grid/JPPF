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

package org.jppf.net;

import java.net.InetAddress;

import org.jppf.utils.RegexUtils;

/**
 * Represents a netmask used for IP addresses include or exclude lists.<br/>
 * A netmask is in CIDR notation and represents an IP address of which only the
 * first N bits are significant.
 * <p>
 * If no slash is included, the string is treated as a
 * {@link org.jppf.net.IPv4AddressPattern}; otherwise it must be a valid IPv4
 * address.
 * <p>
 * Examples:
 * <ul>
 * <li>192.168.1.10 represents a single IP address</li>
 * <li>192.168.1.10/32 represents a single IP address, where all 32 bits are
 * significant.</li>
 * <li>192.168.1.0/24 represents all IP addresses in the range 192.168.1.0 -
 * 192.168.1.255</li>
 * <li>192.168.0.0/16 represents all IP addresses in the range 192.168.0.0 -
 * 192.168.255.255</li>
 * <li>192.160.0.0/12 represents all IP addresses in the range 192.160.0.0 -
 * 192.175.255.255</li>
 * </ul>
 * 
 * @author Daniel Widdis
 * @since 4.2
 */
public class IPv4AddressNetmask extends IPv4AddressPattern {
  /**
   * Initialize this object with the specified string pattern.
   * 
   * @param source
   *        the source pattern as a string.
   * @throws IllegalArgumentException
   *         if the pattern is null or invalid.
   */
  public IPv4AddressNetmask(final String source)
      throws IllegalArgumentException {
    super(netmaskToRange(source));
  }

  /**
   * Converts a String in CIDR notation to use the range notation expected by
   * AbstractIPAddressPattern
   * 
   * @param source
   *        IPv4 Address in CIDR notation
   * @return String representing this IP Address in range notation
   */
  private static String netmaskToRange(final String source) {
    // If no slash, return string directly to treat as IPv4AddressPattern
    if (!source.contains("/")) {
      return source;
    }
    String[] ipAndNetmask = RegexUtils.SLASH_PATTERN.split(source);
    // Ensure IP address has four parts
    String[] ip = RegexUtils.DOT_PATTERN.split(ipAndNetmask[0]);
    if (ip.length != 4) {
      return source;
    }
    // Ensure netmask in valid range
    int netmask = Integer.parseInt(ipAndNetmask[1]);
    if (netmask < 0 || netmask > 32) {
      throw new IllegalArgumentException("Netmask " + netmask
          + " must be between 0 and 32");
    }
    // Construct IP range from source. Significant bits left untouched.
    for (int i = 0; i < 4; i++) {
      int maskBits = 8 * (i + 1) - netmask;
      // If <= 0, all bits are significant; do nothing to this element
      // If >= 8, no bits are significant; leave blank in pattern
      if (maskBits >= 8) {
        ip[i] = "";
      } else if (maskBits > 0) {
        // Mask the insignificant bits
        int b = Integer.parseInt(ip[i]);
        int mask = (1 << maskBits) - 1;
        ip[i] = String.format("%d-%d", b & ~mask, b | mask);
      }
    }
    // Build a string from the ip array, stopping at insignificant elements
    StringBuilder pattern = new StringBuilder(ip[0]);
    for (int i = 1; i < 4; i++) {
      if (!ip[i].equals("")) {
        pattern.append(".").append(ip[i]);
      }
    }
    return pattern.toString();
  }

  /**
   * Main method.
   * 
   * @param args
   *        not used.
   */
  public static void main(final String[] args) {
    System.out.println("***** IP v4 *****");
    String[] ipv4patterns = {"192.168.1.10", "192.168.1.11", "192.168.1.0/24",
        "192.168.0.0/16", "192.160.0.0/13", "192.0.0.0/4", "1.2.0.0/16",
        "1.2.3.0/8", "1.2.3.4/32", "1.2.3.4/31", "1.2.3.4/30", "1.2.3.4/29",
        "1.2.3.4/28", "1.2.3.4/27", "1.2.3.4/26", "1.2.3.4/25"};
    // String[] patterns = { " 1. 2 .  3. 4 - 8 " };
    String ip = "192.168.1.11";
    for (int i = 0; i < ipv4patterns.length; i++) {
      try {
        IPv4AddressNetmask p = new IPv4AddressNetmask(ipv4patterns[i]);
        InetAddress addr = InetAddress.getByName(ip);
        System.out.println("pattern " + i + " for source '" + ipv4patterns[i]
            + "' = '" + p + "', ip match = " + p.matches(addr));
      } catch (Exception e) {
        System.out.println("#" + i + " : " + e.getMessage());
      }
    }
  }
}
