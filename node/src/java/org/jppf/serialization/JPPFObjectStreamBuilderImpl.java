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

/**
 * Standard object stream factory.
 * This factory creates instances of {@link java.io.ObjectInputStream ObjectInputStream}
 * and {@link java.io.ObjectOutputStream ObjectOutputStream}
 * @author Laurent Cohen
 */
public class JPPFObjectStreamBuilderImpl implements JPPFObjectStreamBuilder
{
	/**
	 * Obtain an input stream used for deserializing objects.
   * @param	in input stream to read from.
	 * @return an <code>ObjectInputStream</code>
	 * @throws Exception if an error is raised while creating the stream.
	 * @see org.jppf.serialization.JPPFObjectStreamBuilder#newObjectInputStream(java.io.InputStream)
	 */
	public ObjectInputStream newObjectInputStream(InputStream in) throws Exception
	{
		return new ObjectInputStream(in);
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
		return new ObjectOutputStream(out);
	}
}
