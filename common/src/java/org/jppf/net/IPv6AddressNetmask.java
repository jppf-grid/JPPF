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

import java.net.InetAddress;

import org.jppf.utils.RegexUtils;

/**
 * Represents a netmask used for IPv6 addresses inclusion and exclusion lists.<br/>
 * A netmask is in CIDR notation and represents an IPv6 address of which only
 * the first N bits are significant.
 * <p>
 * If no slash is included and there is no double colon ("::") the string is
 * treated as a {@link org.jppf.net.IPv6AddressPattern}; otherwise it must be a
 * valid IPv6 address. This class does not handle Dotted Quad notation.
 * <p>
 * Examples:
 * <ul>
 * <li>1080::8:800:200C:417A represents a single IPv6 address</li>
 * <li>1080::8:800:200C:417A/128 represents a single IPv6 address</li>
 * <li>1080::8:800:200C:417A/125 represents all IPv6 addresses in the range
 * 1080::8:800:200C:4178 - 1080::8:800:200C:417F</li>
 * <li>1080::8:800:200C:417A/112 represents all IPv6 addresses in the range
 * 1080::8:800:200C:0 - 1080::8:800:200C:FFFF</li>
 * <li>1080::8:800:200C:417A/96 represents all IPv6 addresses in the range
 * 1080::8:800:0:0 - 1080::8:800:FFFF:FFFF</li>
 * </ul>
 * 
 * @author Daniel Widdis
 * @since 4.2
 */
public class IPv6AddressNetmask extends IPv6AddressPattern {

  /**
   * Initialize this object with the specified string pattern.
   * 
   * @param source
   *        the source pattern as a string.
   * @throws IllegalArgumentException
   *         if the pattern is null or invalid.
   */
  public IPv6AddressNetmask(final String source)
      throws IllegalArgumentException {
    super(netmaskToRange(source));
  }

  /**
   * Converts a String in CIDR notation to use the range notation expected by
   * AbstractIPAddressPattern
   * 
   * @param source
   *        IPv6 Address in CIDR notation
   * @return String representing this IP Address in range notation
   */
  private static String netmaskToRange(final String source) {
    // If no slash, use unmodified string directly to treat as
    // IPv6AddressPattern except for "::" parsing to follow
    String sourceIP = source;
    int netmask = 128;
    if (source.contains("/")) {
      String[] ipAndNetmask = RegexUtils.SLASH_PATTERN.split(source);
      sourceIP = ipAndNetmask[0];
      netmask = Integer.parseInt(ipAndNetmask[1]);
    }
    // Ensure netmask in range
    if (netmask < 0 || netmask > 128) {
      throw new IllegalArgumentException("Netmask " + netmask
          + " must be between 0 and 128");
    }
    // Handle leading or trailing "::" by appending a 0
    if (sourceIP.startsWith("::")) {
      sourceIP = "0".concat(sourceIP);
    }
    if (sourceIP.endsWith("::")) {
      sourceIP = sourceIP.concat("0");
    }
    // Sanity check IP format. Fail if no ":" or more than seven; if invalid
    // ":::" or more than one "::"
    int parts = RegexUtils.COLUMN_PATTERN.split(sourceIP).length;
    if (parts < 2 || parts > 8 || sourceIP.contains(":::")
        || sourceIP.split("::").length > 2) {
      //throw new IllegalArgumentException("Invalid IP address pattern: " + sourceIP);
      return sourceIP;
    }
    // Replace "::" with ":0:0:" as needed to create 8 parts
    if (sourceIP.contains("::")) {
      StringBuilder zeroes = new StringBuilder(":0");
      for (int i = parts; i < 8; i++) {
        zeroes.append(":0");
      }
      zeroes.append(":");
      sourceIP = sourceIP.replace("::", zeroes.toString());
    }
    String[] ip = RegexUtils.COLUMN_PATTERN.split(sourceIP);
    // This array should have exactly 8 parts if it was a valid IPv6
    if (ip.length != 8) {
      // The only way to be here is if no "::" is included. If netmask is 128,
      // return the original string to be used in IPv6AddressPattern
      if (netmask == 128) {
        return source;
      }
      // Invalid IP address combined with a netmask
      throw new IllegalArgumentException("Invalid IP address pattern: "
          + sourceIP + " (source=" + source + ")");
    }
    // Construct IP range from source. Significant bits left untouched.
    for (int i = 0; i < 8; i++) {
      int maskBits = 16 * (i + 1) - netmask;
      // If <= 0, all bits are significant; do nothing to this element
      // If >= 16, no bits are significant; leave blank in pattern
      if (maskBits >= 16) {
        ip[i] = "";
      } else if (maskBits > 0) {
        // Mask the insignificant bits
        int b = Integer.parseInt(ip[i], 16);
        int mask = (1 << maskBits) - 1;
        ip[i] = String.format("%s-%s", Integer.toHexString(b & ~mask),
            Integer.toHexString(b | mask));
      }
    }
    // Build a string from the ip array, stopping at insignificant elements
    StringBuilder pattern = new StringBuilder(ip[0]);
    for (int i = 1; i < 8; i++) {
      if (!ip[i].equals("")) {
        pattern.append(":").append(ip[i]);
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
    System.out.println("***** IP v6 *****");
    String[] ipv6patterns = { "1080:0:0:0:8:800:200C:417A", ":0::::::",
        "0:0:aa-bbcc:0:0:0:0:0", "1:2:3:4:5-:6:7:8", "::1", "::", "::1/128",
        "::1/112", "::1/96", "::1/80", "::1/64", "2001:db8::/32",
        "1080::8:800:200C:417A/128", "1080::0:8:800:200C:417A/127",
        "1080::0:0:8:800:200C:417A/126", "1080::0:0:8:800:200C:417A/125",
        "1080::0:0:8:800:200C:417A/124", "1080::0:0:8:800:200C:417A/123",
        "1080::0:0:8:800:200C:417A/97", "1080::0:0:8:800:200C:417A/96",
        "1080::0:0:8:800:200C:417A/95", "1080::0:0:8:800:200C:417A/94",
        "1080::0:0:8:800:200C:417A/1", "1080::0:0:8:800:200C:417A/2"
    };
    //String[] ipv6patterns = { "::" };
    String ip = "1080:0:0:0:8:800:200C:417A";
    for (int i = 0; i < ipv6patterns.length; i++) {
      try {
        IPv6AddressNetmask p = new IPv6AddressNetmask(ipv6patterns[i]);
        InetAddress addr = InetAddress.getByName(ip);
        System.out.println("pattern " + i + " for source '" + ipv6patterns[i]
            + "' = '" + p + "', ip match = " + p.matches(addr));
      } catch (Exception e) {
        System.out.println("#" + i + " pattern='" + ipv6patterns[i] + "' : " + e.getMessage());
      }
    }
  }
}
