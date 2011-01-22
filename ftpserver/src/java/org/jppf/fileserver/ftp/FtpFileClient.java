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

package org.jppf.fileserver.ftp;

import java.io.*;

import org.apache.commons.net.ftp.*;
import org.jppf.fileserver.*;

/**
 * Implementation of a file server client that uses the FTP client provided
 * by the Apache Commons Net library.
 * @see <a href="http://commons.apache.org/net">http://commons.apache.org/net</a>
 * @author Laurent Cohen
 */
public class FtpFileClient extends ConfigurableAdapter implements FileClient
{
	/**
	 * The underlying FTP client.
	 */
	private FTPClient ftpClient;

	/**
	 * {@inheritDoc}
	 */
	public void open() throws Exception
	{
		String host = configuration.getString("jppf.ftp.host", "localhost");
		int port = configuration.getInt("jppf.ftp.port", 12221);
		String user = configuration.getString("jppf.ftp.user", "anonymous");
		String password = configuration.getString("jppf.ftp.password", "");
		ftpClient = new FTPClient();
		ftpClient.connect(host, port);
		ftpClient.login(user, password);
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws Exception
	{
		try
		{
			ftpClient.logout();
		}
		catch(IOException e)
		{
			System.err.println("error logging off the ftp client: " + e.getMessage());
		}
		try
		{
			ftpClient.disconnect();
		}
		catch(IOException e)
		{
			System.err.println("error disconnecting from the ftp client: " + e.getMessage());
		}
		ftpClient = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void download(String localPath, String remotePath) throws Exception
	{
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(localPath));
		boolean ret = ftpClient.retrieveFile(remotePath, bos);
		bos.close();
	}

	/**
	 * {@inheritDoc}
	 */
	public void upload(String localPath, String remotePath) throws Exception
	{
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localPath));
		boolean ret = ftpClient.storeFile(remotePath, bis);
		bis.close();
	}

	/**
	 * Get the underlying FTP client.
	 * <br/>If this method is called before {@link #open() open()} has been invoked, the returned object will be null.
	 * @return  an <code>FTPClient</code> instance.
	 */
	public FTPClient getFtpClient()
	{
		return ftpClient;
	}

	/**
	 * Get information on a remote file. 
	 * @param remotePath the file path on the remote server, with regards to the FTP user root directory.
	 * @return a FTPFile instance, or null if the fille could not be found.
	 * @throws Exception if any error occurs.
	 */
	private FTPFile getRemoteFileInfo(String remotePath) throws Exception
	{
		FTPFile[] files = ftpClient.listFiles(remotePath);
		if ((files == null) || (files.length == 0)) return null;
		return files[0];
	}
}
