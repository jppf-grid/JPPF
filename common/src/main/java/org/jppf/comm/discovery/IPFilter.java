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

package org.jppf.comm.discovery;

import java.net.InetAddress;
import java.util.*;

import org.jppf.net.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class handle IP address inclusion and exclusion filters constructed from the JPPF configuration.
 * @see org.jppf.net.IPv4AddressPattern
 * @author Laurent Cohen
 */
public class IPFilter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(IPFilter.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * List of accepted IP address patterns.
   */
  private final List<AbstractIPAddressPattern> includePatterns = new ArrayList<>();
  /**
   * List of rejected IP address patterns.
   */
  private final List<AbstractIPAddressPattern> excludePatterns = new ArrayList<>();
  /**
   * The loaded configuration.
   */
  private TypedProperties config = null;
  /**
   * Determines whether this filter applies to the driver's broadcaster or to a client's/node's receiver.
   */
  private final boolean broadcaster;

  /**
   * Initialize this filter with the specified configuration.
   * @param config the configuration from which to get the include and exclude filters.
   */
  public IPFilter(final TypedProperties config) {
    this(config, false);
  }

  /**
   * Initialize this filter with the specified configuration.
   * @param config the configuration from which to get the include and exclude filters.
   * @param broadcaster specifies whether this filter applies to the driver's broadcaster or to a client's/node's receiver.
   */
  public IPFilter(final TypedProperties config, final boolean broadcaster) {
    this.config = (config == null) ? new TypedProperties() : config;
    this.broadcaster = broadcaster;
    configure();
  }

  /**
   * Initialize this filter with the specified configuration.
   * @param config the configuration from which to get the include and exclude filters.
   * @param prefix the confiugration properties prefix to use.
   */
  public IPFilter(final TypedProperties config, final String prefix) {
    this.config = (config == null) ? new TypedProperties() : config;
    this.broadcaster = false;
    configure(prefix);
  }

  /**
   * Configure this IP filter from the JPPF configuration.
   */
  public void configure() {
    configure("jppf.discovery." + (broadcaster ? "broadcast." : ""));
  }

  /**
   * Configure this IP filter from the JPPF configuration.
   * @param prefix the confiugration properties prefix to use.
   */
  public void configure(final String prefix) {
    configureIPAddressPatterns(config.getString(prefix + "include.ipv4"), true, true);
    configureIPAddressPatterns(config.getString(prefix + "include.ipv6"), false, true);
    configureIPAddressPatterns(config.getString(prefix + "exclude.ipv4"), true, false);
    configureIPAddressPatterns(config.getString(prefix + "exclude.ipv6"), false, false);
  }

  /**
   * Parse the IP address patterns specified in the source string.
   * @param source contains comma or semicolumn separated patterns.
   * @param ipv4 specifiies whether the specified patterns apply to ipv4 addresses.
   * @param inclusive whether the specified patterns are inclusive patterns or not.
   */
  private void configureIPAddressPatterns(final String source, final boolean ipv4, final boolean inclusive) {
    if (source == null) return;
    final String src = source.trim();
    if ("".equals(src)) return;
    final String[] p = RegexUtils.COMMA_OR_SEMICOLUMN_PATTERN.split(src);
    if ((p == null) || (p.length == 0)) return;
    for (final String s: p) {
      try {
        final AbstractIPAddressPattern pattern = ipv4 ? new IPv4AddressNetmask(s) : new IPv6AddressNetmask(s);
        if (inclusive) includePatterns.add(pattern);
        else excludePatterns.add(pattern);
        if (debugEnabled) log.debug("added pattern {}, inclusive={}", pattern, inclusive);
      } catch (final Exception e) {
        log.warn("invalid pattern in '[}' : {}", source, ExceptionUtils.getMessage(e));
      }
    }
  }

  /**
   * Determine whether an ip address passes the include and exclude filters.
   * @param ip the ip to check.
   * @return true if the address passes the filters, false otherwise.
   */
  public boolean isAddressAccepted(final InetAddress ip) {
    final int[] ipComps = NetworkUtils.toIntArray(ip);
    final boolean included = matches(ipComps, includePatterns, true);
    final boolean excluded = matches(ipComps, excludePatterns, false);
    return included && !excluded;
  }

  /**
   * Determine whether the specified raw IP address matches one of the specified filters.
   * @param ipComps the IP address to check.
   * @param patterns the IP list of patterns to check the address against.
   * @param defIfEmpty the value to return if the list of patterns is empty.
   * @return <code>true</code> if the IP address matches one of the filter, <code>false</code> otherwise.
   */
  private static boolean matches(final int[] ipComps, final List<AbstractIPAddressPattern> patterns, final boolean defIfEmpty) {
    if ((patterns == null) || patterns.isEmpty()) return defIfEmpty;
    for (AbstractIPAddressPattern p: patterns) {
      if (p.matches(ipComps)) return true;
    }
    return false;
  }

  /**
   * @return whether there is at least one pattern.
   */
  public boolean hasPattern() {
    return !includePatterns.isEmpty() || !excludePatterns.isEmpty();
  }
}
