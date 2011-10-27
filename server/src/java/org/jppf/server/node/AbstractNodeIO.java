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

package org.jppf.server.node;

import static org.jppf.server.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.io.InvalidClassException;
import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.io.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public abstract class AbstractNodeIO implements NodeIO
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AbstractNodeIO.class);
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
	 * Initialize this TaskIO with the specified node.
	 * @param node - the node who owns this TaskIO.
	 */
	public AbstractNodeIO(final JPPFNode node)
	{
		this.node = node;
	}

	/**
	 * Read a task from the socket connection, along with its header information.
	 * @return a pair of <code>JPPFTaskBundle</code> and a <code>List</code> of <code>JPPFTask</code> instances.
	 * @throws Exception if an error is raised while reading the task data.
	 * @see org.jppf.server.node.NodeIO#readTask()
	 */
	@Override
	public Pair<JPPFTaskBundle, List<Task>> readTask() throws Exception
	{
		Object[] result = readObjects();
		currentBundle = (JPPFTaskBundle) result[0];
		List<Task> taskList = new LinkedList<Task>();
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
		return new Pair<JPPFTaskBundle, List<Task>>(currentBundle, taskList);
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
	protected abstract Object[] deserializeObjects(JPPFTaskBundle bundle) throws Exception;

	/**
	 * Write the execution results to the socket stream.
	 * @param bundle the task wrapper to send along.
	 * @param tasks the list of tasks with their result field updated.
	 * @throws Exception if an error occurs while writtng to the socket stream.
	 * @see org.jppf.server.node.NodeIO#writeResults(org.jppf.server.protocol.JPPFTaskBundle, java.util.List)
	 */
	@Override
	public abstract void writeResults(JPPFTaskBundle bundle, List<Task> tasks) throws Exception;

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
		public BufferList(final List<JPPFBuffer> first, final Integer second)
		{
			super(first, second);
		}
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
		public ObjectSerializationTask(final Object object)
		{
			this.object = object;
		}

		/**
		 * Execute this task.
		 * @return the serialized object.
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public DataLocation call()
		{
			ObjectSerializer ser = null;
			DataLocation dl = null;
			int p = (object instanceof JPPFTask) ? ((JPPFTask) object).getPosition() : -1;
			try
			{
				ser = node.getHelper().getSerializer();
				if (traceEnabled) log.trace("before serialization of object at position " + p);
				dl = IOHelper.serializeData(object, ser);
				if (traceEnabled) log.trace("serialized object at position " + p);
			}
			catch(Throwable t)
			{
				log.error(t.getMessage(), t);
				try
				{
					JPPFExceptionResult result = new JPPFExceptionResult(t, object);
					object = null;
					result.setPosition(p);
					dl = IOHelper.serializeData(result, ser);
				}
				catch(Exception e2)
				{
					log.error(e2.getMessage(), e2);
				}
			}
			object = null;
			return dl;
		}
	}
}
