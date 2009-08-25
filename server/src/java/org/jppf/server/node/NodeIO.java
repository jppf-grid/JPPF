/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.commons.logging.*;
import org.jppf.comm.socket.SocketWrapper;
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
	private static Log log = LogFactory.getLog(JPPFNode.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
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
		int size = socketWrapper.readInt();
		if (debugEnabled) log.debug("skipped 4 bytes - start reading bundle data");
		byte[] data = socketWrapper.receiveBytes(0).getBuffer();
		if (debugEnabled) log.debug("got bundle");
		JPPFTaskBundle bundle = (JPPFTaskBundle) node.getHelper().getSerializer().deserialize(data);
		bundle.setParameter("initial.data.size", size);
		List<Object> list = new ArrayList<Object>();
		list.add(bundle);
		try
		{
			bundle.setNodeExecutionTime(System.currentTimeMillis());
			int count = bundle.getTaskCount();
			if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				JPPFContainer cont = node.getContainer(bundle.getUuidPath().getList());
				cont.getClassLoader().setRequestUuid(bundle.getRequestUuid());
				cont.deserializeObject(socketWrapper, list, 1+count, size);
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
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		List<JPPFBuffer> list = new ArrayList<JPPFBuffer>();
		ObjectSerializer ser = node.getHelper().getSerializer();
		list.add(ser.serialize(bundle));
		for (JPPFTask task : tasks) list.add(ser.serialize(task));
		int size = 0;
		for (JPPFBuffer buf: list) size += 4 + buf.getLength();
		socketWrapper.writeInt(size);
		for (JPPFBuffer buf: list) socketWrapper.sendBytes(buf);
		socketWrapper.flush();
	}

	/**
	 * Read a task bundle from the socket connection.
	 * @return a <code>JPPFTaskBundle</code> instance.
	 * @throws Exception if an error is raised while reading the task data.
	 */
	public JPPFTaskBundle readBundle() throws Exception
	{
		socketWrapper.skip(4);
		byte[] data = socketWrapper.receiveBytes(0).getBuffer();
		currentBundle = (JPPFTaskBundle) node.getHelper().getSerializer().deserialize(data);
		if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(currentBundle.getState()))
		{
			Runnable r = new Runnable()
			{
				public void run()
				{
					readObjectsAsync();
				}
			};
			new Thread(r).start();
		}
		else
		{
			// skip null data provider
			socketWrapper.receiveBytes(0);
		}
		return currentBundle;
	}

	/**
	 * Deseralize the objects read from the socket, and reload the appropriate classes if any class change is detected.<br>
	 * A class change is triggered when an <code>InvalidClassException</code> is caught. Upon catching this exception,
	 * the class loader is reinitialized and the classes are reloaded.
	 */
	private void readObjectsAsync()
	{
		if (debugEnabled) log.debug("start reading data provider and tasks");
		currentBundle.setNodeExecutionTime(System.currentTimeMillis());
		int count = currentBundle.getTaskCount();
		try
		{
			JPPFContainer cont = node.getContainer(currentBundle.getUuidPath().getList());
			cont.getClassLoader().setRequestUuid(currentBundle.getRequestUuid());
			for (int i=0; i<count+1; i++)
			{
				Object o = cont.deserializeObject(socketWrapper);
				addObject(o);
			}
		}
		catch(ClassNotFoundException e)
		{
			log.error("Exception occurred while deserializing the tasks", e);
			currentBundle.setTaskCount(0);
			currentBundle.setParameter(NODE_EXCEPTION_PARAM, e);
		}
		catch(NoClassDefFoundError e)
		{
			log.error("Exception occurred while deserializing the tasks", e);
			currentBundle.setTaskCount(0);
			currentBundle.setParameter(NODE_EXCEPTION_PARAM, e);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		if (debugEnabled) log.debug("end reading data provider and tasks");
	}

	/**
	 * Add an object (dtaa provider or task) to the list of objects read from the socket stream.
	 * @param o - the object ot add to the list.
	 */
	public synchronized void addObject(Object o)
	{
		readList.add(o);
		wakeUp();
	}

	/**
	 * Get the next object (dtaa provider or task) read from the socket stream.
	 * @return an object read from the socket connection.
	 */
	public synchronized Object nextObject()
	{
		if (readList.isEmpty()) goToSleep();
		return readList.removeFirst();
	}
}
