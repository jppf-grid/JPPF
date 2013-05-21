/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.nio.classloader;

import static org.jppf.utils.StringUtils.build;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;

import org.jppf.classloader.*;
import org.jppf.classloader.JPPFResourceWrapper.State;
import org.jppf.io.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.nio.classloader.node.NodeClassNioServer;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Context object associated with a socket channel used by the class server of the JPPF driver.
 * @author Laurent Cohen
 */
public class ClassContext extends SimpleNioContext<ClassState>
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClassContext.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The resource read from or written to the associated channel.
   */
  protected JPPFResourceWrapper resource = null;
  /**
   * The list of pending resource requests for a resource provider.
   */
  private final Queue<ResourceRequest> pendingRequests = new ConcurrentLinkedQueue<ResourceRequest>();
  //private final List<ResourceRequest> pendingRequests = new ArrayList<ResourceRequest>();
  /**
   * The list of pending resource responses for a node.
   */
  private final Map<JPPFResourceWrapper, ResourceRequest> pendingResponses = new HashMap<JPPFResourceWrapper, ResourceRequest>();
  /**
   * The request currently processed.
   */
  protected AtomicReference<ResourceRequest> currentRequest = new AtomicReference<ResourceRequest>(null);
  /**
   * Determines whether this context relates to a provider or node connection.
   */
  protected boolean provider = false;
  /**
   * Contains the JPPF peer identifier written the socket channel.
   */
  private NioObject nioObject = null;
  /**
   * Used to synchronize pending responses performed by multiple threads.
   */
  private final Lock lockResponse = new ReentrantLock();
  /**
   * Used to synchronize pending requests performed by multiple threads.
   */
  private final Lock lockRequest = new ReentrantLock();
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();

  @Override
  public boolean setState(final ClassState state) {
    ClassState oldState = this.state;
    boolean b = super.setState(state);
    if (ClassState.IDLE_PROVIDER.equals(state))
    {
      processRequests();
      return false;
    }
    else if (ClassState.IDLE_NODE.equals(state))
    {
      synchronized(getChannel())
      {
        getChannel().notifyAll();
      }
    }
    return b;
  }

  /**
   * Deserialize a resource wrapper from an array of bytes.
   * @return a <code>JPPFResourceWrapper</code> instance.
   * @throws Exception if an error occurs while deserializing.
   */
  public JPPFResourceWrapper deserializeResource() throws Exception
  {
    ObjectSerializer serializer = new ObjectSerializerImpl();
    DataLocation dl = ((BaseNioMessage) message).getLocations().get(0);
    resource = (JPPFResourceWrapper) IOHelper.unwrappedData(dl, serializer);
    return resource;
  }

  /**
   * Serialize a resource wrapper to an array of bytes.
   * @throws Exception if an error occurs while serializing.
   */
  public void serializeResource() throws Exception
  {
    ObjectSerializer serializer = new ObjectSerializerImpl();
    DataLocation location = IOHelper.serializeData(resource, serializer);
    message = new BaseNioMessage(sslHandler != null);
    ((BaseNioMessage) message).addLocation(location);
  }

  /**
   * Write the peer initiation message when first connecting to a remote server.
   * The message is made of the JPPF identifier {@link JPPFIdentifiers#NODE_CLASSLOADER_CHANNEL} plus
   * an initial resource wrapper (preceded by its serialized length).
   * @param channel the channel to which to write the message.
   * @return <code>true</code> if the message was fully written, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  public boolean writeIdentifier(final ChannelWrapper<?> channel) throws Exception
  {
    if (nioObject == null)
    {
      byte[] bytes = SerializationUtils.writeInt(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL);
      DataLocation dl = new MultipleBuffersLocation(new JPPFBuffer(bytes, 4));
      if (sslHandler == null) nioObject = new PlainNioObject(channel, dl, false);
      else nioObject = new SSLNioObject(dl, sslHandler);
    }
    boolean b = nioObject.write();
    if (b  && debugEnabled) log.debug(build("sent channel identifier ", JPPFIdentifiers.asString(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL), " to peer server"));
    return b;
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception e)
  {
    if (isProvider())
    {
      ClientClassNioServer.closeConnection(channel);
      handleProviderError();
    }
    else NodeClassNioServer.closeConnection(channel);
  }

  /**
   * Get the resource read from or written to the associated channel.
   * @return the resource a <code>JPPFResourceWrapper</code> instance.
   */
  public JPPFResourceWrapper getResource()
  {
    return resource;
  }

  /**
   * Set the resource read from or written to the associated channel.
   * @param resource a <code>JPPFResourceWrapper</code> instance.
   */
  public void setResource(final JPPFResourceWrapper resource)
  {
    this.resource = resource;
  }

  /**
   * Add a new pending request to this resource provider.
   * @param request the request as a <code>SelectionKey</code> instance.
   */
  @SuppressWarnings("unchecked")
  public void addRequest(final ResourceRequest request)
  {
    pendingRequests.offer(request);
    processRequests();
  }

  /**
   * Ensure the pending requests are processed.
   */
  private void processRequests()
  {
    // if requests are already being processed, no need to do anything
    if (lockRequest.tryLock())
    {
      try
      {
        if (ClassState.IDLE_PROVIDER.equals(getState()) && (currentRequest.get() == null) && (getNbPendingRequests() > 0))
        {
          if (debugEnabled) log.debug("state changing from {} to {} for {}", new Object[] {ClassState.IDLE_PROVIDER, ClassState.SENDING_PROVIDER_REQUEST, this});
          driver.getClientClassServer().getTransitionManager().transitionChannel(getChannel(), ClassTransition.TO_SENDING_PROVIDER_REQUEST);
        }
      }
      finally
      {
        lockRequest.unlock();
      }
    }
  }

  /**
   * Get a pending request if any is present.
   * @return a {@link ResourceRequest} instance.
   */
  public ResourceRequest pollPendingRequest() {
    return pendingRequests.poll();
  }

  /**
   * Get the set of pending resource requests for a node.
   * @return a {@link List} of {@link ResourceRequest} instances.
   */
  protected /*synchronized*/ List<ResourceRequest> getPendingRequests() {
    return new ArrayList<ResourceRequest>(pendingRequests);
  }

  /**
   * Get the request currently processed.
   * @return a <code>SelectionKey</code> instance.
   */
  public ResourceRequest getCurrentRequest()
  {
    return currentRequest.get();
  }

  /**
   * Set the request currently processed.
   * @param currentRequest a <code>SelectionKey</code> instance.
   */
  public void setCurrentRequest(final ResourceRequest currentRequest)
  {
    this.currentRequest.set(currentRequest);
  }

  /**
   * Get the number of pending resource requests for a resource provider.
   * @return a the number of requests as an int.
   */
  public int getNbPendingRequests()
  {
    //return pendingRequests.size() + (getCurrentRequest() == null ? 0 : 1);
    return pendingRequests.size();
  }

  /**
   * Determine whether this context relates to a provider or node connection.
   * @return true if this is a provider context, false otherwise.
   */
  public boolean isProvider()
  {
    return provider;
  }

  /**
   * Specify whether this context relates to a provider or node connection.
   * @param provider true if this is a provider context, false otherwise.
   */
  public void setProvider(final boolean provider)
  {
    this.provider = provider;
  }

  /**
   * Handle the scenario where an exception occurs while sending a request to
   * or receiving a response from a provider, and a node channel is waiting for the response.
   */
  protected void handleProviderError()
  {
    try
    {
      ResourceRequest currentRequest;
      List<ResourceRequest> pendingList;
      synchronized (this)
      {
        currentRequest = getCurrentRequest();
        pendingList = new ArrayList<ResourceRequest>(pendingRequests);
        if (currentRequest != null)
        {
          pendingList.add(currentRequest);
          setCurrentRequest(null);
        }
        pendingRequests.clear();
      }

      if (!pendingList.isEmpty())
      {
        if (debugEnabled) log.debug(build("provider: ", getChannel(), " sending null response(s) for disconnected provider"));
        ClassNioServer server = driver.getNodeClassServer();
        Set<ChannelWrapper<?>> nodeSet = new HashSet<ChannelWrapper<?>>();
        for (ResourceRequest resourceRequest : pendingList)
        {
          ChannelWrapper<?> nodeChannel = resourceRequest.getChannel();
          if (!nodeSet.contains(nodeChannel)) nodeSet.add(nodeChannel);
          resourceRequest.getResource().setState(State.NODE_RESPONSE_ERROR);
        }
        for (ChannelWrapper<?> nodeChannel: nodeSet) resetNodeState(nodeChannel, server);
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Reset the state of the requesting node channel, after an error
   * occurred on the provider which attempted to provide a response.
   * @param request the requesting node channel.
   * @param server the server handling the node.
   */
  private void resetNodeState(final ResourceRequest request, final ClassNioServer server)
  {
    try
    {
      if (debugEnabled) log.debug(build("resetting channel state for node ", request));
      request.getResource().setState(State.NODE_RESPONSE_ERROR);
      server.getTransitionManager().transitionChannel(request.getChannel(), ClassTransition.TO_NODE_WAITING_PROVIDER_RESPONSE);
      server.getTransitionManager().submitTransition(request.getChannel());
    }
    catch (Exception e)
    {
      log.error(build("error while trying to send response to node ", request, ", this node may be unavailable"), e);
    }
  }

  /**
   * Reset the state of the requesting node channel, after an error
   * occurred on the provider which attempted to provide a response.
   * @param channel the requesting node channel to reset.
   * @param server the server handling the node.
   */
  private void resetNodeState(final ChannelWrapper<?> channel, final ClassNioServer server)
  {
    try
    {
      if (debugEnabled) log.debug(build("resetting channel state for node ", channel));
      server.getTransitionManager().transitionChannel(channel, ClassTransition.TO_NODE_WAITING_PROVIDER_RESPONSE);
      server.getTransitionManager().submitTransition(channel);
    }
    catch (Exception e)
    {
      log.error(build("error while trying to send response to node ", channel, ", this node may be unavailable"), e);
    }
  }

  /**
   * Get the set of pending resource responses for a node.
   * @return a {@link Map} of {@link ResourceRequest} instances.
   */
  public Map<JPPFResourceWrapper, ResourceRequest> getPendingResponses()
  {
    lockResponse.lock();
    try {
      return new HashMap<JPPFResourceWrapper, ResourceRequest>(pendingResponses);
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Add a pending response.
   * @param resource the requets resource.
   * @param request the request.
   */
  public void addPendingResponse(final JPPFResourceWrapper resource, final ResourceRequest request)
  {
    lockResponse.lock();
    try {
      pendingResponses.put(resource, request);
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Remove the specified pending responses.
   * @param toRemove the pending responses to remove.
   */
  public void removePendingResponses(final Collection<JPPFResourceWrapper> toRemove)
  {
    lockResponse.lock();
    try {
      for (JPPFResourceWrapper resource: toRemove) pendingResponses.remove(resource);
      toRemove.clear();
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Get the number of pending responses.
   * @return the number of pending responses as an int.
   */
  public int getNbPendingResponses() {
    lockResponse.lock();
    try {
      return pendingResponses.size();
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Determine whether this context has a pending response.
   * @return <code>true</code> if there is at least one pending response, <code>false</code> otherwise.
   */
  public boolean hasPendingResponse()
  {
    lockResponse.lock();
    try {
      return !pendingResponses.isEmpty();
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Get the pending responce for the specified resource.
   * @param resource the resource to lookup.
   * @return a {@link ResourceRequest} instance.
   */
  public ResourceRequest getPendingResponse(final JPPFResourceWrapper resource) {
    lockResponse.lock();
    try {
      return pendingResponses.get(resource);
    } finally {
      lockResponse.unlock();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("channel=").append(channel.getClass().getSimpleName()).append("[id=").append(channel.getId()).append(']');
    sb.append(", state=").append(getState());
    sb.append(", resource=").append(resource == null ? "null" : resource.getName());
    if (provider) {
      sb.append(", pendingRequests=").append(getNbPendingRequests());
      sb.append(", currentRequest=").append(getCurrentRequest());
      sb.append(", connectionUuid=").append(connectionUuid);
      sb.append(", type=client");
    } else {
      if (lockResponse.tryLock()) {
        try {
          sb.append(", pendingResponses=").append(pendingResponses.size());
        } finally {
          lockResponse.unlock();
        }
      }
      sb.append(", type=node");
    }
    sb.append(", peer=").append(peer);
    sb.append(", uuid=").append(uuid);
    return sb.toString();
  }
}
