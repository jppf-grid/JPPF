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

package org.jppf.server.protocol;

import java.io.*;

/**
 * Wrapper fro manipulating a file.
 * @author Laurent Cohen
 */
public class FileLocation extends AbstractLocation<File>
{
	/**
	 * Initialize this location with the specified file path.
	 * @param file an abstract file path.
	 */
	public FileLocation(File file)
	{
		super(file);
	}

	/**
	 * Initialize this location with the specified file path.
	 * @param file an abstract file path.
	 */
	public FileLocation(String file)
	{
		super(new File(file));
	}

	/**
	 * Obtain an input stream to read from this location.
	 * @return an <code>InputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#getInputStream()
	 */
	public InputStream getInputStream() throws Exception
	{
		return new BufferedInputStream(new FileInputStream(path));
	}

	/**
	 * Obtain an output stream to write to this location.
	 * @return an <code>OutputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#getOutputStream()
	 */
	public OutputStream getOutputStream() throws Exception
	{
		return new BufferedOutputStream(new FileOutputStream(path));
	}

	/**
	 * Get the size of the file this location points to.
	 * @return the size as a long value, or -1 if the file does not exist.
	 * @see org.jppf.server.protocol.Location#size()
	 */
	public long size()
	{
		if ((path != null) && path.exists()) return path.length();
		return -1;
	}
}
