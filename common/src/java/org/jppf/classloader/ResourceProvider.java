/*
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
package org.jppf.classloader;

import java.io.*;
import java.net.URL;

import org.apache.commons.logging.*;
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
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ResourceProvider.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Maximum buffer size for reading class files.
	 */
	private static final int BUFFER_SIZE = 32*1024;
	/**
	 * Temporary buffer used to read class files.
	 */
	protected byte[] buffer = new byte[BUFFER_SIZE];

	/**
	 * Default constructor.
	 */
	public ResourceProvider()
	{
	}

	/**
	 * Load a resource file (including class files) from the class path into an array of byte.
	 * @param resName the name of the resource to load.
	 * @return an array of bytes, or nll if the resource could not be found.
	 */
	public byte[] getResourceAsBytes(String resName)
	{
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
				if (debugEnabled) log.debug("resource [" + resName + "] found");
				return getInputStreamAsByte(is);
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		if (debugEnabled) log.debug("resource [" + resName + "] not found");
		return null;
	}

	/**
	 * Get a resource as an array of byte using a call to <b>ClassLoader#getResource()</b>.
	 * @param resName  the name of the resource to find.
	 * @return the content of the resource as an array of bytes.
	 */
	public byte[] getResource(String resName)
	{
		URL url = getClass().getClassLoader().getResource(resName);
		if (url != null)
		{
			try
			{
				if (debugEnabled) log.debug("resource [" + resName + "] found, url = " + url);
				return getInputStreamAsByte(url.openStream());
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
		else
		{
			InputStream is = null;
			try
			{
				File file = new File(resName);
				if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
				if (is != null)
				{
					if (debugEnabled) log.debug("resource [" + resName + "] found");
					return getInputStreamAsByte(is);
				}
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
		if (debugEnabled) log.debug("resource [" + resName + "] not found");
		return null;
	}

	/**
	 * Get the content of an input stream as an array of byte.
	 * @param is the input stream to read from.
	 * @return a byte array.
	 */
	private byte[] getInputStreamAsByte(InputStream is)
	{
		byte[] b = null;
		try
		{
			ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
			boolean end = false;
			while (!end)
			{
				int n = is.read(buffer, 0, BUFFER_SIZE);
				// by contract instances of InputStream are not require to fill the entire byte[]
				if (n < 0) end = true;
				else baos.write(buffer, 0, n);
			}
			is.close();
			baos.flush();
			b = baos.toByteArray();
			baos.close();
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return b;
	}
}
