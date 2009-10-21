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

/**
 * Interface for all builders instantiating alternate object input streams and output streams.
 * @author Laurent Cohen
 */
public interface JPPFObjectStreamBuilder
{
	/**
	 * Obtain an input stream used for deserializing objects.
   * @param	in input stream to read from.
	 * @return an <code>ObjectInputStream</code>
	 * @throws Exception if an error is raised while creating the stream.
	 */
	ObjectInputStream newObjectInputStream(InputStream in) throws Exception;
	/**
	 * Obtain an Output stream used for serializing objects.
   * @param	out output stream to write to.
	 * @return an <code>ObjectOutputStream</code>
	 * @throws Exception if an error is raised while creating the stream.
	 */
	ObjectOutputStream newObjectOutputStream(OutputStream out) throws Exception;
}
