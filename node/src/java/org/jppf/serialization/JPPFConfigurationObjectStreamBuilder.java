/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.serialization;

import java.io.*;
import java.lang.reflect.Constructor;

/**
 * Standard object stream factory.
 * This factory creates instances of {@link java.io.ObjectInputStream ObjectInputStream}
 * and {@link java.io.ObjectOutputStream ObjectOutputStream}
 * @author Laurent Cohen
 */
public class JPPFConfigurationObjectStreamBuilder implements JPPFObjectStreamBuilder
{
	/**
	 * The class of input object streams.
	 */
	private Class oisClass = null;
	/**
	 * The class of output object streams.
	 */
	private Class oosClass = null;
	/**
	 * Object input stream constructor.
	 */
	private Constructor oisConstructor = null;
	/**
	 * Object output stream constructor.
	 */
	private Constructor oosConstructor = null;

	/**
	 * Initialize this builder with the default <code>ObjectInputStream</code> and <code>ObjectOutputStream</code> classes of the JDK. 
	 * @throws Exception if an error is raised while initializing.
	 */
	public JPPFConfigurationObjectStreamBuilder() throws Exception
	{
		oisClass = ObjectInputStream.class;
		oosClass = ObjectOutputStream.class;
		initializeConstructors();
	}

	/**
	 * Initialize this builder with the specified object input and output stream classes.
	 * @param oisClass the object input stream class to use.
	 * @param oosClass the object output stream class to use.
	 * @throws Exception if an error is raised while initializing.
	 */
	public JPPFConfigurationObjectStreamBuilder(Class oisClass, Class oosClass) throws Exception
	{
		this.oisClass = oisClass;
		this.oosClass = oosClass;
		initializeConstructors();
	}

	/**
	 * Initialize this builder with the specified object input and output stream classes.
	 * @param oisClassName the fully qualified name of the object input stream class to use.
	 * @param oosClassName the fully qualified name of the object output stream class to use.
	 * @throws Exception if an error is raised while initializing.
	 */
	public JPPFConfigurationObjectStreamBuilder(String oisClassName, String oosClassName) throws Exception
	{
		this(Class.forName(oisClassName), Class.forName(oosClassName));
	}

	/**
	 * Initialize the constructors for the object stream classes.
	 * @throws Exception if an error is raised while initializing.
	 */
	private void initializeConstructors() throws Exception
	{
		oisConstructor = oisClass.getConstructor(InputStream.class);
		oosConstructor = oosClass.getConstructor(OutputStream.class);
	}

	/**
	 * Obtain an input stream used for deserializing objects.
   * @param	in input stream to read from.
	 * @return an <code>ObjectInputStream</code>
	 * @throws Exception if an error is raised while creating the stream.
	 * @see org.jppf.serialization.JPPFObjectStreamBuilder#newObjectInputStream(java.io.InputStream)
	 */
	public ObjectInputStream newObjectInputStream(InputStream in) throws Exception
	{
		return (ObjectInputStream) oisConstructor.newInstance(in);
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
		return (ObjectOutputStream) oosConstructor.newInstance(out);
	}
}
