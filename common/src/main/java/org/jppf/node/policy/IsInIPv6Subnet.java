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
package org.jppf.node.policy;

import java.util.Collection;

import org.jppf.net.IPv6AddressNetmask;

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
public class IsInIPv6Subnet extends AbstractIsInIPSubnet<IPv6AddressNetmask> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

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
    super(subnets);
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
    super(subnets);
  }

  @Override
  IPv6AddressNetmask createNetmask(final String pattern) {
    return new IPv6AddressNetmask(pattern);
  }

  @Override
  IPType getIPType() {
    return IPType.IPV6;
  }
}
