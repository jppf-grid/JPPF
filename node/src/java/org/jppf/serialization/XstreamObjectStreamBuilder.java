/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.serialization;

import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.jppf.JPPFError;

/**
 * Standard object stream factory.
 * This factory creates instances of {@link java.io.ObjectInputStream ObjectInputStream}
 * and {@link java.io.ObjectOutputStream ObjectOutputStream}
 * @author Laurent Cohen
 */
public class XstreamObjectStreamBuilder implements JPPFObjectStreamBuilder
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(XstreamObjectStreamBuilder.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Used for thread synchronization when initializing the xstream object.
	 */
	private static ReentrantLock lock = new ReentrantLock();
	/**
	 * The method to invoke to create an object input stream.
	 */
	private static Method createOisMethod = null;
	/**
	 * The method to invoke to create an object output stream.
	 */
	private static Method createOosMethod = null;
	/**
	 * The Xstream facade object.
	 */
	private static Object xstream = getXstream();

	/**
	 * Obtain an input stream used for deserializing objects.
   * @param	in input stream to read from.
	 * @return an <code>ObjectInputStream</code>
	 * @throws Exception if an error is raised while creating the stream.
	 * @see org.jppf.serialization.JPPFObjectStreamBuilder#newObjectInputStream(java.io.InputStream)
	 */
	public ObjectInputStream newObjectInputStream(InputStream in) throws Exception
	{
		return (ObjectInputStream) createOisMethod.invoke(xstream, new Object[] {in});
	}

	/**
	 * Obtain an Output stream used for serializing objects.
   * @param	out output stream to write to.
	 * @return an <code>ObjectOutputStream</code>
	 * @throws Exception if an error is raised while creating the stream.
	 * @see org.jppf.serialization.JPPFObjectStreamBuilder#newObjectOutputStream(java.io.OutputStream)
	 */
	public ObjectOutputStream newObjectOutputStream(OutputStream out) throws Exception
	{
		return (ObjectOutputStream) createOosMethod.invoke(xstream, new Object[] {out});
	}

	/**
	 * Create an Xstream object using reflection. 
	 * @return an Object instance.
	 */
	private static synchronized Object getXstream()
	{
		try
		{
		if (xstream == null)
		{
			lock.lock();
			try
			{
				if (xstream == null)
				{
					Class xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
					Object o =  xstreamClass.newInstance();
					/*
					// use this code to use the XStream default driver (DomDriver)
					Class xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
					Class hierarchicalStreamDriverClass = Class.forName("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
					Class domDriverClass = Class.forName("com.thoughtworks.xstream.io.xml.DomDriver");
					Constructor c = xstreamClass.getConstructor(hierarchicalStreamDriverClass);
					Object o = c.newInstance(domDriverClass.newInstance());
					*/
					// use this code to use the XStream XPP driver (XPPDriver)
					createOisMethod = xstreamClass.getMethod("createObjectInputStream", new Class[] {InputStream.class});
					createOosMethod = xstreamClass.getMethod("createObjectOutputStream", new Class[] {OutputStream.class});
					xstream = o;
				}
			}
			finally
			{
				lock.unlock();
			}
		}
		}
		catch(Exception e)
		{
			log.fatal(e.getMessage(), e);
			throw new JPPFError("A fatal error occurred: "+e.getMessage(), e);
		}
		return xstream;
	}
}
