/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package sample;

import java.net.*;
import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NetworkHelper
{
  /**
   * 
   * @param args not used
   */
  public static void main(final String[] args) {
    try {
      List<InetAddress> list = getNonLocalIPV4Addresses();
      if (list.size() > 0) System.out.println("current node address is " + list.get(0));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  /**
   * Get a list of all known non-local IP v4 addresses for the current host.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   * @throws Exception if any error occurs.
   */
  public static List<InetAddress> getNonLocalIPV4Addresses() throws Exception {
    return getIPAddresses(new InetAddressFilter() {
      @Override
      public boolean accepts(final InetAddress addr) {
        return (addr instanceof Inet4Address)
            && !(addr.getHostAddress().startsWith("127.0.0.") || "localhost".equals(addr.getHostName()));
      }
    });
  }

  /**
   * Get a list of all known IP addresses for the current host, according to the specified filter.
   * @param filter filters out unwanted addresses.
   * @return a List of <code>InetAddress</code> instances, may be empty but never null.
   * @throws Exception if any error occurs.
   */
  public static List<InetAddress> getIPAddresses(final InetAddressFilter filter) throws Exception {
    List<InetAddress> list = new ArrayList<>();
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface ni = interfaces.nextElement();
      Enumeration<InetAddress> addresses = ni.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress addr = addresses.nextElement();
        if ((filter == null) || filter.accepts(addr)) list.add(addr);
      }
    }
    return list;
  }

  /**
   * Filter interface for the methods discovering available IP addresses.
   */
  public interface InetAddressFilter {
    /**
     * Determine whether the specified address is accepted.
     * @param addr the address to check.
     * @return true if the address is accepted, false otherwise.
     */
    boolean accepts(InetAddress addr);
  }
}
