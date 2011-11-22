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

package org.jppf.net;

import java.util.regex.Pattern;

import org.jppf.utils.Range;

/**
 * Instances of this class represent the configuration parameters for a specific IP address pattern impelmentation. 
 * @author Laurent Cohen
 */
final class PatternConfiguration
{
	/**
	 * Configuration for IPv6 address patterns.
	 */
	static final PatternConfiguration IPV4_CONFIGURATION = new PatternConfiguration(4, 0, 255, '.', "");
	/**
	 * Configuration for IPv6 address patterns.
	 */
	static final PatternConfiguration IPV6_CONFIGURATION = new PatternConfiguration(8, 0, 0xffff, ':', "0x");
	/**
	 * Regex pattern that matches any sequence of one or more spaces.
	 */
	static final Pattern SPACES_PATTERN = Pattern.compile("\\s+");
	/**
	 * Regex pattern that matches any one minus sign.
	 */
	static final Pattern MINUS_PATTERN = Pattern.compile("-");
	/**
	 * The separator for the components of an address.
	 */
	final char compSeparator;
	/**
	 * Regex pattern that matches any one dot.
	 */
	final Pattern compSeparatorPattern;
	/**
	 * Constant representing the [minValue,maxValue] range.
	 */
	final Range<Integer> fullRange;
	/**
	 * Number of components in the address.
	 */
	final int nbComponents;
	/**
	 * Minimum value of a component.
	 */
	final int minValue;
	/**
	 * Maximum value of a component.
	 */
	final int maxValue;
	/**
	 * The prefix indicating in which base the numbers are represented, i.e. "" for decimal, "0x" for hexadecimal.
	 */
	final String valuePrefix;

	/**
	 * Initialize this pattern configuration with the specified values.
	 * @param nbComponents the number of components in the address.
	 * @param minValue the minimum value of a component.
	 * @param maxValue the maximum value of a component.
	 * @param compSeparator the separator for the components of an address.
	 * @param valuePrefix the prefix indicating in which base the numbers are represented, i.e. "" for decimal, "0X" for hexadecimal.
	 */
	private PatternConfiguration(int nbComponents, int minValue, int maxValue, char compSeparator, String valuePrefix)
	{
		this.nbComponents = nbComponents;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.compSeparator = compSeparator;
		fullRange = new Range<Integer>(minValue, maxValue);
		compSeparatorPattern = Pattern.compile((compSeparator == '.' ? "\\" : "") + compSeparator);
		this.valuePrefix = valuePrefix;
	}
}
