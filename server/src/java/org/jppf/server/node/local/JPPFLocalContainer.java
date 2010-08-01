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
package org.jppf.server.node.local;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.io.DataLocation;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.JPPFContainer;

/**
 * Instances of this class represent dynamic class loading, and serialization/deserialization, capabilities, associated
 * with a specific client application.<br>
 * The application is identified through a unique uuid. This class effectively acts as a container for the classes of
 * a client application, a provides the methods to enable the transport, serialization and deserialization of these classes.
 * @author Laurent Cohen
 */
public class JPPFLocalContainer extends JPPFContainer
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFLocalContainer.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * The I/O handler for this node.
	 */
	private LocalNodeChannel channel = null;

	/**
	 * Initialize this container with a specified application uuid.
	 * @param channel the I/O channelof the node.
	 * @param uuidPath the unique identifier of a submitting application.
	 * @param classLoader the class loader for this container.
	 * @throws Exception if an error occurs while initializing.
	 */
	public JPPFLocalContainer(LocalNodeChannel channel, List<String> uuidPath, AbstractJPPFClassLoader classLoader) throws Exception
	{
		super(uuidPath, classLoader);
		this.channel = channel;
	}

	/**
	 * Deserialize a number of objects from a socket client.
	 * @param list a list holding the resulting deserialized objects.
	 * @param count the number of objects to deserialize.
	 * @param executor the number of objects to deserialize.
	 * @return the new position in the source data after deserialization.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public int deserializeObjects(List<Object> list, int count, ExecutorService executor) throws Exception
	{
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try
		{
			Thread.currentThread().setContextClassLoader(classLoader);
			LocalNodeMessage message = channel.getNodeResource();
			List<DataLocation> locations = message.getLocations();
			List<Future<Object>> futureList = new ArrayList<Future<Object>>();
			for (int i=0; i<count; i++)
			{
				futureList.add(executor.submit(new ObjectDeserializationTask(locations.get(i+1), i)));
			}
			for (Future<Object> f: futureList) list.add(f.get());
			return 0;
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(cl);
		}
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
		private DataLocation location = null;
		/**
		 * Index of the object to deserialize in the incoming IO message; used for debugging purposes.
		 */
		private int index = 0;

		/**
		 * Initialize this task with the specicfied data buffer.
		 * @param location the data read from the network connection.
		 * @param index index of the object to deserialize in the incoming IO message; used for debugging purposes.
		 */
		public ObjectDeserializationTask(DataLocation location, int index)
		{
			this.location = location;
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
				InputStream is = location.getInputStream();
				if (traceEnabled) log.debug("deserializing object index = " + index);
				byte[] buffer = JPPFDataTransformFactory.transform(false, is);
				Object o = helper.getSerializer().deserialize(buffer);
				if (traceEnabled) log.debug("deserialized object index = " + index);
				return o;
			}
			catch(Exception e)
			{
				log.error(e.getMessage() + " [object index: " + index + "]", e);
			}
			finally
			{
				Thread.currentThread().setContextClassLoader(cl);
			}
			return null;
		}
	}
}
