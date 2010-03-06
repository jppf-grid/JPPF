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

package org.jppf.server.nio.nodeserver;

import java.nio.channels.SocketChannel;
import java.util.List;

import org.jppf.data.transform.*;
import org.jppf.io.*;
import org.jppf.server.JPPFDriver;
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
	 * @param bundle a {@link JPPFTaskBundle} instance.
	 */
	public void setBundle(BundleWrapper bundle)
	{
		this.bundle = bundle;
	}

	/**
	 * Get the bundler used to schedule tasks for the corresponding node.
	 * @return a {@link Bundler} instance.
	 */
	public Bundler getBundler()
	{
		return bundler;
	}

	/**
	 * Set the bundler used to schedule tasks for the corresponding node.
	 * @param bundler a {@link Bundler} instance.
	 */
	public void setBundler(Bundler bundler)
	{
		this.bundler = bundler;
	}

	/**
	 * Check whether the bundler held by this context is up to date by comparison
	 * with the specified bundler.<br>
	 * If it is not, then it is replaced with a copy of the specified bundler, with a
	 * timestamp taken at creation time.
	 * @param serverBundler the bundler to compare with.
	 * @return true if the bundler is up to date, false if it wasn't and has been updated.
	 */
	public boolean checkBundler(Bundler serverBundler)
	{
		if (this.bundler.getTimestamp() < serverBundler.getTimestamp())
		{
			this.bundler.dispose();
			this.bundler = serverBundler.copy();
			this.bundler.setup();
			return true;
		}
		return false;
	}

	/**
	 * Resubmit a task bundle at the head of the queue. This method is invoked
	 * when a node is disconnected while it was executing a task bundle.
	 * @param bundle the task bundle to resubmit.
	 */
	public void resubmitBundle(BundleWrapper bundle)
	{
		//bundle.getBundle().setPriority(10);
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
			JPPFDriver.getInstance().getJobManager().jobReturned(bundle, new ChannelWrapper<SocketChannel>(channel));
			resubmitBundle(bundle);
		}
	}

	/**
	 * Serialize this context's bundle into a byte buffer.
	 * @throws Exception if any error occurs.
	 */
	public void serializeBundle() throws Exception
	{
		//if (nodeMessage == null)
		nodeMessage = new NodeMessage();
		byte[] data = helper.getSerializer().serialize(bundle.getBundle()).getBuffer();
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform != null) data = transform.wrap(data);
		nodeMessage.addLocation(new ByteBufferLocation(data, 0, data.length));
		nodeMessage.addLocation(bundle.getDataProvider());
		for (DataLocation dl: bundle.getTasks()) nodeMessage.addLocation(dl);
		nodeMessage.setBundle(bundle.getBundle());
	}

	/**
	 * Deserialize a task bundle from the message read into this buffer.
	 * @return a {@link NodeContext} instance.
	 * @throws Exception if an error occurs during the deserialization.
	 */
	public BundleWrapper deserializeBundle() throws Exception
	{
		List<DataLocation> locations = nodeMessage.getLocations();
		JPPFTaskBundle bundle = nodeMessage.getBundle();
		BundleWrapper wrapper = new BundleWrapper(bundle);
		if (locations.size() > 1)
		{
			for (int i=1; i<locations.size(); i++) wrapper.addTask(locations.get(i));
		}
		return wrapper;
	}

	/**
	 * Get the message wrapping the data sent or received over the socket channel.
	 * @return a {@link NodeMessage NodeMessage} instance.
	 */
	public NodeMessage getNodeMessage()
	{
		return nodeMessage;
	}

	/**
	 * Set the message wrapping the data sent or received over the socket channel.
	 * @param nodeMessage a {@link NodeMessage NodeMessage} instance.
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
