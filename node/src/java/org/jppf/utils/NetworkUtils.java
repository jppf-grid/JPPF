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

import java.net.*;
import java.util.*;

import org.slf4j.*;

/**
 * Utility class that provides method to discover the network configuration of the current machine.
 * @author Laurent Cohen
 */
public final class NetworkUtils
{
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
   * Instantiation opf this class is not permitted.
   */
  private NetworkUtils()
  {
  }

  /**
   * Get the non local (meaning neither localhost or loopback) address of the current host.
   * @return the ipv4 address as a string.
   */
  public static String getNonLocalHostAddress()
  {
    List<InetAddress> allAddresses = getNonLocalIPV4Addresses();
    return allAddresses.isEmpty() ? null : allAddresses.get(0).getHostAddress();
  }

  /**
   * Get a list of all known IP v4 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getIPV4Addresses()
  {
    return getIPAddresses(new InetAddressFilter()
    {
      @Override
      public boolean accepts(final InetAddress addr)
      {
        return addr instanceof Inet4Address;
      }
    });
  }

  /**
   * Get a list of all known non-local IP v4 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPV4Addresses()
  {
    return getIPAddresses(new InetAddressFilter()
    {
      @Override
      public boolean accepts(final InetAddress addr)
      {
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
    return getIPAddresses(new InetAddressFilter()
    {
      @Override
      public boolean accepts(final InetAddress addr)
      {
        return addr instanceof Inet6Address;
      }
    });
  }

  /**
   * Get a list of all known non-local IP v4 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPV6Addresses()
  {
    return getIPAddresses(new InetAddressFilter()
    {
      @Override
      public boolean accepts(final InetAddress addr)
      {
        return (addr instanceof Inet6Address) && !(addr.isLoopbackAddress() || "localhost".equals(addr.getHostName()));
      }
    });
  }

  /**
   * Get a list of all known IP addresses for the current host, according to the specified filter.
   * @param filter filters out unwanted addresses.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  private static List<InetAddress> getIPAddresses(final InetAddressFilter filter)
  {
    List<InetAddress> list = new ArrayList<InetAddress>();
    try
    {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements())
      {
        NetworkInterface ni = interfaces.nextElement();
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        if (debugEnabled && addresses.hasMoreElements()) log.debug("found network interface: " + ni);
        while (addresses.hasMoreElements())
        {
          InetAddress addr = addresses.nextElement();
          if ((filter == null) || filter.accepts(addr)) list.add(addr);
        }
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
    return list;
  }

  /**
   * Get a list of all known non-local IP v4  and v6 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   */
  public static List<InetAddress> getNonLocalIPAddresses()
  {
    List<InetAddress> addresses = new ArrayList<InetAddress>();
    addresses.addAll(getNonLocalIPV4Addresses());
    addresses.addAll(getNonLocalIPV6Addresses());
    return addresses;
  }

  /**
   * Create a set of IPV4 addresses that map to the loopback address.
   * These are all the IPs in the 127.0.0.0/8 range.
   * @return a set of IP addresses as strings.
   */
  private static Set<String> createLoopbackAddresses()
  {
    Set<String> addresses = new HashSet<String>();
    String s = "127.0.0.";
    for (int i=0; i<=8; i++) addresses.add(s + i);
    return addresses;
  }

  /**
   * Get the management host specified in the configuration file.
   * @return the host as a string.
   */
  public static String getManagementHost()
  {
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
  public static String getHostName(final String ip)
  {
    try
    {
      InetAddress a = InetAddress.getByName(ip);
      return a.getHostName();
    }
    catch(Exception e)
    {
      return ip;
    }
  }

  /**
   * Get the subnet mask length of a given address.
   * @param addr the address for which to get the subnet mask.
   * @return the length (number of bits set to 1) for the corresponding subnet mask.
   */
  public static int getSubnetMaskLength(final InetAddress addr)
  {
    try
    {
      NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
      List<InterfaceAddress> intAddresses = ni.getInterfaceAddresses();
      for (InterfaceAddress ia: intAddresses)
      {
        if (addr.equals(ia.getAddress())) return ia.getNetworkPrefixLength();
      }
    }
    catch (Exception e)
    {
      log.error("Error getting subnet mask for address "  + addr, e);
    }
    return 0;
  }

  /**
   * Get a {@link SubnetInformation} object for each non local address of the current host.
   * @return a list of {@link SubnetInformation} instance. This list may be empty, but never null.
   */
  public static List<SubnetInformation> getAllNonLocalSubnetInfo()
  {
    List<SubnetInformation> result = new ArrayList<SubnetInformation>();
    List<InetAddress> addresses = NetworkUtils.getNonLocalIPV4Addresses();
    for (InetAddress addr: addresses)
    {
      int n = getSubnetMaskLength(addr);
      result.add(new SubnetInformation(addr, n));
    }
    return result;
  }

  /**
   * Determine whether the 2 specified IP addresses are on the same subnet.
   * @param si1 the first IP address to compare.
   * @param si2 the second IP address to compare.
   * @return true if the 2 addresses are on the same subnet, false otherwise.
   */
  public static boolean isSameSubnet(final SubnetInformation si1, final SubnetInformation si2)
  {
    if (!(si1.address() instanceof Inet4Address) || !(si2.address() instanceof Inet4Address)) return false;
    int[] ip = { si1.rawIPAsInt(), si2.rawIPAsInt() };
    int[] l = { si1.subnetMaskLength(), si2.subnetMaskLength() };
    int[] mask = { si1.subnetMask(), si2.subnetMask() };
    int[] n = new int[2];
    for (int i=0; i<2; i++) n[i] =  (ip[i] & mask[i]) >> (32-l[i]);
    return n[0] == n[1];
  }

  /**
   * Filter interface for the methods discovering available IP addresses.
   */
  private interface InetAddressFilter
  {
    /**
     * Determine whether the specified address is accepted.
     * @param addr the address to check.
     * @return true if the address is accepted, false otherwise.
     */
    boolean accepts(InetAddress addr);
  }

  /**
   * A pair grouping an {@link InetAddress} and the corresponding subnet mask length.
   */
  public static class SubnetInformation extends Pair<InetAddress, Integer>
  {
    /**
     * Initialize this pair with the specified InetAddress and subnet mask length.
     * @param addr the address.
     * @param subnetMaskLength the subnet mask length.
     */
    public SubnetInformation(final InetAddress addr, final Integer subnetMaskLength)
    {
      super(addr, subnetMaskLength);
    }

    /**
     * Get the internet address.
     * @return an {@link InetAddress} instance.
     */
    public InetAddress address()
    {
      return first();
    }

    /**
     * Get the subnet mask length.
     * @return the subnet mask length as an integer.
     */
    public Integer subnetMaskLength()
    {
      return second();
    }

    /**
     * Convert an {@link Inet4Address} to a raw IP value.
     * @return the IP as a single int value.
     */
    public int rawIPAsInt()
    {
      byte[] ip = address().getAddress();
      int result = ip[0] << 24;
      result &= ip[1] << 16;
      result &= ip[2] << 8;
      result &= ip[3];
      return result;
    }

    /**
     * Get the subnet mask for this subnet information.
     * @return the subnet mask as an int value.
     */
    public int subnetMask()
    {
      int length = subnetMaskLength();
      int mask = (length == 0) ? 0 : 1;
      for (int i=0; i<32; i++) mask = (mask << 1) & ((i < length) ? 1 : 0);
      return mask;
    }
  }

  /**
   * Main entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    System.out.println("This host's ip addresses: " + getNonLocalHostAddress());
  }
}
