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

package org.jppf.server.nio.nodeserver;

import java.nio.channels.SocketChannel;
import java.util.List;

import org.jppf.io.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.NioContext;
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
	 * The message wrapping the data sent or received over the socket channel.
	 */
	private NodeMessage nodeMessage = null;
	/**
	 * The task bundle to send or receive.
	 */
	private BundleWrapper bundle = null;
	/**
	 * Bundler used to schedule tasks for the corresponding node.
	 */
	private Bundler bundler = null;
	/**
	 * Helper used to serialize the bundle objects.
	 */
	private SerializationHelper helper = new SerializationHelperImpl();
	/**
	 * Determines whether this context is attached to a peer node.
	 */
	private boolean peer = false;
	/**
	 * The uuid of the corresponding node.
	 */
	private String nodeUuid = null;

	/**
	 * Get the task bundle to send or receive.
	 * @return a <code>BundleWrapper</code> instance.
	 */
	public BundleWrapper getBundle()
	{
		return bundle;
	}

	/**
	 * Set the task bundle to send or receive.
	 * @param bundle a <code>JPPFTaskBundle</code> instance.
	 */
	public void setBundle(BundleWrapper bundle)
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
	public void resubmitBundle(BundleWrapper bundle)
	{
		bundle.getBundle().setPriority(10);
		JPPFDriver.getQueue().addBundle(bundle);
	}

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 * @see org.jppf.server.nio.NioContext#handleException(java.nio.channels.SocketChannel)
	 */
	public void handleException(SocketChannel channel)
	{
		if (getBundler() != null) getBundler().dispose();
		NodeNioServer.closeNode(channel, this);
		if ((bundle != null) && !JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getBundle().getState()))
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
		if (nodeMessage == null) nodeMessage = new NodeMessage();
		JPPFBuffer buf = helper.getSerializer().serialize(bundle.getBundle());
		nodeMessage.addLocation(new ByteBufferLocation(buf.getBuffer(), 0, buf.getLength()));
		nodeMessage.addLocation(bundle.getDataProvider());
		for (DataLocation dl: bundle.getTasks()) nodeMessage.addLocation(dl);
	}

	/**
	 * Deserialize a task bundle from the message read into this buffer.
	 * @return a <code>BundleWrapper</code> instance.
	 * @throws Exception if an error occurs during the deserialization.
	 */
	public BundleWrapper deserializeBundle() throws Exception
	{
		List<DataLocation> locations = nodeMessage.getLocations();
		DataLocation location = locations.get(0);
		byte[] data = new byte[location.getSize()];
		OutputDestination dest = new ByteBufferOutputDestination(data, 0, data.length);
		location.transferTo(dest, true);
		JPPFTaskBundle bundle = (JPPFTaskBundle) helper.getSerializer().deserialize(data);
		BundleWrapper wrapper = new BundleWrapper(bundle);
		if (locations.size() > 1)
		{
			//wrapper.setDataProvider(locations.get(1));
			for (int i=1; i<locations.size(); i++) wrapper.addTask(locations.get(i));
		}
		return wrapper;
	}

	/**
	 * Get the message wrapping the data sent or received over the socket channel.
	 * @return a <code>NodeMessage</code> instance.
	 */
	public NodeMessage getNodeMessage()
	{
		return nodeMessage;
	}

	/**
	 * Set the message wrapping the data sent or received over the socket channel.
	 * @param nodeMessage a <code>NodeMessage</code> instance.
	 */
	public void setNodeMessage(NodeMessage nodeMessage)
	{
		this.nodeMessage = nodeMessage;
	}

	/**
	 * Determine whether this context is attached to a peer node.
	 * @return true if the context is attached to a peer node, false otherwise.
	 */
	public boolean isPeer()
	{
		return peer;
	}

	/**
	 * Specifiy whether this context is attached to a peer node.
	 * @param peer true if the context is to be attached to a peer node, false otherwise.
	 */
	public void setPeer(boolean peer)
	{
		this.peer = peer;
	}

	/**
	 * Get the uuid of the corresponding node.
	 * @return the uuid as a string.
	 */
	public String getNodeUuid()
	{
		return nodeUuid;
	}

	/**
	 * Set the uuid of the corresponding node.
	 * @param nodeUuid the uuid as a string.
	 */
	public void setNodeUuid(String nodeUuid)
	{
		this.nodeUuid = nodeUuid;
	}
}
