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

package org.jppf.server.mina.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.List;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.server.mina.MinaContext;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.*;

/**
 * This class represents the state of waiting for a request from a node.
 * @author Laurent Cohen
 */
class WaitingNodeRequestState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(WaitingNodeRequestState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	public WaitingNodeRequestState(MinaClassServer server)
	{
		super(server);
	}

	/**
	 * End the transition associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.mina.MinaState#endTransition(org.apache.mina.core.session.IoSession)
	 */
	public void endTransition(IoSession session) throws Exception
	{
		ClassContext context = getContext(session);
		if (debugEnabled) log.debug("read resource request from node: session " + session.getId());
		JPPFResourceWrapper resource = context.deserializeResource();
		TraversalList<String> uuidPath = resource.getUuidPath();
		boolean dynamic = resource.isDynamic();
		String name = resource.getName();
		byte[] b = null;
		String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null; 
		ByteTransitionPair p = processNonDynamic(session, resource);
		if (p.second() != null)
		{
			server.transitionSession(session, p.second());
			session.write(context.getMessage());
			return;
		}
		b = p.first();
		if ((b == null) && dynamic)
		{
			p = processDynamic(session, resource);
			if (p.second() != null)
			{
				server.transitionSession(session, p.second());
				if (p.second() != TO_IDLE_NODE) session.write(context.getMessage());
				return;
			}
			//b = p.first();
		}
		if (debugEnabled) log.debug("resource [" + name + "] not found for node: session " + session.getId());
		resource.setDefinition(null);
		context.serializeResource();
		server.transitionSession(session, TO_SENDING_NODE_RESPONSE);
		session.write(context.getMessage());
	}

	/**
	 * Find a provider connection for the specified provider uuid.
	 * @param uuid the uuid for which to find a conenction.
	 * @return a <code>SelectableChannel</code> instance.
	 * @throws Exception if an error occurs while searching for a connection.
	 */
	private IoSession findProviderConnection(String uuid) throws Exception
	{
		IoSession result = null;
		List<IoSession> connections = ((MinaClassServer)server).providerConnections.get(uuid);
		int minRequests = Integer.MAX_VALUE;
		for (IoSession channel: connections)
		{
			ClassContext ctx = getContext(channel);
			int size = ctx.getNbPendingRequests();
			if (size < minRequests)
			{
				minRequests = size;
				result = channel;
			}
		}
		return result;
	}

	/**
	 * Process a request to the driver's resource provider.
	 * @param session - encapsulates the context and channel.
	 * @param resource - the resource request description
	 * @return a pair of an array of bytes and the resulting state transition.
	 * @throws Exception if any error occurs.
	 */
	private ByteTransitionPair processNonDynamic(IoSession session, JPPFResourceWrapper resource) throws Exception
	{
		MinaClassServer server = (MinaClassServer) this.server;
		byte[] b = null;
		ClassTransition t = null;
		String name = resource.getName();
		ClassContext context = getContext(session);
		TraversalList<String> uuidPath = resource.getUuidPath();

		String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null; 
		if (((uuid == null) || uuid.equals(driver.getUuid())) && (resource.getCallable() == null))
		{
			if (resource.getData("multiple") != null)
			{
				List<byte[]> list = server.getResourceProvider().getMultipleResourcesAsBytes(name, null);
				if (list != null)
				{
					resource.setData("resource_list", list);
					context.serializeResource();
					t = TO_SENDING_NODE_RESPONSE;
				}
				if (debugEnabled) log.debug("multiple resources " + (list != null ? "" : "not ") + "found [" + name + "] in driver's classpath for node: session" + session.getId());
			}
			else
			{
				if ((uuid == null) && !resource.isDynamic()) uuid = driver.getUuid();
				if (uuid != null) b = server.getCacheContent(uuid, name);
				boolean alreadyInCache = (b != null);
				if (debugEnabled) log.debug("resource " + (alreadyInCache ? "" : "not ") + "found [" + name + "] in cache for node: session" + session.getId());
				if (!alreadyInCache) b = server.getResourceProvider().getResourceAsBytes(name);
				if ((b != null) || !resource.isDynamic())
				{
					if (debugEnabled) log.debug("resource " + (b == null ? "not " : "") + "found [" + name + "] in the driver's classpath for node: session " + session.getId());
					if ((b != null) && !alreadyInCache) server.setCacheContent(driver.getUuid(), name, b);
					resource.setDefinition(b);
					context.serializeResource();
					t = TO_SENDING_NODE_RESPONSE;
				}
			}
		}
		return new ByteTransitionPair(b, t);
	}

	/**
	 * Process a request to the client's resource provider.
	 * @param key - encapsulates the context and channel.
	 * @param resource - the resource request description
	 * @return a pair of an array of bytes and the resulting state transition.
	 * @throws Exception if any error occurs.
	 */
	private ByteTransitionPair processDynamic(IoSession key, JPPFResourceWrapper resource) throws Exception
	{
		MinaClassServer server = (MinaClassServer) this.server;
		byte[] b = null;
		ClassTransition t = null;
		String name = resource.getName();
		TraversalList<String> uuidPath = resource.getUuidPath();
		ClassContext context = getContext(key);

		if (resource.getCallable() == null) b = server.getCacheContent(uuidPath.getFirst(), name);
		if (b != null)
		{
			if (debugEnabled) log.debug("found cached resource [" + name + "] for node: session " + key.getId());
			resource.setDefinition(b);
			context.serializeResource();
			t = TO_SENDING_NODE_RESPONSE;
		}
		else
		{
			uuidPath.decPosition();
			String uuid = uuidPath.getCurrentElement();
			IoSession provider = findProviderConnection(uuid);
			if (provider != null)
			{
				if (debugEnabled) log.debug("request resource [" + name + "] from client: session " + provider.getId() + " for node: session " + key.getId());
				ClassContext providerContext = getContext(provider);
				providerContext.addRequest(key);
				if (ClassState.IDLE_PROVIDER.equals(providerContext.getState()))
				{
					if (debugEnabled) log.debug("node session " + key.getId() + " changing key ops for provider session " + provider.getId());
					server.transitionSession(provider, TO_SENDING_PROVIDER_REQUEST);
					ClassServerState s = (ClassServerState) server.getFactory().getState(providerContext.getState());
					provider.setAttribute(MinaContext.TRANSITION_STARTED, s.startTransition(provider));
					provider.write(providerContext);
				}
				t = TO_IDLE_NODE;
			}
		}
		return new ByteTransitionPair(b, t);
	}

	/**
	 * A pair of array of bytes and class transition.
	 */
	private static class ByteTransitionPair extends Pair<byte[], ClassTransition>
	{
		/**
		 * Initialize this pair with the specified array of bytes and class transition.
		 * @param first - an array of bytes.
		 * @param second - a class transition.
		 */
		public ByteTransitionPair(byte[] first, ClassTransition second)
		{
			super(first, second);
		}
	}
}

