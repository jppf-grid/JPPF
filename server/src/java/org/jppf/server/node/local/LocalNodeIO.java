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
import static org.jppf.server.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.io.DataLocation;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.*;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class LocalNodeIO extends AbstractNodeIO
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(LocalNodeIO.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The I/O channel for this node.
	 */
	private LocalNodeChannel channel = null;

	/**
	 * Initialize this TaskIO with the specified node. 
	 * @param node - the node who owns this TaskIO.
	 */
	public LocalNodeIO(JPPFNode node)
	{
		super(node);
		this.channel = ((JPPFLocalNode) node).getChannel();
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
	}

	/**
	 * {@inheritDoc}.
	 */
	protected Object[] deserializeObjects() throws Exception
	{
		channel.setReadyOps(OP_WRITE);
		if (debugEnabled) log.debug("waiting for next request");
		// wait until a message has been sent by the server
		while (channel.getNodeResource() == null) channel.getNodeLock().goToSleep();
		if (debugEnabled) log.debug("got request");
		LocalNodeMessage message = null;
		channel.setReadyOps(0);
		message = channel.getNodeResource();
		DataLocation location = message.getLocations().get(0);
		if (debugEnabled) log.debug("got bundle");
		byte[] data = JPPFDataTransformFactory.transform(false, location.getInputStream());
		JPPFTaskBundle bundle = (JPPFTaskBundle) node.getHelper().getSerializer().deserialize(data);
		Object[] result = deserializeObjects(bundle);
		channel.setNodeResource(null);
		if (debugEnabled) log.debug("got all data");
		return result;
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
				cont.deserializeObjects(list, 1+count, node.getExecutionManager().getExecutor());
			}
			else
			{
				// skip null data provider
				//ioHandler.read();
			}
			if (debugEnabled) log.debug("got all data");
		}
		catch(Throwable t)
		{
			log.error("Exception occurred while deserializing the tasks", t);
			bundle.setTaskCount(0);
			bundle.setParameter(NODE_EXCEPTION_PARAM, t);
		}
		return list.toArray(new Object[0]);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeResults(JPPFTaskBundle bundle, List<JPPFTask> tasks) throws Exception
	{
		if (debugEnabled) log.debug("writing results");
		ExecutorService executor = node.getExecutionManager().getExecutor();
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		List<Future<DataLocation>> futureList = new ArrayList<Future<DataLocation>>();
		futureList.add(executor.submit(new ObjectSerializationTask(bundle)));
		for (JPPFTask task : tasks) futureList.add(executor.submit(new ObjectSerializationTask(task)));
		LocalNodeContext ctx = channel.getChannel();
		LocalNodeMessage message = (LocalNodeMessage) ctx.newMessage();
		for (Future<DataLocation> f: futureList)
		{
			DataLocation location = f.get();
			message.addLocation(location);
		}
		message.setBundle(bundle);
		channel.setServerResource(message);
		channel.setReadyOps(OP_READ);
		if (debugEnabled) log.debug("wrote full results");
		// wait until the message has been read by the server
		while (channel.getServerResource() != null) channel.getServerLock().goToSleep();
		channel.setReadyOps(0);
	}
}
