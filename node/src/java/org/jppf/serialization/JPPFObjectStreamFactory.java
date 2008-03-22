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
 * Interface for all factories instantiating alternate object input streams and output streams.
 * @author Laurent Cohen
 */
public interface JPPFObjectStreamFactory
{
	/**
	 * Obtain an input stream used for deserializing objects.
	 * @return an <code>ObjectInputStream</code>
	 */
	ObjectInputStream newObjectInputStream();
	/**
	 * Obtain an Output stream used for serializing objects.
	 * @return an <code>ObjectOutputStream</code>
	 */
	ObjectOutputStream newObjectOutputStream();
}
