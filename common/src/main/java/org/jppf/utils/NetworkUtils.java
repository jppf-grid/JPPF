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

package org.jppf.utils;

import java.net.*;
import java.util.*;

import org.jppf.comm.discovery.IPFilter;
import org.jppf.utils.configuration.JPPFProperties;
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
  private static final HostIP[] NO_ADDRESS = { };
  /**
   * Performance optimization by caching the discovered IP addresses.
   */
  private static List<InetAddress> ipv4Addresses, ipv6Addresses, nonLocalIpv4Addresses, nonLocalIpv6Addresses;
  static {
    init();
  }

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
    final List<InetAddress> allAddresses = getNonLocalIPAddresses();
    return allAddresses.isEmpty() ? null : allAddresses.get(0).getHostAddress();
  }

  /**
   * Get a list of all known IP v4 addresses for the current host.
   * @return a List of {@link InetAddress} instances, may be empty but never null.
   */
  public static List<InetAddress> getIPV4Addresses() {
    if (ipv4Addresses != null) return ipv4Addresses; 
    return getIPAddresses(addr -> addr instanceof Inet4Address);
  }

  /**
   * Get a list of all known non-local IP v4 addresses for the current host.
   * @return a List of {@link InetAddress} instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPV4Addresses() {
    if (nonLocalIpv4Addresses != null) return nonLocalIpv4Addresses; 
    return getIPAddresses(addr -> (addr instanceof Inet4Address) && !(LOOPBACK_ADDRESSES.contains(addr.getHostAddress()) || "localhost".equals(addr.getHostName())));
  }

  /**
   * Get a list of all known IP v6 addresses for the current host.
   * @return a List of <code>Inet6Address</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getIPV6Addresses() {
    if (ipv6Addresses != null) return ipv6Addresses; 
    return getIPAddresses(addr -> addr instanceof Inet6Address);
  }

  /**
   * Get a list of all known non-local IP v4 addresses for the current host.
   * @return a List of {@link InetAddress} instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPV6Addresses() {
    if (nonLocalIpv6Addresses != null) return nonLocalIpv6Addresses; 
    return getIPAddresses(addr -> (addr instanceof Inet6Address) && !(addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || "localhost".equals(addr.getHostName())));
  }

  /**
   * Get a list of all known IP addresses for the current host, according to the specified filter.
   * @param filter filters out unwanted addresses.
   * @return a List of {@link InetAddress} instances, may be empty but never null.
   */
  private static List<InetAddress> getIPAddresses(final InetAddressFilter filter) {
    final List<InetAddress> list = new ArrayList<>();
    try {
      final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        final NetworkInterface ni = interfaces.nextElement();
        final Enumeration<InetAddress> addresses = ni.getInetAddresses();
        if (debugEnabled && addresses.hasMoreElements()) log.debug("found network interface: " + ni);
        while (addresses.hasMoreElements()) {
          final InetAddress addr = addresses.nextElement();
          if ((filter == null) || filter.accepts(addr)) list.add(addr);
        }
      }
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    return list;
  }

  /**
   * Get a list of all known non-local IP v4  and v6 addresses for the current host.
   * @return a List of {@link InetAddress} instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPAddresses() {
    final List<InetAddress> addresses = new ArrayList<>();
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
    final Set<String> addresses = new HashSet<>();
    final String s = "127.0.0.";
    for (int i=0; i<=8; i++) addresses.add(s + i);
    return addresses;
  }

  /**
   * Get the management host specified in the configuration file.
   * @return the host as a string.
   */
  public static String getManagementHost() {
    final String host = JPPFConfiguration.getProperties().getString(JPPFProperties.MANAGEMENT_HOST.getName(), retrieveManagementHostOrLocalhost());
    if (debugEnabled) log.debug("computed JMX host: " + host);
    return host;
  }

  /**
   * Get the management host specified in the configuration file.
   * @return the host as a string.
   */
  public static String retrieveManagementHostOrLocalhost() {
    final String host = NetworkUtils.getNonLocalHostAddress();
    if (debugEnabled) log.debug("JMX host from NetworkUtils: "+host);
    return (host == null) ? "localhost" : host;
  }

  /**
   * Attempt to resolve an IP address into a host name.
   * @param ip the ip address to resolve.
   * @return the corresponding host name, or its IP if the name could not be resolved.
   */
  public static String getHostName(final String ip) {
    try {
      final InetAddress a = InetAddress.getByName(ip);
      return a.getHostName();
    } catch(@SuppressWarnings("unused") final Exception e) {
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
      final InetAddress a = InetAddress.getByName(hostOrIP);
      return new HostIP(a.getHostName(), a.getHostAddress());
    } catch(@SuppressWarnings("unused") final Exception e) {
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
      final NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
      final List<InterfaceAddress> intAddresses = ni.getInterfaceAddresses();
      for (InterfaceAddress ia: intAddresses) {
        if (addr.equals(ia.getAddress())) return ia.getNetworkPrefixLength();
      }
    } catch (final Exception e) {
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
      String ip = addr.getHostAddress();
      int[] result = null;
      if (addr instanceof Inet6Address) {
        result = new int[8];
        // special processing for scoped IPv6 addresses
        final int idx = ip.indexOf('%');
        if (idx >= 0) ip = ip.substring(0, idx);
        final String[] comp = RegexUtils.COLUMN_PATTERN.split(ip);
        for (int i=0; i<comp.length; i++) result[i] = Integer.decode("0x" + comp[i].toLowerCase());
      } else {
        result = new int[4];
        final String[] comp = RegexUtils.DOT_PATTERN.split(ip);
        for (int i=0; i<comp.length; i++) result[i] = Integer.valueOf(comp[i]);
      }
      return result;
    } catch (@SuppressWarnings("unused") final Exception e) {
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
      final InetAddress addr = InetAddress.getByName(host);
      if ((addr instanceof Inet6Address) && addr.getHostAddress().equals(host)) return true;
    } catch (@SuppressWarnings("unused") final UnknownHostException ignore) {
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
    final String[] pairs = RegexUtils.SPACES_PATTERN.split(addresses);
    if ((pairs == null) || (pairs.length <= 0)) return NO_ADDRESS;
    final HostIP[] result = new HostIP[pairs.length];
    int count = 0;
    for (String pair: pairs) {
      final String[] comps = RegexUtils.PIPE_PATTERN.split(pair);
      if ("".equals(comps[0])) comps[0] = null;
      if (comps[1] != null) {
        final int idx = comps[1].indexOf('%');
        if (idx >= 0) comps[1] = comps[1].substring(0, idx);
      };
      result[count++] = new HostIP(comps[0], comps[1]);
    }
    return result;
  }

  /**
   * Performance optimization by caching the discovered IP addresses.
   */
  private static void init() {
    ipv4Addresses = getIPAddresses(addr -> addr instanceof Inet4Address);
    nonLocalIpv4Addresses = new ArrayList<>(ipv4Addresses.size());
    for (final InetAddress addr: ipv4Addresses) {
      if ((addr instanceof Inet4Address) && !(LOOPBACK_ADDRESSES.contains(addr.getHostAddress()) || "localhost".equals(addr.getHostName()))) nonLocalIpv4Addresses.add(addr);
    }
    ipv6Addresses = getIPAddresses(addr -> addr instanceof Inet6Address);
    nonLocalIpv6Addresses = new ArrayList<>(ipv6Addresses.size());
    for (final InetAddress addr: ipv6Addresses) {
      if ((addr instanceof Inet6Address) && !(addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || "localhost".equals(addr.getHostName())))
        nonLocalIpv6Addresses.add(addr);
    }
  }

  /**
   * @param propertyPrefix prefix for the properties deifining include and exclude IP address patterns.
   * @return an {@link InetAddress} to bind to, or {@code null} to bind to all interfaces.
   */
  public static InetAddress getInetAddress(final String propertyPrefix) {
    return getInetAddress(JPPFConfiguration.getProperties(), propertyPrefix);
  }

  /**
   * @param config the config to use.
   * @param propertyPrefix prefix for the properties deifining include and exclude IP address patterns.
   * @return an {@link InetAddress} to bind to, or {@code null} to bind to all interfaces.
   */
  public static InetAddress getInetAddress(final TypedProperties config, final String propertyPrefix) {
    final IPFilter filter = new IPFilter(config, propertyPrefix);
    if (!filter.hasPattern()) return null;
    final List<InetAddress> addresses = NetworkUtils.getNonLocalIPAddresses();
    if ((addresses != null) && !addresses.isEmpty()) {
      final List<InetAddress> accepted = new ArrayList<>();
      for (final InetAddress addr: addresses) {
        if (filter.isAddressAccepted(addr)) accepted.add(addr);
      }
      if (debugEnabled) log.debug("accepted addresses for '{}' prefix: {}", propertyPrefix, accepted);
      if (accepted.size() == addresses.size()) return null;
      return accepted.isEmpty() ? null : accepted.get(0);
    }
    return null;
  }
}
