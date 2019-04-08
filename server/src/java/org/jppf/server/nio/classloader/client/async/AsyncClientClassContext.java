/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.server.nio.classloader.client.async;

import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.classloader.JPPFResourceWrapper.State;
import org.jppf.nio.ClassLoaderNioMessage;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Context or state information associated with a channel that exchanges heartbeat messages between the server and a node or client.
 * @author Laurent Cohen
 */
public class AsyncClientClassContext extends AbstractAsynClassContext {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientClassContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * 
   */
  private final CollectionMap<JPPFResourceWrapper, AsyncResourceRequest> requestMap = new ArrayListHashMap<>();

  /**
   * @param server the server that handles this context.
   * @param socketChannel the associated socket channel.
   */
  AsyncClientClassContext(final AsyncClientClassNioServer server, final SocketChannel socketChannel) {
    super(server.getDriver(), server);
    this.socketChannel = socketChannel;
  }

  @Override
  public void handleException(final Exception e) {
    if (getClosed().compareAndSet(false, true)) {
      if (debugEnabled) log.debug("handling exception on {}:{}", this, (e == null) ? " null" : "\n" + ExceptionUtils.getStackTrace(e));
      getServer().closeConnection(this);
      handleProviderError();
    }
  }

  /**
   * Handle the scenario where an exception occurs while sending a request to
   * or receiving a response from a provider, and a node channel is waiting for the response.
   */
  protected void handleProviderError() {
    try {
      final List<AsyncResourceRequest> pendingList;
      synchronized(requestMap) {
        pendingList = new ArrayList<>(requestMap.allValues());
        requestMap.clear();
      }
      if (!pendingList.isEmpty()) {
        if (debugEnabled) log.debug("provider: {} sending null response(s) for disconnected provider", this);
        for (final AsyncResourceRequest request : pendingList) {
          request.getResource().setState(State.NODE_RESPONSE_ERROR);
          request.getContext().handleProviderResponse(request);
        }
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Add the specified message to the send queue.
   * @param message the message to add to the queue.
   * @throws Exception if any error occurs.
   */
  public void offerMessageToSend(final ClassLoaderNioMessage message) throws Exception {
    sendQueue.offer(message);
    getServer().updateInterestOps(getSelectionKey(), SelectionKey.OP_WRITE, true);
  }

  /**
   * Add a new request to send to the client.
   * @param request the requets to send.
   * @throws Exception if any error occurs.
   */
  public void addRequest(final AsyncResourceRequest request) throws Exception {
    final JPPFResourceWrapper resource = request.getResource();
    if (resource.isSingleResource()) {
      final String uuid = resource.getUuidPath().getFirst();
      final byte[] content = getServer().getClassCache().getCacheContent(uuid, resource.getName());
      if (content != null) {
        if (debugEnabled) log.debug("resource [uuid={}, res={}] found in the cache, request will not be sent to the client", uuid, resource.getName());
        resource.setDefinition(content);
        resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
        request.getContext().handleProviderResponse(request);
        return;
      }
    }
    if (debugEnabled) log.debug("adding request from node: {}'", resource);
    synchronized(requestMap) {
      requestMap.putValue(resource, request);
    }
    offerMessageToSend(serializeResource(resource));
  }

  /**
   * 
   * @return the number of pending requests.
   */
  public int getNbPendingRequests() {
    synchronized(requestMap) {
      return requestMap.size();
    }
  }

  /**
   * Retrieve the request matching a specified resource.
   * @param resource the key in the request map.
   * @return an {@link AsyncResourceRequest} instance, or {@code null} if there is no entry with the specified resource.
   */
  public List<AsyncResourceRequest> getRequests(final JPPFResourceWrapper resource) {
    synchronized(requestMap) {
      return new ArrayList<>(requestMap.getValues(resource));
    }
  }

  /**
   * Remove the request matching a specified resource.
   * @param resource the key in the request map.
   * @return the removed {@link AsyncResourceRequest} instance, or {@code null} if there is no entry with the specified id.
   */
  public AsyncResourceRequest removeRequest(final JPPFResourceWrapper resource) {
    final long id = resource.getResourceId(driver.getUuid());
    if (debugEnabled) log.debug("removing request with resource={}, resourceId={}", resource, id);
    AsyncResourceRequest toRemove = null;
    synchronized(requestMap) {
      final Collection<AsyncResourceRequest> requests = requestMap.getValues(resource);
      if (requests != null) {
        for (final AsyncResourceRequest request: requests) {
          if (request.getResource().getResourceId(driver.getUuid()) == id) {
            toRemove = request;
            break;
          }
        }
      }
      if (toRemove != null) requestMap.removeValue(resource, toRemove);
    }
    return toRemove;
  }

  /**
   * Get the closed flag for this client context.
   * @return an {@link AtomicBoolean}.
   */
  AtomicBoolean getClosed() {
    return closed;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", connectionUuid=").append(connectionUuid);
    sb.append(", peer=").append(peer);
    sb.append(", ssl=").append(ssl);
    sb.append(", requests=").append(getNbPendingRequests());
    sb.append(", sendQueue size=").append(sendQueue.size());
    sb.append(", interestOps=").append(getInterestOps());
    sb.append(']');
    return sb.toString();
  }

  @Override
  protected boolean isProvider() {
    return true;
  }

  @Override
  public AsyncClientClassNioServer getServer() {
    return (AsyncClientClassNioServer) super.getServer();
  }
}
