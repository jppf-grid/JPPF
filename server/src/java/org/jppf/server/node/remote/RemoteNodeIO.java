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

package org.jppf.server.node.remote;

import static org.jppf.server.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.server.node.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.JPPFBuffer;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class RemoteNodeIO extends AbstractNodeIO
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(RemoteNodeIO.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * The underlying socket wrapper.
	 */
	private SocketWrapper socketWrapper = null;

	/**
	 * Initialize this TaskIO with the specified node. 
	 * @param node - the node who owns this TaskIO.
	 */
	public RemoteNodeIO(JPPFNode node)
	{
		super(node);
		this.socketWrapper = node.getSocketWrapper();
	}

	/**
	 * {@inheritDoc}.
	 */
	protected Object[] deserializeObjects() throws Exception
	{
		if (debugEnabled) log.debug("waiting for next request");
		byte[] data = socketWrapper.receiveBytes(0).getBuffer();
		if (debugEnabled) log.debug("got bundle");
		data = JPPFDataTransformFactory.transform(false, data);
		JPPFTaskBundle bundle = (JPPFTaskBundle) node.getHelper().getSerializer().deserialize(data);
		return deserializeObjects(bundle);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object[] deserializeObjects(JPPFTaskBundle bundle) throws Exception
	{
		List<Object> list = new ArrayList<Object>();
		list.add(bundle);
		try
		{
			bundle.setNodeExecutionTime(System.currentTimeMillis());
			int count = bundle.getTaskCount();
			if (debugEnabled) log.debug("bundle task count = " + count + ", state = " + bundle.getState());
			if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				JPPFContainer cont = node.getContainer(bundle.getUuidPath().getList());
				cont.getClassLoader().setRequestUuid(bundle.getRequestUuid());
				//cont.deserializeObjects(socketWrapper, list, 1+count);
				cont.deserializeObjects(list, 1+count, node.getExecutionManager().getExecutor());
			}
			else
			{
				// skip null data provider
				socketWrapper.receiveBytes(0);
			}
			if (debugEnabled) log.debug("got all data");
		}
		catch(Throwable e)
		{
			log.error("Exception occurred while deserializing the tasks", e);
			bundle.setTaskCount(0);
			bundle.setParameter(NODE_EXCEPTION_PARAM, e);
		}
		return list.toArray(new Object[0]);
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
	 * Write the execution results to the socket stream.
	 * @param bundle the task wrapper to send along.
	 * @param tasks the list of tasks with their result field updated.
	 * @throws Exception if an error occurs while writtng to the socket stream.
	 * @see org.jppf.server.node.NodeIO#writeResults(org.jppf.server.protocol.JPPFTaskBundle, java.util.List)
	 */
	public void writeResults(JPPFTaskBundle bundle, List<JPPFTask> tasks) throws Exception
	{
		ExecutorService executor = node.getExecutionManager().getExecutor();
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		List<Future<BufferList>> futureList = new ArrayList<Future<BufferList>>();
		futureList.add(executor.submit(new ObjectSerializationTask(bundle)));
		for (JPPFTask task : tasks) futureList.add(executor.submit(new ObjectSerializationTask(task)));
		for (Future<BufferList> f: futureList)
		{
			BufferList list = f.get();
			socketWrapper.writeInt(list.second());
			for (JPPFBuffer buf: list.first()) socketWrapper.write(buf.buffer, 0, buf.length);
		}
		//ioHandler.flush();
	}

	/**
	 * The goal of this class is to serialize an object before sending it back to the server,
	 * and catch an eventual exception.
	 */
	protected class ObjectSerializationTask implements Callable<BufferList>
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
		 * @see java.util.concurrent.Callable#call()
		 */
		public BufferList call()
		{
			BufferList data = null;
			int p = (object instanceof JPPFTask) ? ((JPPFTask) object).getPosition() : -1;
			try
			{
				if (traceEnabled) log.trace("before serialization of object at position " + p);
				data = serialize(object);
				if (traceEnabled) log.trace("serialized object at position " + p);
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
			return data;
		}
	}
}
