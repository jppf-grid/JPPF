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

package org.jppf.utils;

import java.net.*;
import java.util.*;

import org.slf4j.*;

/**
 * Utility class that provides method to discover the network configuration of the current machine.
 * @author Laurent Cohen
 * @exclude
 */
public final class NetworkUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NetworkUtils.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Contains a set of all possible loopback addresses.
   * These are all the IPs in the 127.0.0.0-8 range.
   */
  private static final Set<String> LOOPBACK_ADDRESSES = createLoopbackAddresses();
  /**
   * Constant for empty array of host/ip pairs.
   */
  private static final HostIP[] NO_ADDRESS = new HostIP[0];

  /**
   * Instantiation opf this class is not permitted.
   */
  private NetworkUtils() {
  }

  /**
   * Get the non local (meaning neither localhost or loopback) address of the current host.
   * @return the ipv4 address as a string.
   */
  public static String getNonLocalHostAddress() {
    List<InetAddress> allAddresses = getNonLocalIPV4Addresses();
    return allAddresses.isEmpty() ? null : allAddresses.get(0).getHostAddress();
  }

  /**
   * Get a list of all known IP v4 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getIPV4Addresses() {
    return getIPAddresses(new InetAddressFilter() {
      @Override
      public boolean accepts(final InetAddress addr) {
        return addr instanceof Inet4Address;
      }
    });
  }

  /**
   * Get a list of all known non-local IP v4 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPV4Addresses() {
    return getIPAddresses(new InetAddressFilter() {
      @Override
      public boolean accepts(final InetAddress addr) {
        return (addr instanceof Inet4Address)
            && !(LOOPBACK_ADDRESSES.contains(addr.getHostAddress()) || "localhost".equals(addr.getHostName()));
      }
    });
  }

  /**
   * Get a list of all known IP v6 addresses for the current host.
   * @return a List of <code>Inet6Address</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getIPV6Addresses()
  {
    return getIPAddresses(new InetAddressFilter() {
      @Override
      public boolean accepts(final InetAddress addr) {
        return addr instanceof Inet6Address;
      }
    });
  }

  /**
   * Get a list of all known non-local IP v4 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPV6Addresses() {
    return getIPAddresses(new InetAddressFilter() {
      @Override
      public boolean accepts(final InetAddress addr) {
        return (addr instanceof Inet6Address) &&
            !(addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || "localhost".equals(addr.getHostName()));
      }
    });
  }

  /**
   * Get a list of all known IP addresses for the current host, according to the specified filter.
   * @param filter filters out unwanted addresses.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  private static List<InetAddress> getIPAddresses(final InetAddressFilter filter) {
    List<InetAddress> list = new ArrayList<>();
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        if (debugEnabled && addresses.hasMoreElements()) log.debug("found network interface: " + ni);
        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();
          if ((filter == null) || filter.accepts(addr)) list.add(addr);
        }
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return list;
  }

  /**
   * Get a list of all known non-local IP v4  and v6 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPAddresses() {
    List<InetAddress> addresses = new ArrayList<>();
    addresses.addAll(getNonLocalIPV4Addresses());
    addresses.addAll(getNonLocalIPV6Addresses());
    return addresses;
  }

  /**
   * Create a set of IPV4 addresses that map to the loopback address.
   * These are all the IPs in the 127.0.0.0/8 range.
   * @return a set of IP addresses as strings.
   */
  private static Set<String> createLoopbackAddresses() {
    Set<String> addresses = new HashSet<>();
    String s = "127.0.0.";
    for (int i=0; i<=8; i++) addresses.add(s + i);
    return addresses;
  }

  /**
   * Get the management host specified in the configuration file.
   * @return the host as a string.
   */
  public static String getManagementHost() {
    TypedProperties props = JPPFConfiguration.getProperties();
    String host = NetworkUtils.getNonLocalHostAddress();
    if (debugEnabled) log.debug("JMX host from NetworkUtils: "+host);
    if (host == null) host = "localhost";
    host = props.getString("jppf.management.host", host);
    if (debugEnabled) log.debug("computed JMX host: "+host);
    return host;
  }

  /**
   * Attempt to resolve an IP address into a host name.
   * @param ip the ip address to resolve.
   * @return the corresponding host name, or its IP if the name could not be resolved.
   */
  public static String getHostName(final String ip) {
    try {
      InetAddress a = InetAddress.getByName(ip);
      return a.getHostName();
    } catch(Exception e) {
      return ip;
    }
  }

  /**
   * Attempt to resolve an IP address or host name into a (host name, ip address) pair.
   * @param hostOrIP the ip address to resolve.
   * @return a {@link HostIP} instance.
   * @since 5.0
   */
  public static HostIP getHostIP(final String hostOrIP) {
    try {
      InetAddress a = InetAddress.getByName(hostOrIP);
      return new HostIP(a.getHostName(), a.getHostAddress());
    } catch(Exception e) {
      return new HostIP(hostOrIP, hostOrIP);
    }
  }

  /**
   * Get the subnet mask length of a given address.
   * @param addr the address for which to get the subnet mask.
   * @return the length (number of bits set to 1) for the corresponding subnet mask.
   */
  public static int getSubnetMaskLength(final InetAddress addr) {
    try {
      NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
      List<InterfaceAddress> intAddresses = ni.getInterfaceAddresses();
      for (InterfaceAddress ia: intAddresses) {
        if (addr.equals(ia.getAddress())) return ia.getNetworkPrefixLength();
      }
    } catch (Exception e) {
      log.error("Error getting subnet mask for address "  + addr, e);
    }
    return 0;
  }

  /**
   * Filter interface for the methods discovering available IP addresses.
   */
  private interface InetAddressFilter {
    /**
     * Determine whether the specified address is accepted.
     * @param addr the address to check.
     * @return true if the address is accepted, false otherwise.
     */
    boolean accepts(InetAddress addr);
  }

  /**
   * Main entry point.
   * @param args not used.
   */
  public static void main(final String...args) {
    System.out.println("This host's ip addresses: " + getNonLocalHostAddress());
  }

  /**
   * Convert an IP address int array.
   * @param addr the source address to convert.
   * @return an array of int values, or null if the source could not be parsed.
   */
  public static int[] toIntArray(final InetAddress addr) {
    try {
      byte[] bytes = addr.getAddress();
      String ip = addr.getHostAddress();
      int[] result = null;
      if (addr instanceof Inet6Address) {
        result = new int[8];
        // special processing for scoped IPv6 addresses
        int idx = ip.indexOf('%');
        if (idx >= 0) ip = ip.substring(0, idx);
        String[] comp = RegexUtils.COLUMN_PATTERN.split(ip);
        for (int i=0; i<comp.length; i++) result[i] = Integer.decode("0x" + comp[i].toLowerCase());
      } else {
        result = new int[4];
        String[] comp = RegexUtils.DOT_PATTERN.split(ip);
        for (int i=0; i<comp.length; i++) result[i] = Integer.valueOf(comp[i]);
      }
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Determine whether a textual address is an IP address (as opposed ot a host name) in IPv6 format.
   * @param host the textual representation of the address.
   * @return <code>true</code> if the textual address is an IP address in IPv6 format, <code>false</code> otherwise.
   */
  public static boolean isIPv6Address(final String host) {
    try {
      InetAddress addr = InetAddress.getByName(host);
      if ((addr instanceof Inet6Address) && addr.getHostAddress().equals(host)) return true;
    } catch (UnknownHostException ignore) {
    }
    return false;
  }

  /**
   * Parse a list of addresses.
   * @param addresses a string containing a space-separated list of host_name|ip_address pairs.
   * @return an array on <code>HostIP</code> instances.
   */
  public static HostIP[] parseAddresses(final String addresses) {
    if (addresses == null) return NO_ADDRESS;
    String[] pairs = RegexUtils.SPACES_PATTERN.split(addresses);
    if ((pairs == null) || (pairs.length <= 0)) return NO_ADDRESS;
    HostIP[] result = new HostIP[pairs.length];
    int count = 0;
    for (String pair: pairs) {
      String[] comps = RegexUtils.PIPE_PATTERN.split(pair);
      if ("".equals(comps[0])) comps[0] = null;
      if (comps[1] != null) {
        int idx = comps[1].indexOf('%');
        if (idx >= 0) comps[1] = comps[1].substring(0, idx);
      };
      result[count++] = new HostIP(comps[0], comps[1]);
    }
    return result;
  }
}
