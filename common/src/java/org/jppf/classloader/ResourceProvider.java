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
package org.jppf.classloader;

import java.io.*;
import org.apache.log4j.Logger;
import org.jppf.utils.JPPFByteArrayOutputStream;

/**
 * Instances of this class are dedicated to reading resource files form the JVM's classpath and converting them into
 * arrays of bytes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class ResourceProvider
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ResourceProvider.class);
	/**
	 * Maximum buffer size for reading class files.
	 */
	private static final int BUFFER_SIZE = 32*1024;
	/**
	 * Temporary buffer used to read class files.
	 */
	protected byte[] buffer = new byte[BUFFER_SIZE];

	/**
	 * Load a resource file (including class files) from the class path into an array of byte.
	 * @param resName the name of the resource to load.
	 * @return an array of bytes, or nll if the resource could not be found.
	 */
	public byte[] getResourceAsBytes(String resName)
	{
		byte[] b = null;
		try
		{
			InputStream is = getClass().getClassLoader().getResourceAsStream(resName);
			if (is == null)
			{
				File file = new File(resName);
				if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
			}
			if (is != null)
			{
				ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
				boolean end = false;
				while (!end)
				{
					int n = is.read(buffer, 0, BUFFER_SIZE);
					//by contract instances of InputStream are not require to fill 
					//the entire byte[]
					if (n < 0) end = true;
					else baos.write(buffer, 0, n);
				}
				is.close();
				baos.flush();
				b = baos.toByteArray();
				baos.close();
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return b;
	}
}
