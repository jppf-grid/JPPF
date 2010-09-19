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
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.data.transform.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class NodeIO extends ThreadSynchronization
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeIO.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * The node who owns this TaskIO.
	 */
	private JPPFNode node = null;
	/**
	 * The underlying socket wrapper.
	 */
	private SocketWrapper socketWrapper = null;
	/**
	 * The task bundle currently being processed.
	 */
	private JPPFTaskBundle currentBundle = null;
	/**
	 * Synchronized list of tasks being read from the socket and at the same time consumed by the node for execution.
	 */
	private LinkedList<Object> readList = new LinkedList<Object>();
	/**
	 * Used to serialize/deserialize tasks and data providers.
	 */
	ObjectSerializer serializer = null;
	/**
	 * .
	 */
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * Initialize this TaskIO with the specified node. 
	 * @param node - the node who owns this TaskIO.
	 */
	public NodeIO(JPPFNode node)
	{
		this.node = node;
		this.socketWrapper = node.getSocketWrapper();
	}

	/**
	 * Read a task from the socket connection, along with its header information.
	 * @return a pair of <code>JPPFTaskBundle</code> and a <code>List</code> of <code>JPPFTask</code> instances.
	 * @throws Exception if an error is raised while reading the task data.
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
	private Object[] readObjects() throws Exception
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
			node.setClassLoader(null);
			node.initHelper();
			socketWrapper.setSerializer(node.getHelper().getSerializer());
			result = deserializeObjects();
		}
		return result;
	}

	/**
	 * Perform the deserialization of the objects received through the socket connection.
	 * @return an array of objects deserialized from the socket stream.
	 * @throws Exception if an error occurs while deserializing.
	 */
	private Object[] deserializeObjects() throws Exception
	{
		if (debugEnabled) log.debug("waiting for next request");
		byte[] data = socketWrapper.receiveBytes(0).getBuffer();
		if (debugEnabled) log.debug("got bundle");
		data = JPPFDataTransformFactory.transform(false, data);
		JPPFTaskBundle bundle = (JPPFTaskBundle) node.getHelper().getSerializer().deserialize(data);
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
				cont.deserializeObjects(socketWrapper, list, 1+count, node.getExecutionManager().getExecutor());
			}
			else
			{
				// skip null data provider
				socketWrapper.receiveBytes(0);
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
	 */
	public void writeResults(JPPFTaskBundle bundle, List<JPPFTask> tasks) throws Exception
	{
		ExecutorService executor = node.getExecutionManager().getExecutor();
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		if (debugEnabled) log.debug("serializing " + tasks.size() + " tasks");
		List<Future<BufferList>> futureList = new ArrayList<Future<BufferList>>();
		futureList.add(executor.submit(new ObjectWriteTask(bundle)));
		for (JPPFTask task : tasks) futureList.add(executor.submit(new ObjectWriteTask(task)));
		if (debugEnabled) log.debug("sending " + tasks.size() + " tasks to the server");
		for (Future<BufferList> f: futureList)
		{
			BufferList list = f.get();
			if (traceEnabled) log.trace("task has " + list.first().size() + " buffers, total data size = " + list.second());
			socketWrapper.writeInt(list.second());
			//for (JPPFBuffer buf: list.first())
			for (int j=0; j<list.first().size(); j++)
			{
				JPPFBuffer buf = list.first().get(j);
				if (traceEnabled) log.trace("sending buffer[" + j + "] of size " + buf.length);
				socketWrapper.write(buf.buffer, 0, buf.length);
			}
		}
		socketWrapper.flush();
	}

	/**
	 * The goal of this class is to serialize an object before sending it back to the server,
	 * and catch an eventual exception.
	 */
	public class ObjectWriteTask implements Callable<BufferList>
	{
		/**
		 * The data to send over the network connection.
		 */
		private Object object = null;

		/**
		 * Initialize this task with the psecicfied data buffer.
		 * @param object the object to serialize.
		 */
		public ObjectWriteTask(Object object)
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

		/**
		 * Serialize the specified object
		 * @param o the object to serialize.
		 * @return the serialized object as an array of bytes.
		 * @throws Exception if any error occurs.
		 */
		private BufferList serialize(Object o) throws Exception
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
	}

	/**
	 * A pairing of a list of buffers and the total length of their usable data.
	 */
	private static class BufferList extends Pair<List<JPPFBuffer>, Integer>
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
