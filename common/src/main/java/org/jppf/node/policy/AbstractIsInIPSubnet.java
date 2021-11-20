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

import java.net.*;
import java.util.*;

import org.jppf.JPPFRuntimeException;
import org.jppf.net.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Common abstract super class for execution policy rules that encapsulate a test of type <i>IP is in Subnet string</i>.
 * @param <P> the type of pattern handled by this class.
 * @author Daniel Widdis
 * @author Laurent Cohen
 */
public abstract class AbstractIsInIPSubnet<P extends AbstractIPAddressPattern> extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractIsInIPSubnet.class);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Name of the nested subnet mask elements used in the XML representation of this policy.
   */
  private static final String SUBNET = "Subnet";
  /**
   * String value(s) to test for subnet membership
   */
  private final List<Expression<String>> subnets;
  /**
   * Cached list of netmasks, lazily computed.
   */
  private transient List<P> netmasks;
  /**
   * Possible address types.
   */
  static enum IPType {
    /**
     * IPv4 address type.
     */
    IPV4("ipv4.addresses", "IsInIPv4Subnet"),
    /**
     * IPv6 address type.
     */
    IPV6("ipv6.addresses", "IsInIPv6Subnet");

    /**
     * Associated property name and xml tag.
     */
    private final String propertyName, xmlTag;
    
    /**
     * @param propertyName the associated property name.
     * @param xmlTag the associated xml tag.
     */
    private IPType(final String propertyName, final String xmlTag) {
      this.propertyName = propertyName;
      this.xmlTag = xmlTag;
    }
  }

  /**
   * Define a membership test using ipv4.addresses property to determine if a
   * node or driver is in one of the specified subnets.
   * @param subnets
   *        One or more strings representing either a valid IP subnet in CIDR
   *        notation or IP address range pattern as defined in
   *        {@link IPv4AddressPattern} or {@link IPv6AddressPattern}
   */
  public AbstractIsInIPSubnet(final String... subnets) {
    this((subnets == null) ? null : Arrays.asList(subnets));
  }

  /**
   * Define a membership test using ipv6.addresses property to determine if a
   * node is in one of the specified subnets.
   * @param subnets
   *        One or more strings representing either a valid IP subnet in CIDR
   *        notation or IP address range pattern as defined in
   *        {@link IPv4AddressPattern} or {@link IPv6AddressPattern}
   */
  public AbstractIsInIPSubnet(final Collection<String> subnets) {
    final Collection<String> checked = checkSubnets(subnets);
    this.subnets = new ArrayList<>(checked.size());
    for (final String subnet: checked) this.subnets.add(new StringExpression(subnet));
  }

  /**
   * Create a new net mask from the specified pattern.
   * @param pattern the netmask pattern as a stirng.
   * @return a new instance of the netmask type.
   */
  abstract P createNetmask(final String pattern);

  /**
   * @return the ip type for this policy.
   */
  abstract IPType getIPType();

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    // Build list of subnet netmasks
    synchronized(this) {
      if (netmasks == null) {
        netmasks = new ArrayList<>(subnets.size());
        for (final Expression<String> subnet : subnets) {
          netmasks.add(createNetmask(subnet.evaluate(info)));
        }
      }
    }
    // Get IP strings from properties
    // Returns as "foo.com|1.2.3.4 bar.org|5.6.7.8"
    final String ip = getProperty(info, getIPType().propertyName);

    for (final HostIP hip: NetworkUtils.parseAddresses(ip)) {
      // Check IP against each subnet
      for (final P netmask : netmasks) {
        try {
          if (netmask.matches(InetAddress.getByName(hip.ipAddress()))) {
            return true;
          }
        } catch (final UnknownHostException e) {
          final String message = "Unknown host '{}' : {}";
          if (traceEnabled) log.trace(message, hip.ipAddress(), ExceptionUtils.getStackTrace(e));
          else log.warn(message, hip.ipAddress(), ExceptionUtils.getMessage(e));
        }
      }
    }
    return false;
  }

  @Override
  public String toString(final int n) {
    final StringBuilder sb = new StringBuilder(indent(n)).append(tagStart(getIPType().xmlTag)).append('\n');
    for (Expression<String> subnet: subnets) sb.append(indent(n + 1)).append(xmlElement(SUBNET, subnet.getExpression())).append('\n');
    sb.append(indent(n)).append(tagEnd(getIPType().xmlTag)).append('\n');
    return sb.toString();
  }

  /**
   * Check that the specified subnets array contains at least one non-null string.
   * @param subnets the subnet arguments to check.
   * @return an array made of all the non-null subnets.
   * @throws JPPFRuntimeException if there is no non-null subnet.
   */
  private List<String> checkSubnets(final Collection<String> subnets) throws JPPFRuntimeException {
    if ((subnets != null) && (subnets.size() > 0)) {
      final List<String> result = new ArrayList<>();
      for (final String subnet: subnets) {
        if (subnet != null) result.add(subnet);
      }
      if (result.size() > 0) return result;
    }
    final String message = String.format("the execution policy rule '%s' must have at least one non-null subnet", getClass().getSimpleName());
    throw new JPPFRuntimeException(message);
  }
}
