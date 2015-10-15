/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.server.nio.classloader.client;

import static org.jppf.utils.StringUtils.build;
import static org.jppf.utils.stats.JPPFStatisticsHelper.PEER_OUT_TRAFFIC;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;

import org.jppf.classloader.*;
import org.jppf.classloader.JPPFResourceWrapper.State;
import org.jppf.io.*;
import org.jppf.nio.*;
import org.jppf.serialization.SerializationUtils;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.classloader.node.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ClientClassContext extends AbstractClassContext<ClientClassState> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientClassContext.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The list of pending resource requests for a resource provider.
   */
  private final Queue<ResourceRequest> pendingRequests = new ConcurrentLinkedQueue<>();
  /**
   * The request currently processed.
   */
  protected AtomicReference<ResourceRequest> currentRequest = new AtomicReference<>(null);
  /**
   * Used to synchronize pending requests performed by multiple threads.
   */
  private final Lock lockRequest = new ReentrantLock();

  @Override
  public boolean isProvider() {
    return true;
  }

  @Override
  public boolean setState(final ClientClassState state) {
    ClientClassState oldState = this.state;
    boolean b = super.setState(state);
    if (ClientClassState.IDLE_PROVIDER.equals(state)) {
      try {
        processRequests();
      } catch (Exception e) {
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        else throw new IllegalStateException(e);
      }
      return false;
    }
    return b;
  }

  /**
   * Add a new pending request to this resource provider.
   * @param request the request as a <code>SelectionKey</code> instance.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  public void addRequest(final ResourceRequest request) throws Exception {
    String uuid = request.getResource().getUuidPath().getFirst();
    if (!driver.getClientClassServer().addResourceRequest(uuid, request)) {
      request.setRequestStartTime(System.nanoTime());
      pendingRequests.offer(request);
      processRequests();
    }
  }

  /**
   * Ensure the pending requests are processed.
   * @throws Exception if any error occurs.
   */
  private void processRequests() throws Exception {
    // if requests are already being processed, no need to do anything
    if (lockRequest.tryLock()) {
      try {
        if (ClientClassState.IDLE_PROVIDER.equals(getState()) && (currentRequest.get() == null) && (getNbPendingRequests() > 0)) {
          if (debugEnabled) log.debug("state changing from IDLE_PROVIDER to SENDING_PROVIDER_REQUEST for {}", this);
          driver.getClientClassServer().getTransitionManager().transitionChannel(getChannel(), ClientClassTransition.TO_SENDING_PROVIDER_REQUEST);
        }
      } finally {
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
   * Get the number of pending resource requests for a resource provider.
   * @return a the number of requests as an int.
   */
  public int getNbPendingRequests() {
    return pendingRequests.size();
  }

  /**
   * Determine whether this context has at least one pending request.
   * @return <code>true</code> if there is at least obne pending request, <code>false</code> otherwise.
   */
  public boolean hasPendingRequest() {
    return !pendingRequests.isEmpty();
  }

  /**
   * Get the request currently processed.
   * @return a <code>SelectionKey</code> instance.
   */
  public ResourceRequest getCurrentRequest() {
    return currentRequest.get();
  }

  /**
   * Set the request currently processed.
   * @param currentRequest a <code>SelectionKey</code> instance.
   */
  public void setCurrentRequest(final ResourceRequest currentRequest) {
    this.currentRequest.set(currentRequest);
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception e) {
    ClientClassNioServer.closeConnection(channel);
    handleProviderError();
  }

  /**
   * Handle the scenario where an exception occurs while sending a request to
   * or receiving a response from a provider, and a node channel is waiting for the response.
   */
  protected void handleProviderError() {
    try {
      ResourceRequest currentRequest;
      List<ResourceRequest> pendingList;
      synchronized (this) {
        currentRequest = getCurrentRequest();
        pendingList = new ArrayList<>(pendingRequests);
        if (currentRequest != null) {
          pendingList.add(currentRequest);
          setCurrentRequest(null);
        }
        pendingRequests.clear();
      }
      if (!pendingList.isEmpty()) {
        if (debugEnabled) log.debug("provider: {} sending null response(s) for disconnected provider", getChannel());
        ClientClassNioServer clientClassServer = driver.getClientClassServer();
        ClassNioServer nodeClassServer = driver.getNodeClassServer();
        Set<ChannelWrapper<?>> nodeSet = new HashSet<>();
        for (ResourceRequest mainRequest : pendingList) {
          Collection<ResourceRequest> coll = clientClassServer.removeResourceRequest(uuid, getResourceName(mainRequest.getResource()));
          if (coll == null) continue;
          for (ResourceRequest request: coll) {
            ChannelWrapper<?> nodeChannel = request.getChannel();
            if (!nodeSet.contains(nodeChannel)) nodeSet.add(nodeChannel);
            request.getResource().setState(State.NODE_RESPONSE_ERROR);
          }
        }
        for (ChannelWrapper<?> nodeChannel: nodeSet) resetNodeState(nodeChannel, nodeClassServer);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Reset the state of the requesting node channel, after an error
   * occurred on the provider which attempted to provide a response.
   * @param channel the requesting node channel to reset.
   * @param server the server handling the node.
   */
  private void resetNodeState(final ChannelWrapper<?> channel, final ClassNioServer server) {
    try {
      if (debugEnabled) log.debug(build("resetting channel state for node ", channel));
      server.getTransitionManager().transitionChannel(channel, NodeClassTransition.TO_NODE_WAITING_PROVIDER_RESPONSE, true);
    } catch (Exception e) {
      log.error("error while trying to send response to node {}, this node may be unavailable : {}", e);
    }
  }

  /**
   * Write the peer initiation message when first connecting to a remote server.
   * The message is made of the JPPF identifier {@link JPPFIdentifiers#NODE_CLASSLOADER_CHANNEL} plus
   * an initial resource wrapper (preceded by its serialized length).
   * @param channel the channel to which to write the message.
   * @return <code>true</code> if the message was fully written, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  public boolean writeIdentifier(final ChannelWrapper<?> channel) throws Exception {
    int id = JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL;
    if (nioObject == null) {
      byte[] bytes = SerializationUtils.writeInt(id);
      DataLocation dl = new MultipleBuffersLocation(new JPPFBuffer(bytes, 4));
      nioObject = new PlainNioObject(channel, dl);
    }
    boolean b = false;
    try {
      b = nioObject.write();
    } catch(Exception e) {
      driver.getStatistics().addValue(PEER_OUT_TRAFFIC, nioObject.getChannelCount());
      throw e;
    }
    if (b  && debugEnabled) log.debug("sent channel identifier {} to peer server", JPPFIdentifiers.asString(id));
    if (b) driver.getStatistics().addValue(PEER_OUT_TRAFFIC, nioObject.getChannelCount());
    return b;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    try {
      sb.append("channel=").append(channel.getClass().getSimpleName());
    } catch(Exception e) {
      sb.append("???[");
    }
    sb.append("[id=").append(channel.getId()).append(']');
    sb.append(", state=").append(getState());
    sb.append(", resource=").append(resource == null ? "null" : resource.getName());
    sb.append(", pendingRequests=").append(getNbPendingRequests());
    sb.append(", currentRequest=").append(getCurrentRequest());
    sb.append(", connectionUuid=").append(connectionUuid);
    sb.append(", type=client");
    sb.append(", peer=").append(peer);
    sb.append(", uuid=").append(uuid);
    sb.append(", secure=").append(isSecure());
    sb.append(", ssl=").append(ssl);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Send the specified response to the specified node.
   * @param request the initial request for the node to send the response to.
   * @param resource the response from the provider.
   * @throws Exception if any error occurs.
   */
  public void sendNodeResponse(final ResourceRequest request, final JPPFResourceWrapper resource) throws Exception {
    String uuid = request.getResource().getUuidPath().getFirst();
    ClientClassNioServer server = driver.getClientClassServer();
    Collection<ResourceRequest> allRequests = server.removeResourceRequest(uuid, getResourceName(resource));
    StateTransitionManager tm = driver.getNodeClassServer().getTransitionManager();
    for (ResourceRequest req: allRequests) {
      ChannelWrapper<?> nodeChannel = req.getChannel();
      NodeClassContext nodeContext = (NodeClassContext) nodeChannel.getContext();
      synchronized(nodeChannel) {
        while (NodeClassState.IDLE_NODE != nodeContext.getState()) nodeChannel.wait(0L, 10000);
        ResourceRequest pendingResponse = nodeContext.getPendingResponse(req.getResource());
        if (pendingResponse == null) {
          if (debugEnabled) log.debug("node {} has {} pending responses, but none for {}, pendingResponses={}",
              new Object[] {nodeChannel.getId(), nodeContext.getNbPendingResponses(), resource, nodeContext.getPendingResponses()});
        }
        pendingResponse.setResource(resource);
        tm.transitionChannel(nodeChannel, NodeClassTransition.TO_NODE_WAITING_PROVIDER_RESPONSE, true);
      }
    }
  }

}
