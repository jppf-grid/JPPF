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

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.main.CommandLine;

/**
 * This class extends the Apache <code>CommandLine</code> class to enable access to the ftp server
 * while keeping the convenience of the XML configuration file.
 * @author Laurent Cohen
 */
public class CommandLineExt extends CommandLine
{
  /**
   * Path to the FTP server's XML configuration file.
   */
  private String configFile = null;

  /**
   * Initialize this object with the specified configuration file.
   * @param configFile the path to an ftpd XML configuration file.
   */
  public CommandLineExt(final String configFile)
  {
    super();
    this.configFile = configFile;
  }

  /**
   * Create and start the ftpd server
   * @return an <code>FtpServer</code> instance.
   * @throws Exception if any error occurs.
   */
  public FtpServer createServer() throws Exception
  {
    return super.getConfiguration(new String[] {configFile});
  }
}
