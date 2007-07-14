/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.utils;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;


/**
 * This class provides a set of utility methods for manipulating strings. 
 * @author Laurent Cohen
 */
public final class StringUtils
{
	/**
	 * Keywords to look for and replace in the legend items of the charts.
	 */
	private static final String[] KEYWORDS = new String[] { "Execution", "Maximum", "Minimum", "Average" };
	/**
	 * The the replacements words for the keywords in the legend items. Used to shorten the legend labels.
	 */
	private static final String[] REPLACEMENTS = new String[] { "Exec", "Max", "Min", "Avg" };

	/**
	 * Instantiation of this class is not permitted.
	 */
	private StringUtils()
	{
	}

	/**
	 * Format a string so that it fits into a string of specified length.<br>
	 * If the string is longer than the specified length, then characters on the left are truncated, ortherwise
	 * the specified character is appended to the result on the left  to obtain the appropriate length.
	 * @param source the string to format; if null, it is considered an empty string.
	 * @param padChar the character used to fill the result up to the specified length.
	 * @param maxLen the length of the formatted string.
	 * @return a string formatted to the specified length.
	 */
	public static String padLeft(String source, char padChar, int maxLen)
	{
		StringBuilder sb = new StringBuilder();
		if (source == null) source = "";
		int length = source.length();
		if (length > maxLen) sb.append(source, length-maxLen, maxLen);
		else
		{
			for (int i=0; i<maxLen-length; i++) sb.append(padChar);
			sb.append(source);
		}
		return sb.toString();
	}

	/**
	 * Padds a string on the right side with a given character
	 * If the string is longer than the specified length, then characters on the right are truncated, ortherwise
	 * the specified character is appended to the result on the right  to obtain the appropriate length.
	 * @param source the string to pad to the right
	 * @param padChar the character used for padding
	 * @param maxLen the length to pad the string up to
	 * if its length is greater than the padding length
	 * @return the padded (or truncated) string
	 */
	public static String padRight(String source, char padChar, int maxLen)
	{
		String s = source;
		if (s == null) s = "";
		if (s.length() > maxLen) s = s.substring(0, maxLen);
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < maxLen) sb.append(padChar);
		return sb.toString();
	}

	/**
	 * An array of char containing the hex digits in ascending order.
	 */
	private static char[] hexDigits =
		new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	/**
	 * Convert a part of an array of bytes, into a string of space-separated hexadecimal numbers.<br>
	 * This method is proposed as a convenience for debugging purposes.
	 * @param bytes the array that contains the sequence of byte values to convert.
	 * @param start the index to start at in the byte array.
	 * @param length the number of bytes to convert in the array.
	 * @return the cinverted bytes as a string of space-separated hexadecimal numbers.
	 */
	public static String dumpBytes(byte[] bytes, int start, int length)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=start; i<start+length; i++)
		{
			if (i > start) sb.append(' ');
			sb.append(toHexString(bytes[i]));
		}
		return sb.toString();
	}
	
	/**
	 * Convert a byte value into a 2-digits hexadecimal value. The first digit is 0 if the value is less than 16.<br>
	 * If a value is negative, its 2-complement value is converted, otherwise the value itself is converted.
	 * @param b the byte value to convert.
	 * @return a string containing the 2-digit hexadecimal representation of the byte value.
	 */
	public static String toHexString(byte b)
	{
		int n = (b < 0) ? b + 256 : b;
		StringBuilder sb = new StringBuilder();
		sb.append(hexDigits[n / 16]);
		sb.append(hexDigits[n % 16]);
		return sb.toString();
	}
	
	/**
	 * Convert a string of space-separated hexadecimal numbers into an array of bytes.
	 * @param hexString the string to convert.
	 * @return the resulting array of bytes.
	 */
	public static byte[] toBytes(String hexString)
	{
		List<Byte> list = new ArrayList<Byte>();
		String[] bytes = hexString.split("\\s");
		for (String bStr: bytes)
		{
			int n = Byte.parseByte(bStr.substring(0, 1), 16);
			n = 16 * n + Byte.parseByte(bStr.substring(1), 16);
			list.add(new Byte((byte) n));
		}
		byte[] result = new byte[list.size()];
		for (int i=0; i<list.size(); i++) result[i] = list.get(i);
		return result;
	}
	
	/**
	 * Tranform a duration in milliseconds into a string with hours, minutes, seconds and milliseconds..
	 * @param elapsed the duration to transform, expressed in milliseconds.
	 * @return a string specifiying the duration in terms of hours, minutes, seconds and milliseconds.
	 */
	public static String toStringDuration(long elapsed)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(padLeft(""+(elapsed / 3600000L), '0', 2)).append(":");
		elapsed = elapsed % 3600000L;
		sb.append(padLeft(""+(elapsed / 60000L), '0', 2)).append(":");
		elapsed = elapsed % 60000L;
		sb.append(padLeft(""+(elapsed / 1000L), '0', 2)).append(".");
		sb.append(padLeft(""+(elapsed % 1000L), '0', 3));
		return sb.toString();
	}
	
	/**
	 * Replace pre-determined keywords in a string, with shorter ones.
	 * @param key the string to shorten.
	 * @return the string with its keywords replaced.
	 */
	public static String shortenLabel(String key)
	{
		for (int i=0; i<KEYWORDS.length; i++)
		{
			if (key.indexOf(KEYWORDS[i]) >= 0) key = key.replace(KEYWORDS[i], REPLACEMENTS[i]);
		}
		return key;
	}

	/**
	 * Returns the IP address of the remote host for a socket channel.
	 * @param channel the channel to get the host from.
	 * @return an IP address as a string.
	 */
	public static String getRemoteHost(SocketChannel channel)
	{
		StringBuilder sb = new StringBuilder();
		Socket s = channel.socket();
		sb.append("[").append(s.getInetAddress().getHostAddress()).append(":").append(s.getPort()).append("]");
		return sb.toString();
	}
}
