/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
import java.net.*;

/**
 * Wrapper for manipulating data from a URL.
 * @author Laurent Cohen
 */
public class URLLocation extends AbstractLocation<URL>
{
	/**
	 * Initialize this location with the specified file path.
	 * @param url a URL.
	 */
	public URLLocation(URL url)
	{
		super(url);
	}

	/**
	 * Initialize this location with the specified file path.
	 * @param url a URL in string format.
	 * @throws MalformedURLException if the url is malformed.
	 */
	public URLLocation(String url) throws MalformedURLException
	{
		super(new URL(url));
	}

	/**
	 * Obtain an input stream to read from this location.
	 * @return an <code>InputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#getInputStream()
	 */
	public InputStream getInputStream() throws Exception
	{
		return path.openStream();
	}

	/**
	 * Obtain an output stream to write to this location.
	 * @return an <code>OutputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 * @see org.jppf.server.protocol.Location#getOutputStream()
	 */
	public OutputStream getOutputStream() throws Exception
	{
		URLConnection conn = path.openConnection();
		conn.setDoOutput(true);
		return conn.getOutputStream();
	}

	/**
	 * This method always returns -1, as there is no reliable way to know the actual size of available data.
	 * @return -1.
	 * @see org.jppf.server.protocol.Location#size()
	 */
	public long size()
	{
		return -1;
	}
}
