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

package org.jppf.fileserver;

import java.util.Properties;

import org.jppf.startup.*;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class FileServerFactory implements JPPFNodeStartupSPI, JPPFClientStartupSPI, JPPFDriverStartupSPI
{
	/**
	 * Default configuration properties for the file client.
	 */
	private static TypedProperties clientConfig = null;
	/**
	 * Default configuration properties for the file server.
	 */
	private static TypedProperties serverConfig = null;
	/**
	 * A singleton instance of the file server.
	 */
	private static FileServer server = null;

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		TypedProperties jppfConfig = JPPFConfiguration.getProperties();
		clientConfig = jppfConfig.getProperties("jppf.file.client.config", jppfConfig);
		serverConfig = jppfConfig.getProperties("jppf.file.server.config", jppfConfig);
	}

	/**
	 * Create a file server client from the default configuration.
	 * @return a {@link FileClient} instance.
	 * @throws Exception if any error occurs while creating the client.
	 */
	public static FileClient createFileClient() throws Exception
	{
		String className = clientConfig.getString("jppf.file.client.class", null);
		if (className == null) return null;
		return createFileClient(className, clientConfig);
	}

	/**
	 * Create a file server client.
	 * @param className the fully qualified class name of the client implementation;
	 * the class must implement the {@link FileClient} interface and have a no-args constructor.
	 * @param configuration the properties used to configure the client.
	 * @return a {@link FileClient} instance.
	 * @throws Exception if any error occurs while creating the client.
	 */
	@SuppressWarnings("unchecked")
	public static FileClient createFileClient(String className, Properties configuration) throws Exception
	{
		Class<? extends FileClient> implClass =
			(Class<? extends FileClient>) FileServerFactory.class.getClassLoader().loadClass(className);
		FileClient client = implClass.newInstance();
		client.configure(configuration);
		return client;
	}

	/**
	 * Create a file server from the default configuration.
	 * @return a {@link FileServer} instance.
	 * @throws Exception if any error occurs while creating the client.
	 */
	@SuppressWarnings("unchecked")
	public static FileServer getFileServer() throws Exception
	{
		if (server == null)
		{
			String className = serverConfig.getString("jppf.file.server.class", null);
			if (className == null) return null;
			Class<? extends FileServer> implClass =
				(Class<? extends FileServer>) FileServerFactory.class.getClassLoader().loadClass(className);
			server = implClass.newInstance();
			server.configure(serverConfig);
		}
		return server;
	}
}
