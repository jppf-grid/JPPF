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
import org.jppf.utils.TypedProperties;

/**
 * Implementation of a file server client that uses the FTP client provided
 * by the Apache Commons Net library.
 * @see <a href="http://commons.apache.org/net">Apache Commons Net</a>
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
		TypedProperties c = getConfiguration();
		String host = c.getString("jppf.ftp.client.host", "localhost");
		int port = c.getInt("jppf.ftp.client.port", 12221);
		String user = c.getString("jppf.ftp.client.user", "anonymous");
		String password = c.getString("jppf.ftp.client.password", "");
		ftpClient = c.getBoolean("jppf.ftp.client.secure", false) ? new FTPSClient() : new FTPClient();
		ftpClient.setBufferSize(c.getInt("jppf.ftp.client.bufferSize", 32768));
		ftpClient.setRemoteVerificationEnabled(c.getBoolean("jppf.ftp.client.remoteVerificationEnabled", false));
		ftpClient.setListHiddenFiles(c.getBoolean("jppf.ftp.client.listHiddenFiles", false));
		ftpClient.connect(host, port);
		ftpClient.login(user, password);
		ftpClient.setFileStructure(c.getInt("jppf.ftp.client.fileStructure", FTP.FILE_STRUCTURE));
		ftpClient.setFileType(c.getInt("jppf.ftp.client.fileType", FTP.ASCII_FILE_TYPE), c.getInt("jppf.ftp.client.fileFormat", FTP.NON_PRINT_TEXT_FORMAT));
		ftpClient.setFileTransferMode(c.getInt("jppf.ftp.client.fileTransferMode", FTP.STREAM_TRANSFER_MODE));
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
		BufferedOutputStream bos = null;
		try
		{
			bos = new BufferedOutputStream(new FileOutputStream(localPath));
			boolean ret = ftpClient.retrieveFile(remotePath, bos);
		}
		finally
		{
			if (bos != null) bos.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void upload(String localPath, String remotePath) throws Exception
	{
		BufferedInputStream bis = null;
		try
		{
			bis = new BufferedInputStream(new FileInputStream(localPath));
			boolean ret = ftpClient.storeFile(remotePath, bis);
		}
		finally
		{
			if (bis != null) bis.close();
		}
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
