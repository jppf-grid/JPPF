/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.jppf.server.*;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.utils.*;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public class NodeContext extends NioContext<NodeState>
{
	/**
	 * The task bundle to send or receive.
	 */
	private JPPFTaskBundle bundle = null;
	/**
	 * Bundler used to schedule tasks for the corresponding node.
	 */
	private Bundler bundler = null;

	/**
	 * Get the task bundle to send or receive.
	 * @return a <code>JPPFTaskBundle</code> instance.
	 */
	public JPPFTaskBundle getBundle()
	{
		return bundle;
	}

	/**
	 * Set the task bundle to send or receive.
	 * @param bundle a <code>JPPFTaskBundle</code> instance.
	 */
	public void setBundle(JPPFTaskBundle bundle)
	{
		this.bundle = bundle;
	}

	/**
	 * Get the bundler used to schedule tasks for the corresponding node.
	 * @return a <code>Bundler</code> instance.
	 */
	public Bundler getBundler()
	{
		return bundler;
	}

	/**
	 * Set the bundler used to schedule tasks for the corresponding node.
	 * @param bundler a <code>Bundler</code> instance.
	 */
	public void setBundler(Bundler bundler)
	{
		this.bundler = bundler;
	}

	/**
	 * Resubmit a task bundle at the head of the queue. This method is invoked
	 * when a node is disconnected while it was executing a task bundle.
	 * @param bundle the task bundle to resubmit.
	 */
	public void resubmitBundle(JPPFTaskBundle bundle)
	{
		bundle.setPriority(10);
		JPPFDriver.getQueue().addBundle(bundle);
	}

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 * @see org.jppf.server.nio.NioContext#handleException(java.nio.channels.SocketChannel)
	 */
	public void handleException(SocketChannel channel)
	{
		NodeNioServer.closeNode(channel);
		if ((bundle != null) && !JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
		{
			resubmitBundle(bundle);
		}
	}

	/**
	 * Serialize this context's bundle into a byte buffer.
	 * @throws Exception if any error occurs.
	 */
	public void serializeBundle() throws Exception
	{
		SerializationHelper helper = new SerializationHelperImpl();
		byte[] dataProvider = bundle.getDataProvider();
		JPPFBuffer buf = helper.toBytes(bundle, false);
		int size = 4 + buf.getLength();
		if (dataProvider != null) size += 4 + dataProvider.length;
		if (bundle.getTasks() != null)
		{
			for (byte[] task : bundle.getTasks()) size += 4 + task.length;
		}
		byte[] data = new byte[size];
		int pos = helper.copyToBuffer(buf.getBuffer(), data, 0, buf.getLength()); 
		if (dataProvider != null)
		{
			pos = helper.copyToBuffer(dataProvider, data, pos, dataProvider.length);
		}
		if (bundle.getTasks() != null)
		{
			for (byte[] task : bundle.getTasks()) pos = helper.copyToBuffer(task, data, pos, task.length);
		}
		if (message == null) message = new NioMessage();
		message.length = data.length;
		message.buffer = ByteBuffer.wrap(data);
	}

	/**
	 * Deserialize a task bundle from the message read into this buffer.
	 * @return a <code>JPPFTaskBundle</code> instance.
	 * @throws Exception if an error occurs during the deserialization.
	 */
	public JPPFTaskBundle deserializeBundle() throws Exception
	{
		byte[] data = message.buffer.array();
		// reading the bundle as an object
		SerializationHelper helper = new SerializationHelperImpl();
		List<JPPFTaskBundle> list = new ArrayList<JPPFTaskBundle>();
		int pos = helper.fromBytes(data, 0, false, list, 1);
		JPPFTaskBundle bundle = list.get(0);
		List<byte[]> taskList = new ArrayList<byte[]>();
		for (int i = 0; i < bundle.getTaskCount(); i++)
		{
			byte[] task = helper.copyFromBuffer(data, pos);
			pos += 4 + task.length;
			taskList.add(task);
		}
		bundle.setTasks(taskList);
		return bundle;
	}
}
