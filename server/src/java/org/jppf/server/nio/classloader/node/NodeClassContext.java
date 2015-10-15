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

package org.jppf.server.nio.classloader.node;

import java.util.*;
import java.util.concurrent.locks.*;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.*;

/**
 *
 * @author Laurent Cohen
 */
public class NodeClassContext extends AbstractClassContext<NodeClassState> {
  /**
   * The list of pending resource responses for a node.
   */
  private final Map<JPPFResourceWrapper, ResourceRequest> pendingResponses = new HashMap<>();
  /**
   * Used to synchronize pending responses performed by multiple threads.
   */
  private final Lock lockResponse = new ReentrantLock();

  @Override
  public boolean isProvider() {
    return false;
  }

  @Override
  public boolean setState(final NodeClassState state) {
    NodeClassState oldState = this.state;
    boolean b = super.setState(state);
    if (NodeClassState.IDLE_NODE.equals(state)) {
      synchronized(getChannel()) {
        getChannel().notifyAll();
      }
    }
    return b;
  }

  /**
   * Get the set of pending resource responses for a node.
   * @return a {@link Map} of {@link ResourceRequest} instances.
   */
  public Map<JPPFResourceWrapper, ResourceRequest> getPendingResponses() {
    lockResponse.lock();
    try {
      return new HashMap<>(pendingResponses);
    } finally {
      lockResponse.unlock();
    }
  }

  /**
   * Add a pending response.
   * @param resource the requets resource.
   * @param request the request.
   */
  public void addPendingResponse(final JPPFResourceWrapper resource, final ResourceRequest request) {
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
  public void removePendingResponses(final Collection<JPPFResourceWrapper> toRemove) {
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
  public boolean hasPendingResponse() {
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
  public void handleException(final ChannelWrapper<?> channel, final Exception e) {
    NodeClassNioServer.closeConnection(channel);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("channel=").append(channel.getClass().getSimpleName()).append("[id=").append(channel.getId()).append(']');
    sb.append(", state=").append(getState());
    sb.append(", resource=").append(resource == null ? "null" : resource.getName());
    if (lockResponse.tryLock()) {
      try {
        sb.append(", pendingResponses=").append(pendingResponses.size());
      } finally {
        lockResponse.unlock();
      }
    }
    sb.append(", type=node");
    sb.append(", peer=").append(peer);
    sb.append(", uuid=").append(uuid);
    sb.append(", secure=").append(isSecure());
    sb.append(", ssl=").append(ssl);
    sb.append(']');
    return sb.toString();
  }
}
