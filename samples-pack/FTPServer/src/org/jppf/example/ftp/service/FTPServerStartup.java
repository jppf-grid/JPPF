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
import org.jppf.startup.JPPFDriverStartupSPI;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This class is a driver startup class wrapper that starts an instance of the Apache Mina FTPServer at driver startup time.
 * <p>The server configuration is done via the mechanism provided by Mina FTPServer, the ftpd.xml file.
 * The location of this file is specified in the JPPF configuration via <code>jppf.file.server.config = my/path/ftpd.xml</code>.
 * @see <a href="http://mina.apache.org/ftpserver">Apache Mina FTPServer</a>
 * @author Laurent Cohen
 */
public class FTPServerStartup implements JPPFDriverStartupSPI
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(FTPServerStartup.class);
  /**
   * The underlying embedded FTP server.
   */
  private FtpServer server;

  /**
   * Start the FTP server and add a JVM shutdown hook to stop it.
   */
  @Override
  public void run()
  {
    try
    {
      Runnable hook = new Runnable()
      {
        @Override
        public void run()
        {
          stop();
        }
      };
      Runtime.getRuntime().addShutdownHook(new Thread(hook));
      start();
    }
    catch(Exception e)
    {
      log.error("FTP server initialization failed", e);
      // display the error message on the driver's shell console
      System.err.println("FTP server initialization failed: " + e.getMessage());
    }
  }

  /**
   * Start the FTP server using the configuration file whose path is specified in the driver's configuration.
   * @throws Exception if an error occurs while reading the configuration.
   */
  public void start() throws Exception
  {
    String configPath = JPPFConfiguration.getProperties().getString("jppf.file.server.config", "config/ftpd.xml");
    server = new CommandLineExt(configPath).createServer();
    server.start();
  }

  /**
   * Stop the FTP server. This method is called from a JVM shutdown hook.
   */
  public void stop()
  {
    try
    {
      if ((server != null) && !server.isStopped()) server.stop();
    }
    catch(Throwable t)
    {
      t.printStackTrace();
    }
  }

  /**
   * Get the underlying embedded FTP server.
   * @return an <code>FtpServer</code> instance.
   */
  public FtpServer getServer()
  {
    return server;
  }
}
