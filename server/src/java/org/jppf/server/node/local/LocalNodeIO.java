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

import static java.nio.channels.SelectionKey.*;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.io.*;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.MultipleBuffersInputStream;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class LocalNodeIO extends AbstractNodeIO
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(LocalNodeIO.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The underlying socket wrapper.
	 */
	private SocketWrapper socketWrapper = null;

	/**
	 * Initialize this TaskIO with the specified node. 
	 * @param node - the node who owns this TaskIO.
	 */
	public LocalNodeIO(JPPFNode node)
	{
		super(node);
		this.socketWrapper = node.getSocketWrapper();
		this.ioHandler = ((JPPFLocalNode) node).getHandler();
	}

	/**
	 * Performs the actions required if reloading the classes is necessary.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.node.AbstractNodeIO#handleReload()
	 */
	protected void handleReload() throws Exception
	{
		node.setClassLoader(null);
		node.initHelper();
		socketWrapper.setSerializer(node.getHelper().getSerializer());
	}

	/**
	 * {@inheritDoc}.
	 */
	protected Object[] deserializeObjects() throws Exception
	{
		if (debugEnabled) log.debug("waiting for next request");
		LocalNodeWrapperHandler wrapper = (LocalNodeWrapperHandler) ioHandler;
		wrapper.setReadyOps(OP_WRITE);
		while (wrapper.getMessage() == null) wrapper.goToSleep();
		LocalNodeMessage message = wrapper.getMessage();
		DataLocation location = message.getLocations().get(0);
		if (debugEnabled) log.debug("got bundle");
		byte[] data = JPPFDataTransformFactory.transform(false, location.getInputStream());
		JPPFTaskBundle bundle = (JPPFTaskBundle) node.getHelper().getSerializer().deserialize(data);
		return deserializeObjects(bundle);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeResults(JPPFTaskBundle bundle, List<JPPFTask> tasks) throws Exception
	{
		LocalNodeWrapperHandler wrapper = (LocalNodeWrapperHandler) ioHandler;
		wrapper.setMessage(null);
		ExecutorService executor = node.getExecutionManager().getExecutor();
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		List<Future<DataLocation>> futureList = new ArrayList<Future<DataLocation>>();
		futureList.add(executor.submit(new ObjectSerializationTask(bundle)));
		for (JPPFTask task : tasks) futureList.add(executor.submit(new ObjectSerializationTask(task)));
		LocalNodeContext ctx = wrapper.getChannel();
		if (ctx.getNodeMessage() == null) ctx.setNodeMessage(ctx.newMessage(), wrapper);
		for (Future<DataLocation> f: futureList)
		{
			DataLocation location = f.get();
			ctx.getNodeMessage().addLocation(location);
		}
		wrapper.setReadyOps(OP_READ);
		while (ctx.getNodeMessage() != null) wrapper.goToSleep();
		//ioHandler.flush();
	}

	/**
	 * The goal of this class is to serialize an object before sending it back to the server,
	 * and catch an eventual exception.
	 */
	protected class ObjectSerializationTask implements Callable<DataLocation>
	{
		/**
		 * The data to send over the network connection.
		 */
		private Object object = null;

		/**
		 * Initialize this task with the psecicfied data buffer.
		 * @param object the object to serialize.
		 */
		public ObjectSerializationTask(Object object)
		{
			this.object = object;
		}

		/**
		 * Execute this task.
		 * @return the serialized object.
		 */
		public DataLocation call()
		{
			BufferList data = null;
			int p = (object instanceof JPPFTask) ? ((JPPFTask) object).getPosition() : -1;
			try
			{
				if (debugEnabled) log.debug("before serialization of object at position " + p);
				data = serialize(object);
				if (debugEnabled) log.debug("serialized object at position " + p);
			}
			catch(Throwable t)
			{
				data = null;
				log.error(t.getMessage(), t);
				try
				{
					JPPFExceptionResult result = new JPPFExceptionResult(t, object);
					object = null;
					result.setPosition(p);
					data = serialize(result);
				}
				catch(Exception e2)
				{
					log.error(e2.getMessage(), e2);
				}
			}
			object = null;
			DataLocation location = null;
			try
			{
				location = IOHelper.createDataLocationMemorySensitive(data.second());
				InputStream is = new MultipleBuffersInputStream(data.first());
				InputSource source = new StreamInputSource(is);
				location.transferFrom(source, true);
				int i=0;
			}
			catch(Throwable t)
			{
				log.error(t.getMessage(), t);
			}
			return location;
		}
	}
}
