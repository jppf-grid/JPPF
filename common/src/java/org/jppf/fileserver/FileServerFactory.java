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
	private static TypedProperties clientConfiguration = null;
	/**
	 * Default configuration properties for the file server.
	 */
	private static TypedProperties serverConfiguration = null;
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
		clientConfiguration = jppfConfig.getProperties("jppf.file.client.config", new TypedProperties());
		serverConfiguration = jppfConfig.getProperties("jppf.file.server.config", new TypedProperties());
	}

	/**
	 * Create a file server client from the default configuration.
	 * @return a {@link FileClient} instance.
	 * @throws Exception if any error occurs while creating the client.
	 */
	public static FileClient createFileClient() throws Exception
	{
		String className = clientConfiguration.getString("jppf.file.client.class");
		return createFileClient(className, clientConfiguration);
	}

	/**
	 * Create a file server client.
	 * @param implementationName the fully qualified class name of the client implementation;
	 * the class must implement the {@link FileClient} interface and have a no-args constructor.
	 * @param configuration the properties used to configure the client.
	 * @return a {@link FileClient} instance.
	 * @throws Exception if any error occurs while creating the client.
	 */
	public static FileClient createFileClient(String implementationName, Properties configuration) throws Exception
	{
		Class<? extends FileClient> implClass =
			(Class<? extends FileClient>) FileServerFactory.class.getClassLoader().loadClass(implementationName);
		return createFileClient(implClass, configuration);
	}

	/**
	 * Create a file server client.
	 * @param implementationClass the class of the client implementation;
	 * the class must implement the {@link FileClient} interface and have a no-args constructor.
	 * @param configuration the properties used to configure the client.
	 * @return a {@link FileClient} instance.
	 * @throws Exception if any error occurs while creating the client.
	 */
	public static FileClient createFileClient(Class<? extends FileClient> implementationClass, Properties configuration) throws Exception
	{
		FileClient client = implementationClass.newInstance();
		client.configure(configuration);
		return client;
	}

	/**
	 * Get the default configuration properties for the file client.
	 * @return the configuration as a {@link TypedProperties} instance.
	 */
	public static TypedProperties getClientConfiguration()
	{
		return clientConfiguration;
	}

	/**
	 * Get the default configuration properties for the file server.
	 * @return the configuration as a {@link TypedProperties} instance.
	 */
	public static TypedProperties getServerConfiguration()
	{
		return serverConfiguration;
	}
}
