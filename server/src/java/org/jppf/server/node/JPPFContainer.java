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
package org.jppf.server.node;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.classloader.*;
import org.jppf.comm.socket.*;
import org.jppf.data.transform.*;
import org.jppf.utils.*;

/**
 * Instances of this class represent dynamic class loading, and serialization/deserialization, capabilities, associated
 * with a specific client application.<br>
 * The application is identified through a unique uuid. This class effectively acts as a container for the classes of
 * a client application, a provides the methods to enable the transport, serialization and deserialization of these classes.
 * @author Laurent Cohen
 */
public abstract class JPPFContainer
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFContainer.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Utility for deserialization and serialization.
	 */
	protected SerializationHelper helper = null;
	/**
	 * Class loader used for dynamic loading and updating of client classes.
	 */
	protected AbstractJPPFClassLoader classLoader = null;
	/**
	 * The unique identifier for the submitting application.
	 */
	protected List<String> uuidPath = new ArrayList<String>();

	/**
	 * Initialize this container with a specified application uuid.
	 * @param uuidPath the unique identifier of a submitting application.
	 * @param classLoader the class loader for this container.
	 * @throws Exception if an error occurs while initializing.
	 */
	public JPPFContainer(List<String> uuidPath, AbstractJPPFClassLoader classLoader) throws Exception
	{
		this.uuidPath = uuidPath;
		this.classLoader = classLoader;
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
	 * Deserialize a number of objects from a socket client.
	 * @param wrapper the socket client from which to read the objects to deserialize.
	 * @param list a list holding the resulting deserialized objects.
	 * @param count the number of objects to deserialize.
	 * @return the new position in the source data after deserialization.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public int deserializeObjects(SocketWrapper wrapper, List<Object> list, int count) throws Exception
	{
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try
		{
			Thread.currentThread().setContextClassLoader(classLoader);
			JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
			for (int i=0; i<count; i++)
			{
				JPPFBuffer buf = wrapper.receiveBytes(0);
				byte[] data = (transform == null) ? buf.getBuffer() : JPPFDataTransformFactory.transform(transform, false, buf.getBuffer(), 0, buf.getLength());
				list.add(helper.getSerializer().deserialize(data));
			}
			return 0;
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	/**
	 * Deserialize a number of objects from the I/O channel.
	 * @param list a list holding the resulting deserialized objects.
	 * @param count the number of objects to deserialize.
	 * @param executor the number of objects to deserialize.
	 * @return the new position in the source data after deserialization.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public abstract int deserializeObjects(List<Object> list, int count, ExecutorService executor) throws Exception;

	/**
	 * Deserialize an object from a socket client.
	 * @param data the array of bytes to deserialize into an object.
	 * @return the new position in the source data after deserialization.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public Object deserializeObject(byte[] data) throws Exception
	{
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try
		{
			Thread.currentThread().setContextClassLoader(classLoader);
			return helper.getSerializer().deserialize(data);
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	/**
	 * Get the main class loader for this container.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	public AbstractJPPFClassLoader getClassLoader()
	{
		return classLoader;
	}
	
	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @throws Exception if an error occcurs while instantiating the class loader.
	 */
	protected void initHelper() throws Exception
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

	/**
	 * Instances of this class are used to deserialize objects from an
	 * incoming message in parallel.
	 */
	protected class ObjectDeserializationTask implements Callable<Object>
	{
		/**
		 * The data to send over the network connection.
		 */
		private byte[] buffer = null;
		/**
		 * Index of the object to deserialize in the incoming IO message; used for debugging purposes.
		 */
		private int index = 0;

		/**
		 * Initialize this task with the specicfied data buffer.
		 * @param buffer the data read from the network connection.
		 * @param index index of the object to deserialize in the incoming IO message; used for debugging purposes.
		 */
		public ObjectDeserializationTask(byte[] buffer, int index)
		{
			this.buffer = buffer;
			this.index = index;
		}

		/**
		 * Execute this task.
		 * @return a deserialized object.
		 * @see java.util.concurrent.Callable#call()
		 */
		public Object call()
		{
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try
			{
				Thread.currentThread().setContextClassLoader(getClassLoader());
				if (debugEnabled) log.debug("deserializing object index = " + index);
				buffer = JPPFDataTransformFactory.transform(false, buffer);
				Object o = helper.getSerializer().deserialize(buffer);
				buffer = null;
				if (debugEnabled) log.debug("deserialized object index = " + index);
				return o;
			}
			catch(Exception e)
			{
				log.error(e.getMessage() + " [object index: " + index + "]", e);
			}
			finally
			{
				buffer = null;
				Thread.currentThread().setContextClassLoader(cl);
			}
			return null;
		}
	}
}
