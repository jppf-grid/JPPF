/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.*;

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
  private static Logger log = LoggerFactory.getLogger(ClassContext.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The resource read from or written to the associated channel.
   */
  protected JPPFResourceWrapper resource = null;
  /**
   * The list of pending resource requests for a resource provider.
   */
  private final List<ResourceRequest> pendingRequests = new ArrayList<ResourceRequest>();
  /**
   * The list of pending resource reponses for a node.
   */
  protected Map<JPPFResourceWrapper, ResourceRequest> pendingResponses = new HashMap<JPPFResourceWrapper, ResourceRequest>();
  /**
   * The request currently processed.
   */
  protected ResourceRequest currentRequest = null;
  /**
   * Determines whether this context relates to a provider or node connection.
   */
  protected boolean provider = false;
  /**
   * Contains the JPPF peer identifier written the socket channel.
   */
  private NioObject nioObject = null;
  /**
   * The lock used to synchronize between node and client class loader channel.
   */
  private final Object lock = new Object();

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
    if (b  && debugEnabled) log.debug("sent channel identifier " + JPPFIdentifiers.asString(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL) + " to peer server");
    //Thread.sleep(500L);
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
  public synchronized void addRequest(final ResourceRequest request)
  {
    pendingRequests.add(request);
    if (ClassState.IDLE_PROVIDER.equals(state))
    {
      JPPFDriver.getInstance().getClientClassServer().getTransitionManager().transitionChannel(getChannel(), ClassTransition.TO_SENDING_PROVIDER_REQUEST);
      if (debugEnabled) log.debug("node " + request + " transitioned provider " + getChannel());
    }
  }

  public synchronized ResourceRequest pollPendingRequest() {
    if(pendingRequests.isEmpty()) return null;
    else return pendingRequests.remove(0);
  }

  /**
   * Get the request currently processed.
   * @return a <code>SelectionKey</code> instance.
   */
  public synchronized ResourceRequest getCurrentRequest()
  {
    return currentRequest;
  }

  /**
   * Set the request currently processed.
   * @param currentRequest a <code>SelectionKey</code> instance.
   */
  public synchronized void setCurrentRequest(final ResourceRequest currentRequest)
  {
    this.currentRequest = currentRequest;
  }

  /**
   * Get the number of pending resource requests for a resource provider.
   * @return a the number of requests as an int.
   */
  public synchronized int getNbPendingRequests()
  {
    return pendingRequests.size() + (getCurrentRequest() == null ? 0 : 1);
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
   * Handle the scenario where an exception occurs while sendinf a request to
   * or receiving a response from a provider, and a node channel is wating for the response.
   */
  protected void handleProviderError()
  {
    try
    {
      ResourceRequest currentRequest = getCurrentRequest();
      List<ResourceRequest> pendingList;
      synchronized (this)
      {
        if(pendingRequests.isEmpty())
          pendingList = Collections.emptyList();
        else {
          pendingList = new ArrayList<ResourceRequest>(pendingRequests);
          pendingRequests.clear();
        }
      }
      if ((currentRequest != null) || !pendingList.isEmpty())
      {
        if (debugEnabled) log.debug("provider: " + getChannel() + " sending null response(s) for disconnected provider");
        //ClassNioServer server = JPPFDriver.getInstance().getClientClassServer();
        ClassNioServer server = JPPFDriver.getInstance().getNodeClassServer();
        if (currentRequest != null)
        {
          setCurrentRequest(null);
          resetNodeState(currentRequest, server);
        }

        for (ResourceRequest resourceRequest : pendingList) {
          resetNodeState(resourceRequest, server);
        }
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
  public void resetNodeState(final ResourceRequest request, final ClassNioServer server)
  {
    try
    {
      if (debugEnabled) log.debug("resetting channel state for node " + request);
      request.getResource().setState(State.NODE_RESPONSE);
      server.getTransitionManager().transitionChannel(request.getChannel(), ClassTransition.TO_NODE_WAITING_PROVIDER_RESPONSE);
      server.getTransitionManager().submitTransition(request.getChannel());
    }
    catch (Exception e)
    {
      log.error("error while trying to send response to node " + request + ", this node may be unavailable", e);
    }
  }

  /**
   * Get the set of pending resource reponses for a node.
   * @return a {@link Map} of {@link ResourceRequest} instances.
   */
  public Map<JPPFResourceWrapper, ResourceRequest> getPendingResponses()
  {
    return pendingResponses;
  }

  /**
   * Get the lock used to synchronize between node and client class loader channel.
   * @return an {@link Object}.
   */
  public Object getLock()
  {
    return lock;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("channel=").append(channel.getClass().getSimpleName()).append("[id=").append(channel.getId()).append(']');
    sb.append(", state=").append(getState());
    sb.append(", resource=").append(resource);
    sb.append(", pendingRequests=").append(pendingRequests);
    sb.append(", pendingResponses=").append(pendingResponses);
    sb.append(", currentRequest=").append(currentRequest);
    sb.append(", provider=").append(provider);
    sb.append(", peer=").append(peer);
    sb.append(", uuid=").append(uuid);
    sb.append(", connectionUuid=").append(connectionUuid);
    return sb.toString();
  }
}
