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

package org.jppf.server.nio.classloader;

import static org.jppf.classloader.ResourceIdentifier.*;

import org.jppf.classloader.*;
import org.jppf.server.nio.classloader.node.async.*;

/**
 * Instances of this class represent a class loading request from a node to a client channel.
 */
public class AsyncResourceRequest {
  /**
   * The types of resource.
   */
  private static final ResourceIdentifier[] RESOURCE_IDS = { DEFINITION, RESOURCE_LIST, RESOURCE_MAP };
  /**
   * The node class loader channel.
   */
  private final AsyncNodeClassContext context;
  /**
   * The resource to lookup in the client.
   */
  private JPPFResourceWrapper resource;
  /**
   * The time at which this request was submitted.
   */
  private long requestStartTime;

  /**
   * Initialize this request with the specified node channel and resource.
   * @param context the node class loader channel.
   * @param resource the resource to lookup in the client.
   */
  public AsyncResourceRequest(final AsyncNodeClassContext context, final JPPFResourceWrapper resource) {
    this.requestStartTime = System.nanoTime();
    this.context = context;
    this.resource = resource;
  }

  /**
   * Get the node class loader channel.
   * @return a {@link AsyncNodeClassContext} instance.
   */
  public AsyncNodeClassContext getContext() {
    return context;
  }

  /**
   * Get the resource to lookup in the client.
   * @return a {@link JPPFResourceWrapper} instance.
   */
  public JPPFResourceWrapper getResource() {
    return resource;
  }

  /**
   * Set the resource to lookup in the client.
   * @param resource a {@link JPPFResourceWrapper} instance.
   */
  public void setResource(final JPPFResourceWrapper resource) {
    Object o = null;
    for (final ResourceIdentifier id: RESOURCE_IDS) {
      if ((o = resource.getData(id)) != null) this.resource.setData(id, o);
    }
    final long callableId = resource.getCallableID();
    if ((callableId == this.resource.getCallableID()) && (callableId >= 0) && ((o = resource.getCallable()) != null)) this.resource.setCallable((byte[]) o);
    this.resource.setState(resource.getState());
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("context=").append(context);
    sb.append(", resource=").append(resource);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the time at which this request was submitted.
   * @return the time in nanoseconds.
   */
  public long getRequestStartTime() {
    return requestStartTime;
  }

  /**
   * Set the time at which this request was submitted.
   * @param requestStartTime the time in nanoseconds.
   */
  public void setRequestStartTime(final long requestStartTime) {
    this.requestStartTime = requestStartTime;
  }
}
