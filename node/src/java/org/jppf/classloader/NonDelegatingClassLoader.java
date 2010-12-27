/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.classloader;

import java.io.InputStream;
import java.net.*;
import java.util.*;

import org.jppf.utils.FileUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class NonDelegatingClassLoader extends URLClassLoader
{
	/**
	 * A cache of the resources loaded by all instances of <code>NonDelegatingClassLoader</code>,
	 * to avoid reloading the byte code.
	 */
	private static Map<String, byte[]> resourceMap = new Hashtable<String, byte[]>();

	/**
	 * Initialize this class loader with the specified urls and parent.
	 * @param urls an array of class path urls.
	 * @param parent the parent class loader.
	 */
	public NonDelegatingClassLoader(URL[] urls, ClassLoader parent)
	{
		super(urls == null ? new URL[0] : urls, parent);
	}

	/**
	 * Attempts to load a class directly from the parent.
	 * @param name the name of the class to load.
	 * @return the correpsonding class.
	 * @throws ClassNotFoundException if the class could not be loaded.
	 */
	public Class<?> loadClassDirect(String name) throws ClassNotFoundException
	{
		Class<?> c = null;
		try
		{
			byte[] classBytes = getResourceBytes(name);
			if (classBytes != null) c = defineClass(name, classBytes, 0, classBytes.length);
		}
		catch(Exception e)
		{
			//e.printStackTrace();
		}
		if (c == null) c = super.loadClass(name);
		return c;
	}

	/**
	 * Get the byte[] for the specified resource name.
	 * @param name the name of the resource to find.
	 * @return an array of bytes, or null if the resource could not be found.
	 */
	private byte[] getResourceBytes(String name)
	{
		byte[] b = resourceMap.get(name);
		if (b == null)
		{
			String resName = name.replace('.', '/') + ".class";
			InputStream is = getResourceAsStream(resName);
			if (is != null)
			{
				try
				{
					b = FileUtils.getInputStreamAsByte(is);
					if (b != null) resourceMap.put(name, b);
					// we won't have to search it again
					else resourceMap.put(name, new byte[0]);
				}
				catch(Exception e)
				{
				}
			}
		}
		else if (b.length == 0) b = null; 
		return b;
	}
}
