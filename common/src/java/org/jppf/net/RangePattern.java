/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import static org.jppf.utils.StringUtils.build;

import java.util.*;

import org.jppf.utils.*;

/**
 * Represents a pattern used for IP addresses inclusion or exclusion lists.<br/>
 * A pattern represents a single value or a range of values for each component of an IP address.<br/>
 * @author Laurent Cohen
 */
public class RangePattern {
  /**
   * The list of ranges constituting this address pattern.
   */
  protected List<Range<Integer>> ranges = new ArrayList<>();
  /**
   * The configuration used for this pattern.
   */
  protected PatternConfiguration config = null;

  /**
   * Initialize this object with the specified string pattern.
   * @param source the source pattern as a string.
   * @param config the configuration used for this pattern.
   * @throws IllegalArgumentException if the pattern is null or invalid.
   */
  public RangePattern(final String source, final PatternConfiguration config) throws IllegalArgumentException {
    this.config = config;
    convertSource(source);
  }

  /**
   * Convert the specified source into an IP pattern.
   * @param source the source pattern as a string.
   * @throws IllegalArgumentException if the pattern is null or invalid.
   */
  protected void convertSource(final String source) throws IllegalArgumentException {
    if (source == null) throw new IllegalArgumentException("pattern cannot be null");
    String src = preProcess(source);
    src = RegexUtils.SPACES_PATTERN.matcher(src).replaceAll("");
    src = postProcess(src);
    String[] rangeArray = config.compSeparatorPattern.split(src);
    if ((rangeArray == null) || (rangeArray.length == 0)) throw new IllegalArgumentException("invalid empty pattern");
    if (rangeArray.length > config.nbComponents) throw new IllegalArgumentException("pattern describes more than " + config.nbComponents + " components : \"" + source + '\"');
    try {
      for (String s : rangeArray)
        ranges.add(parseRangePattern(s));
      if (rangeArray.length < config.nbComponents) {
        for (int i = rangeArray.length; i < config.nbComponents; i++)
          ranges.add(config.fullRange);
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(build("error in pattern \"", source, "\" : ", e.getMessage()));
    }
  }

  /**
   * Perform pre-processing of the source string before applying common transformations.
   * @param source the pattern source to process.
   * @return a new processed string.
   */
  protected String preProcess(final String source) {
    return source;
  }

  /**
   * Perform post-processing of the source string before applying common transformations.
   * @param source the pattern source to process.
   * @return a new processed string.
   */
  protected String postProcess(final String source) {
    return source;
  }

  /**
   * Determine whether the specified IP address matches this pattern.
   * No check is made to verify that the IP address is valid.
   * @param values the ip address to match as a array of values representing its components.
   * @return true if the address matches this pattern, false otherwise.
   */
  public boolean matches(final int... values) {
    try {
      if ((values == null) || (values.length != ranges.size())) return false;
      for (int i = 0; i < values.length; i++) {
        if (!ranges.get(i).isValueInRange(values[i])) return false;
      }
    } catch (@SuppressWarnings("unused") Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Parse the specified string into a <code>Range</code> object.
   * @param src the range pattern string to parse.
   * @return a <code>Range</code> instance, or null if the pattern is invalid.
   * @throws IllegalArgumentException if the pattern is invalid.
   */
  private Range<Integer> parseRangePattern(final String src) throws IllegalArgumentException {
    if ((src == null) || "".equals(src)) return config.fullRange;
    if (src.indexOf('-') < 0) return new Range<>(parseValue(src));
    String[] vals = RegexUtils.MINUS_PATTERN.split(src);
    if ((vals == null) || vals.length == 0) return config.fullRange;
    if (vals.length > 2) throw new IllegalArgumentException(build("invalid range pattern (pattern: ", src, ")"));
    int lower = 0;
    int upper = 0;
    if (vals.length == 1) {
      if (src.startsWith("-")) {
        lower = config.minValue;
        upper = parseValue(vals[0]);
      } else {
        lower = parseValue(vals[0]);
        upper = config.maxValue;
      }
    } else {
      lower = "".equals(vals[0]) ? config.minValue : parseValue(vals[0]);
      upper = "".equals(vals[1]) ? config.maxValue : parseValue(vals[1]);
    }
    if (upper < lower) throw new IllegalArgumentException(build("lower bound must be <= upper bound (pattern: ", src, ")"));
    return new Range<>(lower, upper);
  }

  /**
   * Parse the specified string into an int value.
   * @param src the string to parse.
   * @return the value as an int
   * @throws IllegalArgumentException if the string is not a valid number format or the value is out of allowed bounds.
   */
  private int parseValue(final String src) throws IllegalArgumentException {
    try {
      int value = Integer.decode(config.valuePrefix + src.toLowerCase());
      if ((value < config.minValue) || (value > config.maxValue))
        throw new IllegalArgumentException(build("value must be in [", config.minValue, " ... ", config.maxValue, "] range (value: ", src, ")"));
      return value;
    } catch (@SuppressWarnings("unused") NumberFormatException e) {
      throw new IllegalArgumentException(build("invalid value format (value: ", src, ")"));
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ranges.size(); i++) {
      if (i > 0) sb.append(config.getCompSeparator());
      sb.append(ranges.get(i));
    }
    return sb.toString();
  }
}
