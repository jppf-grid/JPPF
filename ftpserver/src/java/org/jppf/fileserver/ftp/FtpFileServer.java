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

import org.apache.ftpserver.FtpServer;
import org.jppf.fileserver.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This file server implementation relies on an underlying FTP server
 * provided by the Apache Mina FTPServer library.
 * <p>The server configuration is done via the mechanism provided by Mina FTPServer, the ftpd.xml file.
 * The location of this file is specified in the JPPF configuration via <code>jppf.file.server.config = my/path/ftpd.xml</code>.
 * @see <a href="http://mina.apache.org/ftpserver">Apache Mina FTPServer</a>
 * @author Laurent Cohen
 */
public class FtpFileServer extends ConfigurableAdapter implements FileServer, FileServerStartup
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(FtpFileServer.class);
	/**
	 * The underlying embedded FTP server.
	 */
	private FtpServer server;

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		try
		{
			start();
		}
		catch(Exception e)
		{
			log.error("FTP server initialization failed", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void start() throws Exception
	{
		configure(JPPFConfiguration.getProperties());
		String configPath = configuration.getString("jppf.file.server.config", "config/ftpd.xml");
		server = new CommandLineExt(configPath).createServer();
		server.start();
	}

	/**
	 * {@inheritDoc}
	 */
	public void stop() throws Exception
	{
		server.stop();
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
