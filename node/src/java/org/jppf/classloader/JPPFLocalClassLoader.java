/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
package org.jppf.classloader;

import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.IOHandler;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public class JPPFLocalClassLoader extends AbstractJPPFClassLoader
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFLocalClassLoader.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * temporary IO handler.
	 */
	private static IOHandler tmpHandler = null;

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param ioHandler wraps the communication channel with the driver.
	 * @param parent a ClassLoader instance.
	 */
	public JPPFLocalClassLoader(IOHandler ioHandler, ClassLoader parent)
	{
		super(ioHandler, parent);
		tmpHandler = ioHandler;
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 * @param uuidPath unique identifier for the submitting application.
	 */
	public JPPFLocalClassLoader(ClassLoader parent, List<String> uuidPath)
	{
		super(parent, uuidPath);
	}

	/**
	 * Initialize the connection with the class server.
	 * @return the IOHandler created.
	 * @see org.jppf.classloader.AbstractJPPFClassLoader#initIoHandler()
	 */
	protected IOHandler initIoHandler()
	{
		setInitializing(true);
		if (debugEnabled) log.debug("initializing connection");
		System.out.println("JPPFClassLoader.init(): attempting connection to the class server");
		return tmpHandler;
	}

	/**
	 * Initialize the underlying socket connection.
	 */
	private void initSocketClient()
	{
		if (debugEnabled) log.debug("initializing socket connection");
	}
	
	/**
	 * Terminate this classloader and clean the resources it uses.
	 * @see org.jppf.classloader.AbstractJPPFClassLoader#close()
	 */
	public void close()
	{
	}
}
