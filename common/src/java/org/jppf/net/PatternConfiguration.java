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

package org.jppf.net;

import java.util.regex.Pattern;

import org.jppf.utils.Range;

/**
 * Instances of this class represent the configuration parameters for a specific IP address pattern implementation.
 * @author Laurent Cohen
 */
public final class PatternConfiguration
{
  /**
   * Configuration for IPv4 address patterns.
   */
  public static final PatternConfiguration IPV4_CONFIGURATION = new PatternConfiguration(4, 0, 255, '.', "");
  /**
   * Configuration for IPv6 address patterns.
   */
  public static final PatternConfiguration IPV6_CONFIGURATION = new PatternConfiguration(8, 0, 0xffff, ':', "0x");
  /**
   * The separator for the components of an address.
   */
  private final char compSeparator;
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
  public PatternConfiguration(final int nbComponents, final int minValue, final int maxValue, final char compSeparator, final String valuePrefix)
  {
    this.nbComponents = nbComponents;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.compSeparator = compSeparator;
    fullRange = new Range<>(minValue, maxValue);
    compSeparatorPattern = Pattern.compile((compSeparator == '.' ? "\\" : "") + compSeparator);
    this.valuePrefix = valuePrefix;
  }

  /**
   * Get the separator for the components of an address.
   * @return the separator character.
   */
  protected char getCompSeparator()
  {
    return compSeparator;
  }
}
