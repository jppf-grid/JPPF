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

import java.net.URL;

import org.apache.ftpserver.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.jppf.fileserver.*;

/**
 * This file server implementation relies on an underlying FTP server
 * provided by the Apache Mina FTPServer library.
 * @see <a href="http://mina.apache.org/ftpserver">http://mina.apache.org/ftpserver</a>
 * @author Laurent Cohen
 */
public class FtpFileServer extends ConfigurableAdapter implements FileServer
{
	/**
	 * The underlying embedded FTP server.
	 */
	private FtpServer server;

	/**
	 * {@inheritDoc}
	 */
	public void start() throws Exception
	{
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory listenerFactory = new ListenerFactory();
		listenerFactory.setPort(configuration.getInt("jppf.file.server.port", 12221));
		serverFactory.addListener("default", listenerFactory.createListener());
		URL url = getClass().getClassLoader().getResource("users.properties");
		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		userManagerFactory.setUrl(url);
		serverFactory.setUserManager(userManagerFactory.createUserManager());
		// start the server
		server = serverFactory.createServer(); 
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
