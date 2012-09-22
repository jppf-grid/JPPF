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

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.server.nio.*;

/**
 * 
 */
public class ResourceRequest
{
  /**
   * The node class loader channel.
   */
  private final ChannelWrapper<?> channel;
  /**
   * The resource to lookup in the client.
   */
  private JPPFResourceWrapper resource;

  /**
   * 
   * @param channel the node class loader channel.
   * @param resource the resource to lookup in the client.
   */
  public ResourceRequest(final ChannelWrapper<?> channel, final JPPFResourceWrapper resource)
  {
    this.channel = channel;
    this.resource = resource;
  }

  /**
   * Get the node class loader channel.
   * @return a {@link ChannelWrapper} instance.
   */
  public ChannelWrapper<?> getChannel()
  {
    return channel;
  }

  /**
   * Get the resource to lookup in the client.
   * @return a {@link JPPFResourceWrapper} instance.
   */
  public JPPFResourceWrapper getResource()
  {
    return resource;
  }

  /**
   * Set the resource to lookup in the client.
   * @param resource a {@link JPPFResourceWrapper} instance.
   */
  public void setResource(final JPPFResourceWrapper resource)
  {
    this.resource = resource;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("channel=").append(channel.getClass().getSimpleName()).append("[id=").append(channel.getId()).append(']');
    sb.append(", resource=").append(resource);
    sb.append(']');
    return sb.toString();
  }
}