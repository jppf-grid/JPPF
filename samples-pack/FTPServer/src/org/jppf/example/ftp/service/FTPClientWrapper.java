/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.ftp.service;

import java.io.*;

import org.apache.commons.net.ftp.*;

/**
 * Implementation of a file server client that uses the FTP client provided
 * by the Apache Commons Net library.
 * @see <a href="http://commons.apache.org/net">Apache Commons Net</a>
 * @author Laurent Cohen
 */
public class FTPClientWrapper
{
  /**
   * The underlying FTP client.
   */
  private FTPClient ftpClient;

  /**
   * Open a secure ftp connection with the specified parameters.
   * @param host the host where the FTP server is running
   * @param port the secure FTP port.
   * @param user username to use.
   * @param password the user password.
   * @throws Exception if any error occurs.
   */
  public void open(final String host, final int port, final String user, final String password) throws Exception
  {
    // create with implicit TLS
    ftpClient = new FTPSClient(true);
    ftpClient.connect(host, port);
    ftpClient.login(user, password);
  }

  /**
   * Disconnect from the FTP server and close the connection.
   * @throws Exception if any error occurs.
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
   * Download a file from a remote FTP server.
   * @param localPath the path to the resulting local file.
   * @param remotePath the path of the remote file.
   * @throws Exception if any error occurs during the file transfer.
   */
  public void download(final String localPath, final String remotePath) throws Exception
  {
    BufferedOutputStream bos = null;
    try
    {
      bos = new BufferedOutputStream(new FileOutputStream(localPath));
      ftpClient.retrieveFile(remotePath, bos);
    }
    finally
    {
      if (bos != null) bos.close();
    }
  }

  /**
   * Upload a local file to a remote FTP server.
   * @param localPath the path to the file to upload.
   * @param remotePath the path of the resulting remote file.
   * @throws Exception if any error occurs during the file transfer.
   */
  public void upload(final String localPath, final String remotePath) throws Exception
  {
    BufferedInputStream bis = null;
    try
    {
      bis = new BufferedInputStream(new FileInputStream(localPath));
      ftpClient.storeFile(remotePath, bis);
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
}
