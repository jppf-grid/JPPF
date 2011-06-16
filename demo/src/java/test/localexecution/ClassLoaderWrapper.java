/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package test.localexecution;

import java.lang.reflect.Method;
import java.net.*;

import org.jppf.classloader.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ClassLoaderWrapper
{
	/**
	 * 
	 */
	private static Method addURLMethod = findAddURLMethod();
	/**
	 * 
	 */
	private JPPFClassLoader loader;

	/**
	 * Create this class loader.
	 * @param loader the parent class loader.
	 */
	public ClassLoaderWrapper(JPPFClassLoader loader)
	{
		this.loader = loader;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addURL(URL url)
	{
		try
		{
			addURLMethod.invoke(loader, url);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Fetch the method object for <code>addURL()</code>.
	 * @return a <code>Method</code> object.
	 */
	private static Method findAddURLMethod()
	{
		Method m = null;
		try
		{
			m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			if (!m.isAccessible()) m.setAccessible(true);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return m;
	}
}
