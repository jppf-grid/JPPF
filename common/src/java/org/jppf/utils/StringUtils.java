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
package org.jppf.utils;

import java.net.*;
import java.nio.charset.Charset;
import java.text.*;
import java.util.*;


/**
 * This class provides a set of utility methods for manipulating strings.
 * @author Laurent Cohen
 */
public final class StringUtils {
  /**
   * Logger for this class.
   */
  //private static Logger log = LoggerFactory.getLogger(StringUtils.class);
  /**
   * Charset instance for UTF-8 encoding.
   */
  public static final Charset UTF_8 = makeUTF8();
  /**
   * Constant for an empty array of URLs.
   */
  public static final String[] ZERO_STRING = new String[0];
  /**
   * Constant for an empty array of Objects.
   */
  public static final Object[] ZERO_OBJECT = new Object[0];
  /**
   * Constant for an empty array of URLs.
   */
  public static final URL[] ZERO_URL = new URL[0];
  /**
   * Constant for an empty array of ints.
   */
  public static final int[] ZERO_INT = new int[0];
  /**
   * An array of char containing the hex digits in ascending order.
   */
  private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

  /**
   * Instantiation of this class is not permitted.
   */
  private StringUtils() {
  }

  /**
   * Format a string so that it fits into a string of specified length.<br>
   * If the string is longer than the specified length, then characters on the left are truncated, otherwise
   * the specified character is appended to the result on the left  to obtain the appropriate length.
   * @param source the string to format; if null, it is considered an empty string.
   * @param padChar the character used to fill the result up to the specified length.
   * @param maxLen the length of the formatted string.
   * @return a string formatted to the specified length.
   */
  public static String padLeft(final String source, final char padChar, final int maxLen) {
    String src = (source == null) ? "" : source;
    int length = src.length();
    if (length > maxLen) return source;
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<maxLen-length; i++) sb.append(padChar);
    sb.append(src);
    return sb.toString();
  }

  /**
   * Format a string so that it fits into a string of specified length.<br>
   * If the string is longer than the specified length, then characters on the left are truncated, otherwise
   * the specified character is appended to the result on the left  to obtain the appropriate length.
   * @param source the string to format; if null, it is considered an empty string.
   * @param padChar the character used to fill the result up to the specified length.
   * @param maxLen the length of the formatted string.
   * @param truncate if <code>true</code>, then truncate the string if its length is greater than <code>maxLen</code>.
   * @return a string formatted to the specified length.
   */
  public static String padLeft(final String source, final char padChar, final int maxLen, final boolean truncate) {
    String src = (source == null) ? "" : source;
    int length = src.length();
    if (length > maxLen) return source;
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<maxLen-length; i++) sb.append(padChar);
    sb.append(src);
    return sb.toString();
  }

  /**
   * Pads a string on the right side with a given character
   * If the string is longer than the specified length, then characters on the right are truncated, otherwise
   * the specified character is appended to the result on the right  to obtain the appropriate length.
   * @param source the string to pad to the right
   * @param padChar the character used for padding
   * @param maxLen the length to pad the string up to
   * if its length is greater than the padding length
   * @return the padded (or truncated) string
   */
  public static String padRight(final String source, final char padChar, final int maxLen) {
    return padRight(source, padChar, maxLen, true);
  }

  /**
   * Pads a string on the right side with a given character
   * If the string is longer than the specified length, then characters on the right are truncated, otherwise
   * the specified character is appended to the result on the right  to obtain the appropriate length.
   * @param source the string to pad to the right
   * @param padChar the character used for padding
   * @param maxLen the length to pad the string up to
   * if its length is greater than the padding length
   * @param truncate if <code>true</code>, then truncate the string if its length is greater than <code>maxLen</code>.
   * @return the padded (or truncated) string
   */
  public static String padRight(final String source, final char padChar, final int maxLen, final boolean truncate) {
    String s = source;
    if (s == null) s = "";
    if (s.length() > maxLen) return truncate ? s = s.substring(0, maxLen) : s;
    StringBuilder sb = new StringBuilder(s);
    while (sb.length() < maxLen) sb.append(padChar);
    return sb.toString();
  }

  /**
   * Convert an array of bytes into a string of hexadecimal numbers.<br>
   * @param bytes the array that contains the sequence of byte values to convert.
   * @return the converted bytes as a string of space-separated hexadecimal numbers.
   */
  public static String toHexString(final byte[] bytes) {
    return toHexString(bytes, 0, bytes.length, null);
  }

  /**
   * Convert a part of an array of bytes, into a string of hexadecimal numbers.
   * The hex numbers may or may not be separated, depending on the value of the <code>sep</code> parameter.<br>
   * @param bytes the array that contains the sequence of byte values to convert.
   * @param start the index to start at in the byte array.
   * @param length the number of bytes to convert in the array.
   * @param sep the separator between hexadecimal numbers in the resulting string. If null, then no separator is used.
   * @return the converted bytes as a string of space-separated hexadecimal numbers.
   */
  public static String toHexString(final byte[] bytes, final int start, final int length, final String sep) {
    StringBuilder sb = new StringBuilder();
    if (length >= 0) {
      boolean sepNotNull = sep != null;
      for (int i=start; i<Math.min(bytes.length, start+length); i++) {
        if (sepNotNull && (i > start)) sb.append(sep);
        byte b = bytes[i];
        sb.append(HEX_DIGITS[(b & 0xF0) >> 4]);
        sb.append(HEX_DIGITS[b & 0x0F]);
      }
    }
    return sb.toString();
  }

  /**
   * Convert a string of space-separated hexadecimal numbers into an array of bytes.
   * @param hexString the string to convert.
   * @return the resulting array of bytes.
   */
  public static byte[] toBytes(final String hexString) {
    String[] bytes = hexString.split("\\s");
    List<Byte> list = new ArrayList<>(bytes.length);
    byte[] result = new byte[list.size()];
    for (int i=0; i<bytes.length; i++) {
      int n = Byte.parseByte(bytes[i].substring(0, 1), 16);
      n = 16 * n + Byte.parseByte(bytes[i].substring(1), 16);
      result[i] = Byte.valueOf((byte) n);
    }
    return result;
  }

  /**
   * Transform a duration in milliseconds into a string with hours, minutes, seconds and milliseconds..
   * @param duration the duration to transform, expressed in milliseconds.
   * @return a string specifying the duration in terms of hours, minutes, seconds and milliseconds.
   */
  public static String toStringDuration(final long duration) {
    long elapsed = duration;
    StringBuilder sb = new StringBuilder();
    sb.append(padLeft(""+(elapsed / 3600000L), '0', 2)).append(':');
    elapsed = elapsed % 3600000L;
    sb.append(padLeft(""+(elapsed / 60000L), '0', 2)).append(':');
    elapsed = elapsed % 60000L;
    sb.append(padLeft(""+(elapsed / 1000L), '0', 2)).append('.');
    sb.append(padLeft(""+(elapsed % 1000L), '0', 3));
    return sb.toString();
  }

  /**
   * Get a String representation of an array of any type.
   * @param <T> the type of the array.
   * @param array the array from which to build a string representation.
   * @return the array's content as a string.
   */
  public static <T> String arrayToString(final T...array) {
    return arrayToString(",", "[", "]", array);
  }

  /**
   * Get a String representation of an array of any type.
   * @param <T> the type of the array.
   * @param array the array from which to build a string representation.
   * @param sep the separator to use for values. If null, no separator is used.
   * @param prefix the prefix to use at the start of the resulting string. If null, no prefix is used.
   * @param suffix the suffix to use at the end of the resulting string. If null, no suffix is used.
   * @return the array's content as a string.
   */
  public static <T> String arrayToString(final String sep, final String prefix, final String suffix, final T...array) {
    StringBuilder sb = new StringBuilder();
    if (array == null) sb.append("null");
    else {
      if (prefix != null) sb.append(prefix);
      for (int i=0; i<array.length; i++) {
        if ((i > 0) && (sep != null)) sb.append(sep);
        sb.append(array[i]);
      }
      if (suffix != null) sb.append(suffix);
    }
    return sb.toString();
  }

  /**
   * Parse an array of port numbers from a string containing a list of space-separated port numbers.
   * @param s list of space-separated port numbers
   * @return an array of int port numbers.
   */
  public static int[] parseIntValues(final String s) {
    String[] strPorts = s.split("\\s");
    int[] ports = new int[strPorts.length];
    for (int i=0; i<strPorts.length; i++) {
      try {
        int n = Integer.valueOf(strPorts[i].trim());
        ports[i] = n;
      } catch(NumberFormatException e) {
        return null;
      }
    }
    return ports;
  }

  /**
   * Convert an array of int values into a space-separated string.
   * @param ports list of port numbers
   * @return a space-separated list of ports.
   */
  public static String buildString(final int[] ports) {
    if ((ports == null) || (ports.length == 0)) return "";
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<ports.length; i++) {
      if (i > 0) sb.append(' ');
      sb.append(ports[i]);
    }
    return sb.toString();
  }

  /**
   * Build a string made of the specified tokens.
   * @param args the tokens composing the string.
   * @return the concatenation of the string values of the tokens.
   */
  public static String build(final Object...args) {
    if (args == null) return null;
    StringBuilder sb = new StringBuilder();
    for (Object o: args) sb.append(o);
    return sb.toString();
  }

  /**
   * Determine whether the specified source string starts with one of the specified values.
   * @param source the string to match with the values.
   * @param ignoreCase specifies whether case should be ignore in the string matching.
   * @param values the values to match the source with.
   * @return true if the source matches one of the values, false otherwise.
   */
  public static boolean startsWithOneOf(final String source, final boolean ignoreCase, final String...values) {
    if ((source == null) || (values == null)) return false;
    String s = ignoreCase ? source.toLowerCase(): source;
    for (String val: values) {
      if (val == null) continue;
      String s2 = ignoreCase ? val.toLowerCase() : val;
      if (s.startsWith(s2)) return true;
    }
    return false;
  }

  /**
   * Determine whether the specified source string is equal to one of the specified values.
   * @param source the string to match with the values.
   * @param ignoreCase specifies whether case should be ignore in the string matching.
   * @param values the values to match the source with.
   * @return true if the source matches one of the values, false otherwise.
   */
  public static boolean isOneOf(final String source, final boolean ignoreCase, final String...values) {
    if ((source == null) || (values == null)) return false;
    String s = ignoreCase ? source.toLowerCase(): source;
    for (String val: values) {
      if (val == null) continue;
      String s2 = ignoreCase ? val.toLowerCase() : val;
      if (s.equals(s2)) return true;
    }
    return false;
  }

  /**
   * Create an instance of the UTF-8 charset.
   * @return a {@link Charset} instance for UTF-8, or null if the charset could not be instantiated.
   */
  private static Charset makeUTF8() {
    try {
      return Charset.forName("UTF-8");
    } catch(Exception e) {
      return null;
    }
  }

  /**
   * Print a top-down representation of a class loader hierarchy into a string.
   * @param leafClassLoader the class loader at the bottom of the hierarchy.
   * @return a string representation of the class loader hierarchy.
   */
  public static String printClassLoaderHierarchy(final ClassLoader leafClassLoader) {
    StringBuilder sb = new StringBuilder();
    ClassLoader cl = leafClassLoader;
    if (cl != null) {
      sb.append("class loader hierarchy:\n");
      Stack<String> stack = new Stack<>();
      while (cl != null) {
        if ("org.jppf.classloader.AbstractJPPFClassLoader".equals(cl.getClass().getName())) stack.push(cl.toString());
        else if (cl instanceof URLClassLoader) stack.push(toString((URLClassLoader) cl));
        else  stack.push(cl.toString());
        cl = cl.getParent();
      }
      int count = 0;
      while (!stack.isEmpty()) {
        for (int i=0; i<2*count; i++) sb.append(' ');
        sb.append(stack.pop());
        if (!stack.isEmpty()) sb.append('\n');
        count++;
      }
    }
    return sb.toString();
  }

  /**
   * Print a representation of a <code>URLClassLoader</code> into a string.
   * The resulting string includes the class loader's classpath.
   * @param cl the classloader to print.
   * @return a string representation of the input class loader.
   */
  public static String toString(final URLClassLoader cl) {
    StringBuilder sb = new StringBuilder();
    sb.append(cl.getClass().getSimpleName()).append("[classpath=");
    URL[] urls = cl.getURLs();
    if ((urls != null) && (urls.length > 0)) {
      for (int i=0; i<urls.length; i++) {
        if (i > 0) sb.append(';');
        sb.append(urls[i]);
      }
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Parse a Number from a String.
   * @param source the string to parse.
   * @param def the default value to return if the source cannot be parsed.
   * @return the source parsed as a number or <code>def</code> if it could not be parsed as a number.
   */
  public static Number parseNumber(final String source, final Number def) {
    if (source == null) return def;
    NumberFormat nf = NumberFormat.getInstance();
    try {
      return nf.parse(source);
    } catch (ParseException ignore) {
    }
    return null;
  }

  /**
   * Return a String in the format &lt;object class name&gt;@hashcode for the specified object.
   * @param obj the object for which to get a string.
   * @return an identity string, or "null" if the object is null.
   */
  public static String toIdentityString(final Object obj) {
    if (obj == null) return "null";
    return obj.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(obj));
  }

  /**
   * Parse the specified source string into a list of strings according to the specified separator.
   * @param source the string to parse.
   * @param separator the delimiter for the resulting strings; it can be a regex.
   * @return a list of strings, possibly empty but never null;
   */
  public static List<String> parseStrings(final String source, final String separator) {
    List<String> list = new ArrayList<>();
    if (source != null) {
      if (separator == null) list.add(source);
      else {
        String[] tokens = source.split(separator);
        for (String token: tokens) {
          if (token == null) continue;
          String s = token.trim();
          if (!s.isEmpty()) list.add(s);
        }
      }
    }
    return list;
  }
}
