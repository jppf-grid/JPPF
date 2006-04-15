/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

import java.io.*;
import java.util.zip.*;
import org.apache.log4j.Logger;

/**
 * Collection of utility methods for compressing and uncompressing data.
 * @author Laurent Cohen
 */
public final class CompressionUtils
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(CompressionUtils.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Instantiation of this class is not permitted.
	 */
	private CompressionUtils()
	{
	}

	/**
	 * Compress an array of bytes using the gzip API.
	 * @param bytes the array of bytes to compress.
	 * @param start the start position in the array of bytes.
	 * @param length the number of bytes to compress.
	 * @return the compressed bytes as an array of bytes.
	 * @throws Exception if an error occurs while compressing.
	 */
	public static byte[] zip(byte[] bytes, int start, int length) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		DataOutputStream dos = new DataOutputStream(gzos);
		dos.writeInt(length);
		dos.write(bytes, start, length);
		dos.flush();
		dos.close();
		if (debugEnabled) log.debug("compressed "+length+" bytes into "+baos.size());
		return baos.toByteArray();
	}

	/**
	 * Uncompress an array of bytes using the gzip API.
	 * @param bytes the array of bytes to uncompress.
	 * @param start the start position in the array of bytes.
	 * @param length the number of bytes to uncompress.
	 * @return the uncompressed bytes as an array.
	 * @throws Exception if an error occurs while uncompressing.
	 */
	public static byte[] unzip(byte[] bytes, int start, int length) throws Exception
	{
		int count = 0;
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes, start, length);
		GZIPInputStream gzis = new GZIPInputStream(bais);
		DataInputStream dis = new DataInputStream(gzis);
		int len = dis.readInt();
		byte[] result = new byte[len];
		while (count < len)
		{
			int offset = count;
			int n = dis.read(result, offset, len-count);
			if (n <= 0) break;
			count += n;
		}
		dis.close();
		if (debugEnabled) log.debug("uncompressed "+length+" bytes into "+result.length);
		return result;
	}
}
