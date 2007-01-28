/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server.node;

import java.util.*;

import org.apache.log4j.Logger;
import org.jppf.node.*;
import org.jppf.utils.SerializationHelper;

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
	private List<String> uuidPath = new ArrayList<String>();

	/**
	 * Initialize this container with a specified application uuid.
	 * @param uuidPath the unique identifier of a submitting application.
	 * @throws Exception if an error occurs while initializing.
	 */
	public JPPFContainer(List<String> uuidPath) throws Exception
	{
		this.uuidPath = uuidPath;
		init();
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public final void init() throws Exception
	{
		initHelper();
	}
	
	/**
	 * Deserialize a number of objects from an array of bytes.
	 * @param data the array pf bytes from which to deserialize.
	 * @param offset the position in the source data at whcih to start reading.
	 * @param compressed deternines whether the source data is comprssed or not.
	 * @param list a list holding the resulting deserialized objects.
	 * @param count the number of objects to deserialize.
	 * @return the new position in the source data after deserialization.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public int deserializeObject(byte[] data, int offset, boolean compressed, List<Object> list, int count) throws Exception
	{
		return helper.fromBytes(data, offset, compressed, list, count);
	}

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	public JPPFClassLoader getClassLoader()
	{
		if (classLoader == null)
		{
			log.debug("Creating new class loader with uuidPath="+uuidPath);
			classLoader = new JPPFClassLoader(NodeLauncher.getJPPFClassLoader(), uuidPath);
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
		return uuidPath.isEmpty() ? null : uuidPath.get(0);
	}

	/**
	 * Set the unique identifier for the submitting application.
	 * @param uuidPath the application uuid as a string.
	 */
	public void setUuidPath(List<String> uuidPath)
	{
		this.uuidPath = uuidPath;
	}
}
