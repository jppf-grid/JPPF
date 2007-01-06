/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
