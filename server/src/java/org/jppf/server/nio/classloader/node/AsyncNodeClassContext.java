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

package org.jppf.server.nio.classloader.node;

import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.locks.*;

import org.jppf.classloader.*;
import org.jppf.nio.ClassLoaderNioMessage;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class AsyncNodeClassContext extends AbstractAsynClassContext implements AsyncLocalNodeClassloaderContext {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeClassContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * 
   */
  private final Map<Long, JPPFResourceWrapper> currentNodeRequests = new HashMap<>();
  /**
   * 
   */
  private final CollectionMap<Long, AsyncResourceRequest> pendingResponses = new SetHashMap<>();
  /**
   * Used to synchronize pending responses performed by multiple threads.
   */
  private final Lock lockResponse = new ReentrantLock();
  /**
   * A request emmitted by a local node.
   */
  private JPPFResourceWrapper localRequest;
  /**
   * A response to a local request.
   */
  private JPPFResourceWrapper localResponse;
  /**
   * 
   */
  private final Lock localLock = new ReentrantLock();
  /**
   * 
   */
  private final Condition responseSent = localLock.newCondition();

  /**
   * @param server the server handling this context.
   * @param socketChannel the underlying socket channel.
   */
  public AsyncNodeClassContext(final AsyncNodeClassNioServer server, final SocketChannel socketChannel) {
    super(server.getDriver(), server);
    this.socketChannel = socketChannel;
  }

  @Override
  public void handleException(final Exception e) {
    if (debugEnabled) log.debug("excception on channel {} :\n{}", this, ExceptionUtils.getStackTrace(e));
    getServer().closeConnection(this);
  }

  @Override
  protected boolean isProvider() {
    return false;
  }

  /**
   * Add the specified message to the send queue.
   * @param message the message to add to the queue.
   * @throws Exception if any error occurs.
   */
  void offerMessageToSend(final ClassLoaderNioMessage message) throws Exception {
    sendQueue.offer(message);
    getServer().updateInterestOps(getSelectionKey(), SelectionKey.OP_WRITE, true);
  }

  /**
   * @param resource the node request to add.
   */
  void addNodeRequest(final JPPFResourceWrapper resource) {
    lockResponse.lock();
    try {
      final long id = resource.getResourceId(driver.getUuid());
      if (debugEnabled) log.debug("adding node request for id={}, resource={} to {}", id, resource, this);
      currentNodeRequests.put(id, resource);
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * @param resource the node request to add.
   */
  void removeNodeRequest(final JPPFResourceWrapper resource) {
    lockResponse.lock();
    try {
      final long id = resource.getResourceId(driver.getUuid());
      currentNodeRequests.remove(id);
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Add a pending response.
   * @param resource the resource for which to await a response.
   * @return the new added request.
   */
  public AsyncResourceRequest addPendingResponse(final JPPFResourceWrapper resource) {
    lockResponse.lock();
    try {
      final AsyncResourceRequest request = new AsyncResourceRequest(this, resource);
      resource.setState(JPPFResourceWrapper.State.PROVIDER_REQUEST);
      final long id = resource.getResourceId(driver.getUuid());
      if (debugEnabled) log.debug("adding pending response for id={}, resource={} to {}", id, resource, this);
      pendingResponses.putValue(id, request);
      return request;
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Process a response ot a provider request.
   * @param request the request to handle.
   * @throws Exception if any error occurs.
   */
  public void handleProviderResponse(final AsyncResourceRequest request) throws Exception {
    lockResponse.lock();
    try {
      final JPPFResourceWrapper response = request.getResource();
       final long id = response.getResourceId(driver.getUuid());
       final JPPFResourceWrapper nodeRequest = currentNodeRequests.get(id);
       if (debugEnabled) log.debug("handling provider response with id={}, nodeRquest={} for request={}", id, nodeRequest, request);
       if (nodeRequest == null) return;
       if (nodeRequest instanceof CompositeResourceWrapper) ((CompositeResourceWrapper) nodeRequest).addOrReplaceResource(response);
       pendingResponses.removeValue(id, request);
       if (!pendingResponses.containsKey(id)) {
         currentNodeRequests.remove(id);
         if (nodeRequest instanceof CompositeResourceWrapper) {
           sendResponse(nodeRequest);
         } else {
           response.setRequestStartTime(nodeRequest.getRequestStartTime());
           sendResponse(response);
         }
       }
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Send a response back to the node
   * @param response the response to send.
   * @throws Exception if any error occurs.
   */
  public void sendResponse(final JPPFResourceWrapper response) throws Exception {
    if (local) {
      setLocalResponse(response);
    } else {
      final ClassLoaderNioMessage message = serializeResource(response);
      offerMessageToSend(message);
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
   * 
   */
  void close() {
    lockResponse.lock();
    try {
      pendingResponses.clear();
    } finally {
      lockResponse.unlock();
    }
  }

  @Override
  public JPPFResourceWrapper getLocalRequest() throws Exception {
    localLock.lock();
    try {
      return localRequest;
    } finally {
      localLock.unlock();
    }
  }

  @Override
  public void setLocalRequest(final JPPFResourceWrapper localRequest) throws Exception {
    localLock.lock();
    try {
      this.localRequest = localRequest;
      this.localResponse = null;
      final AsyncNodeClassMessageHandler handler = getServer().getMessageHandler();
      switch(localRequest.getState()) {
        case NODE_INITIATION:
          handler.handshakeRequest(this, localRequest);
          break;

        case NODE_REQUEST:
          handler.nodeRequest(this, localRequest);
          break;

        case CLOSE_CHANNEL:
          handler.closeChannelRequest(this, localRequest);
          break;
      }
    } finally {
      localLock.unlock();
    }
  }

  @Override
  public JPPFResourceWrapper getLocalResponse() throws Exception {
    localLock.lock();
    try {
      while (localResponse == null) responseSent.await();
      return localResponse;
    } finally {
      localLock.unlock();
    }
  }

  @Override
  public void setLocalResponse(final JPPFResourceWrapper localResponse) throws Exception {
    localLock.lock();
    try {
      this.localResponse = localResponse;
      responseSent.signalAll();
    } finally {
      localLock.unlock();
    }
  }

  @Override
  public AsyncNodeClassNioServer getServer() {
    return (AsyncNodeClassNioServer) super.getServer();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", peer=").append(peer);
    sb.append(", ssl=").append(ssl);
    lockResponse.lock();
    try {
      sb.append(", currentRequests=").append(currentNodeRequests.size());
      sb.append(", pendingResponses=").append(pendingResponses.size());
    } finally {
      lockResponse.unlock();
    }
    sb.append(", sendQueueSize=").append(sendQueue.size());
    sb.append(", interestOps=").append(getInterestOps());
    sb.append(']');
    return sb.toString();
  }
}
