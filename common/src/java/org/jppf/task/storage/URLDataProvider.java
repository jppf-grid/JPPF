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
package org.jppf.task.storage;

import java.io.*;
import java.net.*;
import org.apache.commons.io.IOUtils;

/**
 * Implementation of the DataProvider interface to read data from a URL
 * @author Laurent Cohen
 */
public class URLDataProvider implements DataProvider
{
	/**
	 * Get an input stream from a URL.
	 * @param key the URL identifying the data to retrieve, must be an instance of <code>java.net.URL</code>.
	 * @return a <code>java.io.InputStream</code> opened from the URL location.
	 * @throws Exception if an error occured while retrieving the data.
	 * @see org.jppf.task.storage.DataProvider#getValue(java.lang.Object)
	 */
	public Object getValue(Object key) throws Exception
	{
		URL url = (URL) key;
		try
		{
			return url.openStream();
		}
		catch(Exception e)
		{
			throw new Exception(e);
		}
	}

	/**
	 * Copy the content of an input stream to a location specified as a URL.
	 * @param key the URL specifying the destination, must be an instance of <code>java.net.URL</code>.
	 * @param value the input stream to copy the data from, must be an instance of <code>java.io.InputStream</code>.
	 * @throws Exception if an error occured setting the data.
	 * @see org.jppf.task.storage.DataProvider#setValue(java.lang.Object, java.lang.Object)
	 */
	public void setValue(Object key, Object value) throws Exception
	{
		URL url = (URL) key;
		InputStream is = (InputStream) value;
		try
		{
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			IOUtils.copy(is, os);
			os.close();
		}
		catch(Exception e)
		{
			throw new Exception(e);
		}
	}
}
