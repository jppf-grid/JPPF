/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.classloader;

import java.io.*;
import java.net.URL;

import org.apache.commons.logging.*;
import org.jppf.utils.FileUtils;

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
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Default constructor.
	 */
	public ResourceProvider()
	{
	}

	/**
	 * Load a resource file (including class files) from the class path into an array of byte.<br>
	 * This method simply calls {@link #getResourceAsBytes(java.lang.String, java.lang.ClassLoader) getResourceAsBytes(String, ClassLoader)}
	 * with a null class loader.
	 * @param resName the name of the resource to load.
	 * @return an array of bytes, or nll if the resource could not be found.
	 */
	public byte[] getResourceAsBytes(String resName)
	{
		return getResourceAsBytes(resName, null);
	}

	/**
	 * Load a resource file (including class files) from the class path or the file system into an array of byte.
	 * The search order is defined as follows:<br>
	 * - first the search is performed in the order specified by {@link java.lang.ClassLoader#getResourceAsStream(java.lang.String) ClassLoader.getResourceAsStream(String)}<br>
	 * - if the resource is not found, it will be looked up in the file system <br>
	 * @param resName the name of the resource to load.
	 * @param cl the class loader to use to load the request resource.
	 * @return an array of bytes, or nll if the resource could not be found.
	 */
	public byte[] getResourceAsBytes(String resName, ClassLoader cl)
	{
		try
		{
			if (cl == null) cl = Thread.currentThread().getContextClassLoader();
			if (cl == null) cl = getClass().getClassLoader();
			InputStream is = cl.getResourceAsStream(resName);
			if (is == null)
			{
				File file = new File(resName);
				if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
			}
			if (is != null)
			{
				if (debugEnabled) log.debug("resource [" + resName + "] found");
				return FileUtils.getInputStreamAsByte(is);
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
	 * This method simply calls {@link #getResource(java.lang.String, java.lang.ClassLoader) getResource(String, ClassLoader)}
	 * with a null class loader.
	 * @param resName  the name of the resource to find.
	 * @return the content of the resource as an array of bytes.
	 */
	public byte[] getResource(String resName)
	{
		return getResource(resName, null);
	}

	/**
	 * Get a resource as an array of byte using a call to <b>ClassLoader#getResource()</b>.
	 * @param resName  the name of the resource to find.
	 * @param cl the class loader to use to load the request resource.
	 * @return the content of the resource as an array of bytes.
	 */
	public byte[] getResource(String resName, ClassLoader cl)
	{
		if (cl == null) cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) cl = getClass().getClassLoader();
		URL url = cl.getResource(resName);
		if (url != null)
		{
			try
			{
				if (debugEnabled) log.debug("resource [" + resName + "] found, url = " + url);
				return FileUtils.getInputStreamAsByte(url.openStream());
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
					return FileUtils.getInputStreamAsByte(is);
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
}
