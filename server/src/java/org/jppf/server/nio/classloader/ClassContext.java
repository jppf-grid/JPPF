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

import java.util.List;

import org.jppf.classloader.JPPFResourceWrapper;
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
   * Determines whther DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The resource read from or written to the associated channel.
   */
  protected JPPFResourceWrapper resource = null;
  /**
   * The list of pending resource requests for a resource provider.
   */
  protected List<ChannelWrapper<?>> pendingRequests = null;
  /**
   * The request currently processed.
   */
  protected ChannelWrapper<?> currentRequest = null;
  /**
   * Determines whether this context relates to a provider or node connection.
   */
  protected boolean provider = false;
  /**
   * Contains the JPPF peer identifier written the socket channel.
   */
  private NioObject nioObject = null;

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
    message = new BaseNioMessage(sslEngineManager != null);
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
      if (sslEngineManager == null) nioObject = new PlainNioObject(channel, dl, false);
      else nioObject = new SSLNioObject(dl, sslEngineManager);
    }
    boolean b = nioObject.write();
    if (b  && debugEnabled) log.debug("sent channel identifier " + JPPFIdentifiers.asString(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL) + " to peer server");
    //Thread.sleep(500L);
    return b;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleException(final ChannelWrapper<?> channel)
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
  public synchronized void addRequest(final ChannelWrapper<?> request)
  {
    pendingRequests.add(request);
    if (ClassState.IDLE_PROVIDER.equals(state))
    {
      JPPFDriver.getInstance().getClientClassServer().getTransitionManager().transitionChannel(getChannel(), ClassTransition.TO_SENDING_PROVIDER_REQUEST);
      if (debugEnabled) log.debug("node " + request + " transitioned provider " + getChannel());
    }
  }

  /**
   * Get the request currently processed.
   * @return a <code>SelectionKey</code> instance.
   */
  public synchronized ChannelWrapper<?> getCurrentRequest()
  {
    return currentRequest;
  }

  /**
   * Set the request currently processed.
   * @param currentRequest a <code>SelectionKey</code> instance.
   */
  public synchronized void setCurrentRequest(final ChannelWrapper<?> currentRequest)
  {
    this.currentRequest = currentRequest;
  }

  /**
   * Get the number of pending resource requests for a resource provider.
   * @return a the number of requests as an int.
   */
  public synchronized int getNbPendingRequests()
  {
    List<ChannelWrapper<?>> reqs = getPendingRequests();
    return (reqs == null ? 0 : reqs.size()) + (getCurrentRequest() == null ? 0 : 1);
  }

  /**
   * Get the list of pending resource requests for a resource provider.
   * @return a <code>List</code> of <code>SelectionKey</code> instances.
   */
  public synchronized List<ChannelWrapper<?>> getPendingRequests()
  {
    return pendingRequests;
  }

  /**
   * Set the list of pending resource requests for a resource provider.
   * @param pendingRequests a <code>List</code> of <code>SelectionKey</code> instances.
   */
  public synchronized void setPendingRequests(final List<ChannelWrapper<?>> pendingRequests)
  {
    this.pendingRequests = pendingRequests;
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
      ChannelWrapper<?> currentRequest = getCurrentRequest();
      if ((currentRequest != null) || !getPendingRequests().isEmpty())
      {
        if (debugEnabled) log.debug("provider: " + getChannel() + " sending null response(s) for disconnected provider");
        ClassNioServer server = JPPFDriver.getInstance().getClientClassServer();
        if (currentRequest != null)
        {
          setCurrentRequest(null);
          resetNodeState(currentRequest, server);
        }
        for (int i=0; i<getPendingRequests().size(); i++) resetNodeState(getPendingRequests().remove(0), server);
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
   * @param request the requestinhg node channel.
   * @param server the server handling the node.
   */
  public void resetNodeState(final ChannelWrapper<?> request, final ClassNioServer server)
  {
    try
    {
      if (debugEnabled) log.debug("resetting channel state for node " + request);
      server.getTransitionManager().transitionChannel(request, ClassTransition.TO_NODE_WAITING_PROVIDER_RESPONSE);
      server.getTransitionManager().submitTransition(request);
    }
    catch (Exception e)
    {
      log.error("error while trying to send response to node " + request + ", this node may be unavailable", e);
    }
  }

  /**
   * Send a null response to a request from a node connection. This method is called for a provider
   * that was disconnected but still has pending requests, so as not to block the requesting channels.
   * @param request the selection key wrapping the requesting channel.
   */
  protected void sendNullResponse(final ChannelWrapper<?> request)
  {
    try
    {
      if (debugEnabled) log.debug("disconnected provider: sending null response to node " + request);
      ClassContext requestContext = (ClassContext) request.getContext();
      requestContext.getResource().setDefinition(null);
      requestContext.serializeResource();
      ClassNioServer server = JPPFDriver.getInstance().getNodeClassServer();
      server.getTransitionManager().transitionChannel(request, ClassTransition.TO_SENDING_NODE_RESPONSE);
    }
    catch (Exception e)
    {
      log.error("error while trying to send response to node " + request + ", this node may be unavailable", e);
    }
  }
}
