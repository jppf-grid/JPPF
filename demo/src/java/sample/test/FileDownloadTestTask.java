/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package sample.test;

import java.io.*;
import java.net.URL;
import org.jppf.utils.FileUtils;

/**
 * This task downloads a file from a web site, then uploads it to an ftp server.
 * Task used to test the {@link org.jppf.task.storage.URLDataProvider URLDataProvider} class.
 * @author Laurent Cohen
 */
public class FileDownloadTestTask extends JPPFTestTask
{
	/**
	 * The URL of a file to downlod as a string.
	 */
	protected String location = null;

	/**
	 * Initialize this task with the location of a file to download.
	 * @param location the file URL as a string.
	 */
	public FileDownloadTestTask(String location)
	{
		this.location = location;
	}

	/**
	 * Run the task.
	 * @throws Exception if any error occurs while running ths task..
	 * @see java.lang.Runnable#run()
	 */
	public void testDownloadUpload() throws Exception
	{
		URL url = new URL(location);
		InputStream is = (InputStream) getDataProvider().getValue(url);
		String s = FileUtils.readTextFile(new BufferedReader(new InputStreamReader(is)));
		setResult(s);
		url = new URL("ftp://localhost/Options.xsd");
		getDataProvider().setValue(url, new ByteArrayInputStream(s.getBytes()));
	}
}
