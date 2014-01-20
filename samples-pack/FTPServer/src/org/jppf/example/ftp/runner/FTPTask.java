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
package org.jppf.example.ftp.runner;

import org.jppf.example.ftp.service.FTPClientWrapper;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.FileUtils;


/**
 * This task processes a text file downloaded from an FTP server embedded within the JPPF driver.
 * The text file is transformed into a resulting html file which is then uploaded to the same FTP server.
 * @author Laurent Cohen
 */
public class FTPTask extends JPPFTask
{
  /**
   * The file to download from the driver.
   */
  private String inFile;
  /**
   * The resulting file to upload to the server.
   */
  private String outFile;

  /**
   * Initialize this task with the specified in and out files.
   * @param inFile the file to download from the driver.
   * @param outFile the place where to store the downloaded file.
   */
  public FTPTask(final String inFile, final String outFile)
  {
    this.inFile = inFile;
    this.outFile = outFile;
  }

  /**
   * Download a text file from the driver, process it, store the result in an HTML file and upload it to the driver.
   */
  @Override
  public void run()
  {
    try
    {
      // retrieve the FTP host from the data provider
      DataProvider dataProvider = getDataProvider();
      String host = dataProvider.getParameter("ftp.host");
      FTPClientWrapper client = new FTPClientWrapper();
      // this is just for demonstration purposes, the password should never be exposed like this!
      client.open(host, 12222, "admin", "admin");

      // download the input text file
      client.download(inFile, inFile);
      String text = FileUtils.readTextFile(inFile);
      // transform double line breaks into paragraphs
      text = text.replace("\n\n", "<p>");
      // transform remaining line breaks into html line breaks
      text = text.replace("\n", "<br/>");
      // set all occurrences of JPPF in bold
      text = text.replace("JPPF", "<b>JPPF</b>");
      // add barebone HTML header and footer
      StringBuilder sb = new StringBuilder();
      sb.append("<html><body>").append(text).append("</body></html>");
      FileUtils.writeTextFile(outFile, sb.toString());
      // upload the HTML file to the server.
      client.upload(outFile, outFile);

      client.close();
      setResult("execution successful");
    }
    catch(Exception e)
    {
      setResult("execution failed: " + e.getClass().getName() + ": " + e.getMessage());
      setThrowable(e);
    }
  }
}

