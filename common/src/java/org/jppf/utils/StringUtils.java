/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.utils;

import java.io.File;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.*;
import org.apache.log4j.Logger;


/**
 * This class provides a set of utility methods for manipulating strings. 
 * @author Laurent Cohen
 */
public final class StringUtils
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(StringUtils.class);
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
		sb.append(elapsed / 3600000L).append(":");
		elapsed = elapsed % 3600000L;
		sb.append(elapsed / 60000L).append(":");
		elapsed = elapsed % 60000L;
		sb.append(elapsed / 1000L).append(".");
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
	 * COmpute a localisation base path from a base folder and a file name.
	 * @param base the base folder path as a string.
	 * @param filename the filename from which to get the resource bundle name.
	 * @return the complete path to a resource bundle.
	 */
	public static String getLocalisationBase(String base, String filename)
	{
		String result = null;
		try
		{
			File file = new File(filename);
			result = file.getName();
			int idx = result.lastIndexOf(".xml");
			if (idx >= 0) result = result.substring(0, idx);
			result = base + "/" + result;
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Get a localized property value.
	 * @param baseName the base name to use, in combination with the default locale,
	 * to lookup the appropriate resource bundle.
	 * @param key the key for the localized value to lookup.
	 * @return the name localized through the default locale information, or the key itself if
	 * it could not be localized.
	 * @see java.util.ResourceBundle
	 */
	public static String getLocalized(String baseName, String key)
	{
		return getLocalized(baseName, key, key);
	}

	/**
	 * Get a localized property value.
	 * @param baseName the base name to use, in combination with the default locale,
	 * to lookup the appropriate resource bundle.
	 * @param key the key for the localized value to lookup.
	 * @param def the default value to return if no localized string could be found.
	 * @return the name localized through the default locale information, or the key itself if
	 * it could not be localized.
	 * @see java.util.ResourceBundle
	 */
	public static String getLocalized(String baseName, String key, String def)
	{
		if (baseName == null) return def;
		String result = null;
		try
		{
			ResourceBundle bundle = ResourceBundle.getBundle(baseName);
			result = bundle.getString(key);
		}
		catch (Exception e)
		{
			log.error(e.getMessage());
			if (log.isDebugEnabled()) log.debug(e);
		}
		return result == null ? def : result;
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
