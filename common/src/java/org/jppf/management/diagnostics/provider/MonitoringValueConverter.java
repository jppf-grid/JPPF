/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.management.diagnostics.provider;

import java.text.*;
import java.util.Locale;

/**
 * Interface for converting monitored data values to displayable strings.
 * @author Laurent Cohen
 */
public interface MonitoringValueConverter {
  /**
   * Convert or format the specified value expressed as a string.
   * @param value the value to convert/format.
   * @return a string resulting from the conversion.
   */
  String convert(String value);

  /**
   * Specialized converter interface to transform {@code int} values to strings.
   */
  public interface IntConverter extends MonitoringValueConverter {
    @Override
    default String convert(String value) {
      return convert(Integer.valueOf(value));
    }

    /**
     * Convert or format the specified value expressed as an {@code int}.
     * @param value the value to convert/format.
     * @return a string resulting from the conversion.
     */
    String convert(Integer value);
  }

  /**
   * Specialized converter interface to transform {@code long} values to strings.
   */
  public interface LongConverter extends MonitoringValueConverter {
    @Override
    default String convert(String value) {
      return convert(Long.valueOf(value));
    }

    /**
     * Convert or format the specified value expressed as a {@code long}.
     * @param value the value to convert/format.
     * @return a string resulting from the conversion.
     */
    String convert(Long value);
  }

  /**
   * Specialized converter interface to transform {@code float} values to strings.
   */
  public interface FloatConverter extends MonitoringValueConverter {
    @Override
    default String convert(String value) {
      return convert(Float.valueOf(value));
    }

    /**
     * Convert or format the specified value expressed as a {@code float}.
     * @param value the value to convert/format.
     * @return a string resulting from the conversion.
     */
    String convert(Float value);
  }

  /**
   * Specialized converter interface to transform {@code double} values to strings.
   */
  public interface DoubleConverter extends MonitoringValueConverter {
    @Override
    default String convert(String value) {
      return convert(Double.valueOf(value));
    }

    /**
     * Convert or format the specified value expressed as a {@code double}.
     * @param value the value to convert/format.
     * @return a string resulting from the conversion.
     */
    String convert(Double value);
  }

  /**
   * Specialized converter to transform {@code double} values to a string with a specified number of fraction digits.
   */
  public static class DoubleConverterWithFractionDigits implements DoubleConverter {
    /**
     * Used to format the numnber.
     */
    private final NumberFormat nf;
    /**
     * Suffix after the number, e.g. " %".
     */
    private final String suffix;

    /**
     * Create a converter producing values with a specified number of fraction digits.
     * @param nbDecimals number of digits after the decimal point.
     */
    public DoubleConverterWithFractionDigits(final int nbDecimals) {
      this(nbDecimals, null);
    }

    /**
     * Create a converter producing values with a specified number of fraction digits and a specified suffix.
     * @param nbDecimals number of digits after the decimal point.
     * @param suffix suffix appended to the formatted number, e.g. " %". May be {@code null}.
     */
    public DoubleConverterWithFractionDigits(final int nbDecimals, final String suffix) {
      nf = DecimalFormat.getIntegerInstance(Locale.getDefault());
      nf.setGroupingUsed(true);
      nf.setMinimumFractionDigits(nbDecimals);
      nf.setMaximumFractionDigits(nbDecimals);
      this.suffix = suffix;
    }
    
    @Override
    public String convert(final Double value) {
      final String s = nf.format(value);
      return (suffix == null) ? s : s + suffix;
    }
  }
}
