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

package org.jppf.io;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * Output destination backed by a file.
 * @author Laurent Cohen
 */
public class FileOutputDestination extends ChannelOutputDestination
{
	/**
	 * Initialize this file output destination with the specified file path.
	 * @param path the path to the file to read from.
	 * @throws Exception if an IO error occurs.
	 */
	public FileOutputDestination(String path) throws Exception
	{
		this(new File(path));
	}

	/**
	 * Initialize this file output destination with the specified file.
	 * @param file the file to read from.
	 * @throws Exception if an IO error occurs.
	 */
	public FileOutputDestination(File file) throws Exception
	{
		super(new FileOutputStream(file).getChannel());
	}

	/**
	 * Close this output destination and release any system resources associated with it.
	 * @throws IOException if an IO error occurs.
	 * @see java.io.Closeable#close()
	 */
	public void close() throws IOException
	{
		((FileChannel) channel).force(false);
		channel.close();
	}
}
