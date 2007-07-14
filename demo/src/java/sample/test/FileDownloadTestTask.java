/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
