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

import static org.jppf.server.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.io.InvalidClassException;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.IOHandler;
import org.jppf.data.transform.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public abstract class AbstractNodeIO implements NodeIO
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractNodeIO.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The node who owns this TaskIO.
	 */
	protected JPPFNode node = null;
	/**
	 * The task bundle currently being processed.
	 */
	protected JPPFTaskBundle currentBundle = null;
	/**
	 * Used to serialize/deserialize tasks and data providers.
	 */
	protected ObjectSerializer serializer = null;
	/**
	 * The I/O handler to which read/write operations are delegated.
	 */
	protected IOHandler ioHandler = null;

	/**
	 * Initialize this TaskIO with the specified node. 
	 * @param node - the node who owns this TaskIO.
	 */
	public AbstractNodeIO(JPPFNode node)
	{
		this.node = node;
	}

	/**
	 * Read a task from the socket connection, along with its header information.
	 * @return a pair of <code>JPPFTaskBundle</code> and a <code>List</code> of <code>JPPFTask</code> instances.
	 * @throws Exception if an error is raised while reading the task data.
	 * @see org.jppf.server.node.NodeIO#readTask()
	 */
	public Pair<JPPFTaskBundle, List<JPPFTask>> readTask() throws Exception
	{
		Object[] result = readObjects();
		currentBundle = (JPPFTaskBundle) result[0];
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(currentBundle.getState()) &&
			(currentBundle.getParameter(NODE_EXCEPTION_PARAM) == null))
		{
			DataProvider dataProvider = (DataProvider) result[1];
			for (int i=0; i<currentBundle.getTaskCount(); i++)
			{
				JPPFTask task = (JPPFTask) result[2 + i];
				task.setDataProvider(dataProvider);
				taskList.add(task);
			}
		}
		return new Pair<JPPFTaskBundle, List<JPPFTask>>(currentBundle, taskList);
	}

	/**
	 * Deserialize the objects read from the socket, and reload the appropriate classes if any class change is detected.<br>
	 * A class change is triggered when an <code>InvalidClassException</code> is caught. Upon catching this exception,
	 * the class loader is reinitialized and the class are reloaded.
	 * @return an array of objects deserialized from the socket stream.
	 * @throws Exception if the classes could not be reloaded or an error occurred during deserialization.
	 */
	protected Object[] readObjects() throws Exception
	{
		Object[] result = null;
		boolean reload = false;
		try
		{
			result = deserializeObjects();
		}
		catch(IncompatibleClassChangeError err)
		{
			reload = true;
			if (debugEnabled) log.debug(err.getMessage() + "; reloading classes", err);
		}
		catch(InvalidClassException e)
		{
			reload = true;
			if (debugEnabled) log.debug(e.getMessage() + "; reloading classes", e);
		}
		if (reload)
		{
			if (debugEnabled) log.debug("reloading classes");
			handleReload();
			result = deserializeObjects();
		}
		return result;
	}

	/**
	 * Performs the actions required if reloading the classes is necessary.
	 * @throws Exception if any error occurs.
	 */
	protected abstract void handleReload() throws Exception;

	/**
	 * Perform the deserialization of the objects received through the socket connection.
	 * @return an array of objects deserialized from the socket stream.
	 * @throws Exception if an error occurs while deserializing.
	 */
	protected abstract Object[] deserializeObjects() throws Exception;

	/**
	 * Perform the deserialization of the objects received through the socket connection.
	 * @param bundle the message header that contains information about the tasks and data provider.
	 * @return an array of objects deserialized from the socket stream.
	 * @throws Exception if an error occurs while deserializing.
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
				cont.deserializeObjects(ioHandler, list, 1+count, node.getExecutionManager().getExecutor());
			}
			else
			{
				// skip null data provider
				ioHandler.read();
			}
			if (debugEnabled) log.debug("got all data");
		}
		catch(ClassNotFoundException e)
		{
			log.error("Exception occurred while deserializing the tasks", e);
			bundle.setTaskCount(0);
			bundle.setParameter(NODE_EXCEPTION_PARAM, e);
		}
		catch(NoClassDefFoundError e)
		{
			log.error("Exception occurred while deserializing the tasks", e);
			bundle.setTaskCount(0);
			bundle.setParameter(NODE_EXCEPTION_PARAM, e);
		}
		return list.toArray(new Object[0]);
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
			ioHandler.writeInt(list.second());
			for (JPPFBuffer buf: list.first()) ioHandler.write(buf.buffer, 0, buf.length);
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
			return data;
		}
	}

	/**
	 * Serialize the specified object
	 * @param o the object to serialize.
	 * @return the serialized object as an array of bytes.
	 * @throws Exception if any error occurs.
	 */
	protected BufferList serialize(Object o) throws Exception
	{
		MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
		node.getHelper().getSerializer().serialize(o, mbos);
		List<JPPFBuffer> data = mbos.toBufferList();
		int length = mbos.size();
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform != null)
		{
			MultipleBuffersInputStream mbis = new MultipleBuffersInputStream(mbos.toBufferList());
			mbos = new MultipleBuffersOutputStream();
			transform.wrap(mbis, mbos);
			data = mbos.toBufferList();
			length = mbos.size();
		}
		return new BufferList(data, length);
	}

	/**
	 * A pairing of a list of buffers and the total length of their usable data.
	 */
	protected static class BufferList extends Pair<List<JPPFBuffer>, Integer>
	{
		/**
		 * Iitialize this pairing with the specified list of buffers and length.
		 * @param first the list of buffers.
		 * @param second the total data length.
		 */
		public BufferList(List<JPPFBuffer> first, Integer second)
		{
			super(first, second);
		}
	}
}
