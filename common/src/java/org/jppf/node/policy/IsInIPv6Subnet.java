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
package org.jppf.node.policy;

import java.net.*;
import java.util.*;

import org.jppf.net.IPv6AddressNetmask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * An execution policy rule that encapsulates a test of type <i>IPv6 is in Subnet string</i>.
 *
 * <p>This policy has the following XML representation:
 * <pre>&lt;IsInIPv6Subnet&gt;
 *   &lt;Subnet&gt;subnet-1&lt;/Subnet&gt;
 *   ...
 *   &lt;Subnet&gt;subnet-N&lt;/Subnet&gt;
 * &lt;/IsInIPv6Subnet&gt;</pre>
 * where each <i>subnet-i</i> is a subnet expressed either in <a href="http://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing">CIDR</a> notation
 * or in the representation described in {@link org.jppf.net.IPv6AddressPattern IPv6AddressPattern}.
 * 
 * @author Daniel Widdis
 * @since 4.2
 */
public class IsInIPv6Subnet extends ExecutionPolicy {

  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(IsInIPv6Subnet.class);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Name of the tag used in the XML representation of this policy.
   */
  private static final String TAG = IsInIPv6Subnet.class.getSimpleName();
  /**
   * Name of the nested subnet mask elements used in the XML representation of this policy.
   */
  private static final String SUBNET = "Subnet";
  /**
   * String value(s) to test for subnet membership
   */
  private String[] subnets = null;
  /**
   * Cached list of netmasks, lazily computed.
   */
  private transient List<IPv6AddressNetmask> netmasks;

  /**
   * Define a membership test using ipv6.addresses property to determine if a
   * node or driver is in one of the specified subnets.
   *
   * @param subnets
   *        One or more strings representing either a valid IPv6 subnet in CIDR
   *        notation or IP address range pattern without double colons ("::") as
   *        defined in {@link org.jppf.net.IPv6AddressPattern IPv6AddressPattern}
   */
  public IsInIPv6Subnet(final String... subnets) {
    if ((subnets == null) || (subnets.length <= 0)) throw new IllegalArgumentException("at least one IPv6 subnet must be specified");
    this.subnets = subnets;
  }

  /**
   * Define a membership test using ipv6.addresses property to determine if a
   * node is in one of the specified subnets.
   *
   * @param subnets
   *        One or more strings representing either a valid IPv6 subnet in CIDR
   *        notation or IP address range pattern without double colons ("::") as
   *        defined in {@link org.jppf.net.IPv6AddressPattern IPv6AddressPattern}
   */
  public IsInIPv6Subnet(final Collection<String> subnets) {
    if ((subnets == null) || (subnets.size() <= 0)) throw new IllegalArgumentException("at least one IPv6 subnet must be specified");
    this.subnets = subnets.toArray(new String[subnets.size()]);
  }

  /**
   * Determines whether this policy accepts the specified node.
   *
   * @param info
   *        system information for the node on which the tasks will run if
   *        accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    // Build list of subnet netmasks
    synchronized(this) {
      if (netmasks == null) {
        netmasks = new ArrayList<>(subnets.length);
        for (String subnet : subnets) {
          netmasks.add(new IPv6AddressNetmask(subnet));
        }
      }
    }
    // Get IP strings from properties
    // Returns as "localhost|::1 foo.com|1080::8:800:200C:417A"
    String ipv6 = getProperty(info, "ipv6.addresses");

    // Iterate and check if any are in subnet
    for (HostIP hip: NetworkUtils.parseAddresses(ipv6)) {
      // Check IP against each subnet
      for (IPv6AddressNetmask netmask : netmasks) {
        try {
          if (netmask.matches(InetAddress.getByName(hip.ipAddress()))) {
            return true;
          }
        } catch (UnknownHostException e) {
          String message = "Unknown host '{}' : {}";
          if (traceEnabled) log.trace(message, hip.ipAddress(), ExceptionUtils.getStackTrace(e));
          else log.warn(message, hip.ipAddress(), ExceptionUtils.getMessage(e));
        }
      }
    }
    return false;
  }

  /**
   * Print this object to a string.
   * @return an XML string representation of this object
   */
  @Override
  public String toString() {
    if (computedToString == null) {
      synchronized(ExecutionPolicy.class) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append(tagStart(TAG)).append('\n');
        toStringIndent++;
        for (String subnet: subnets) sb.append(indent()).append(xmlElement(SUBNET, subnet)).append('\n');
        toStringIndent--;
        sb.append(indent()).append(tagEnd(TAG)).append('\n');
        computedToString = sb.toString();
      }
    }
    return computedToString;
  }
}
