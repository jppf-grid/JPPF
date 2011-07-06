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
public class IPFilter
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(IPFilter.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * List of accepted IP adress patterns.
	 */
	private List<AbstractIPAddressPattern> includePatterns = new ArrayList<AbstractIPAddressPattern>();
	/**
	 * List of rejected IP adress patterns.
	 */
	private List<AbstractIPAddressPattern> excludePatterns = new ArrayList<AbstractIPAddressPattern>();
	/**
	 * The loaded configuration.
	 */
	private TypedProperties config = null;

	/**
	 * Initialize this filter with the specified configuration.
	 * @param config the configuration from which to get the include and exclude filters.
	 */
	public IPFilter(TypedProperties config)
	{
		this.config = (config == null) ? new TypedProperties() : config;
		configure();
	}

	/**
	 * Configure this IP filter from the JPPF configuration.
	 */
	public void configure()
	{
		configureIPAddressPatterns(config.getString("jppf.discovery.include.ipv4"), includePatterns);
		configureIPAddressPatterns(config.getString("jppf.discovery.include.ipv6"), includePatterns);
		configureIPAddressPatterns(config.getString("jppf.discovery.exclude.ipv4"), excludePatterns);
		configureIPAddressPatterns(config.getString("jppf.discovery.exclude.ipv6"), excludePatterns);
	}

	/**
	 * Parse the IP address patterns specified in the source string. 
	 * @param source contains comma or semicomlumn separated patterns.
	 * @param addToList the list to add the parsed patterns to.
	 */
	private void configureIPAddressPatterns(String source, List<AbstractIPAddressPattern> addToList)
	{
		if (source == null) return;
		source = source.trim();
		if ("".equals(source)) return;
		String[] p = source.split(",|;");
		if ((p == null) || (p.length == 0)) return;
		for (String s: p)
		{
			try
			{
				addToList.add(new IPv4AddressPattern(s));
			}
			catch (Exception e)
			{
				log.warn(e.getMessage());
			}
		}
	}

	/**
	 * Determine whether an ip address passes the include and exclude filters.
	 * @param ip the ip to check.
	 * @return true if the address passes the filters, false otherwise.
	 */
	public boolean isAddressAccepted(InetAddress ip)
	{
		int[] ipComps = StringUtils.toIntArray(ip);
		boolean included = matches(ipComps, includePatterns, true);
		boolean excluded = matches(ipComps, excludePatterns, false);
		return included && !excluded;
	}

	/**
	 * Determine whether the specified raw IP address matches one of the specified filters.
	 * @param ipComps the IP address to check.
	 * @param patterns the IP list of patterns to check the address against.
	 * @param defIfEmpty the value to return if the list of patterns is empty.
	 * @return <code>true</code> if the IP address matches one of the filterz, <code>false</code> otherwise.
	 */
	private boolean matches(int[] ipComps, List<AbstractIPAddressPattern> patterns, boolean defIfEmpty)
	{
		if ((patterns == null) || patterns.isEmpty()) return defIfEmpty;
		for (AbstractIPAddressPattern p: patterns)
		{
			if (p.matches(ipComps)) return true;
		}
		return false;
	}
}
