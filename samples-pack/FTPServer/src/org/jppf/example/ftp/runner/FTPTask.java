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
package org.jppf.example.ftp.runner;

import org.jppf.example.ftp.service.FTPClientWrapper;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;


/**
 * This task is for testing the netwrok transfer of task with various data sizes.
 * @author Laurent Cohen
 */
public class FTPTask extends JPPFTask
{
	/**
	 * The file to download from the driver.
	 */
	private String inFile;
	/**
	 * The place where to store the downloaded file.
	 */
	private String outFile;

	/**
	 * Initialize this task with the specified in and out files.
	 * @param inFile the file to download from the driver.
	 * @param outFile the place where to store the downloaded file.
	 */
	public FTPTask(String inFile, String outFile)
	{
		this.inFile = inFile;
		this.outFile = outFile;
	}
	
	/**
	 * Download the file from the driver and store it in the out location.
	 * @see sample.BaseDemoTask#doWork()
	 */
	public void run()
	{
		try
		{
			// retrieve the FTP host from the data provider
			DataProvider dataProvider = getDataProvider();
			String host = (String) dataProvider.getValue("ftp.host");
			FTPClientWrapper client = new FTPClientWrapper();
			// this is just for demonstration purposes, the password should never be exposed like this!
			client.open(host, 12221, "admin", "admin");
			client.download(outFile, inFile);
			client.close();
			setResult("execution successful");
		}
		catch(Exception e)
		{
			setResult("execution failed: " + e.getClass().getName() + ": " + e.getMessage());
			setException(e);
		}
	}
}

