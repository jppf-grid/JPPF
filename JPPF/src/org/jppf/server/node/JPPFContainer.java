/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.server.node;

import java.io.*;
import org.apache.log4j.Logger;
import org.jppf.node.*;
import org.jppf.utils.*;

/**
 * Instances of this class represent dynamic class loading, and serialization/deserialization, capabilities, associated
 * with a specific client application.<br>
 * The application is identified through a unique uuid. This class effectively acts as a container for the classes of
 * a client application, a provides the methods to enable the transport, serialization and deserialization of these classes.
 * @author Laurent Cohen
 */
public class JPPFContainer
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFContainer.class);
	/**
	 * Utility for deserialization and serialization.
	 */
	private SerializationHelper helper = null;
	/**
	 * Class loader used for dynamic loading and updating of client classes.
	 */
	private JPPFClassLoader classLoader = null;
	/**
	 * The unique identifier for the submitting application.
	 */
	private String appUuid = null;

	/**
	 * Initialize this container with a specified application uuid.
	 * @param appUuid the unique identifier of a submitting application.
	 * @throws Exception if an error occurs while initializing.
	 */
	public JPPFContainer(String appUuid) throws Exception
	{
		this.appUuid = appUuid;
		init();
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void init() throws Exception
	{
		initHelper();
	}
	
	/**
	 * Perform the deserialization of a single object from a data output stream.
	 * @param dis the stream to deserialize from.
	 * @param compressed determine whether the object must be decompressed first.
	 * @return the deserializ object
	 * @throws Exception if an error occurs while deserializing.
	 */
	public Object deserializeObject(DataInputStream dis, boolean compressed) throws Exception
	{
		return helper.readNextObject(dis, compressed);
	}

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	private JPPFClassLoader getClassLoader()
	{
		if (classLoader == null)
		{
			log.debug("Creating new class loader with appUuid="+appUuid);
			classLoader = new JPPFClassLoader(NodeLauncher.getJPPFClassLoader(), appUuid);
		}
		return classLoader;
	}
	
	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @throws Exception if an error occcurs while instantiating the class loader.
	 */
	private void initHelper() throws Exception
	{
		Class c = getClassLoader().loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
		Object o = c.newInstance();
		helper = (SerializationHelper) o;
	}
	
	/**
	 * Get the unique identifier for the submitting application.
	 * @return the application uuid as a string.
	 */
	public String getAppUuid()
	{
		return appUuid;
	}

	/**
	 * Set the unique identifier for the submitting application.
	 * @param appUuid the application uuid as a string.
	 */
	public void setAppUuid(String appUuid)
	{
		this.appUuid = appUuid;
	}
}
