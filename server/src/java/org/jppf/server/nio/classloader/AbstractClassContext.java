/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import org.jppf.classloader.*;
import org.jppf.io.*;
import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;

/**
 * Context object associated with a socket channel used by the class server of the JPPF driver.
 * @param <S> the ytpe of states associated with the channels.
 * @author Laurent Cohen
 */
public abstract class AbstractClassContext<S extends Enum<S>> extends SimpleNioContext<S> {
  /**
   * The resource read from or written to the associated channel.
   */
  protected JPPFResourceWrapper resource = null;
  /**
   * Contains the JPPF peer identifier written to the socket channel.
   */
  protected NioObject nioObject = null;
  /**
   * Reference to the driver.
   */
  protected final JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * Time at which the current request was received.
   */
  protected long requestStartTime = 0L;

  /**
   * Deserialize a resource wrapper from an array of bytes.
   * @return a <code>JPPFResourceWrapper</code> instance.
   * @throws Exception if an error occurs while deserializing.
   */
  public JPPFResourceWrapper deserializeResource() throws Exception {
    requestStartTime = System.nanoTime();
    final DataLocation dl = ((BaseNioMessage) message).getLocations().get(0);
    resource = (JPPFResourceWrapper) IOHelper.unwrappedData(dl, JPPFDriver.getSerializer());
    return resource;
  }

  /**
   * Serialize a resource wrapper to an array of bytes.
   * @throws Exception if an error occurs while serializing.
   */
  public void serializeResource() throws Exception {
    final DataLocation location = IOHelper.serializeData(resource, JPPFDriver.getSerializer());
    message = new BaseNioMessage(getChannel());
    ((BaseNioMessage) message).addLocation(location);
  }

  /**
   * Get the resource read from or written to the associated channel.
   * @return the resource a <code>JPPFResourceWrapper</code> instance.
   */
  public JPPFResourceWrapper getResource() {
    return resource;
  }

  /**
   * Set the resource read from or written to the associated channel.
   * @param resource a <code>JPPFResourceWrapper</code> instance.
   */
  public void setResource(final JPPFResourceWrapper resource) {
    this.resource = resource;
  }

  /**
   * Determine whether this context relates to a provider or node connection.
   * @return true if this is a provider context, false otherwise.
   */
  public abstract boolean isProvider();

  /**
   * Get the time at which the current request was received.
   * @return the time in nanos.
   */
  public long getRequestStartTime() {
    return requestStartTime;
  }

  @Override
  public boolean readMessage(final ChannelWrapper<?> wrapper) throws Exception {
    boolean b = false;
    try {
      b = super.readMessage(wrapper);
    } catch(final Exception e) {
      updateTrafficStats();
      throw e;
    }
    if (b) updateTrafficStats();
    return b;
  }

  @Override
  public boolean writeMessage(final ChannelWrapper<?> wrapper) throws Exception {
    boolean b = false;
    try {
      b = super.writeMessage(wrapper);
    } catch(final Exception e) {
      updateTrafficStats();
      throw e;
    }
    if (b) updateTrafficStats();
    return b;
  }

  /**
   * Update the inbound and outbound traffic statistics.
   */
  private void updateTrafficStats() {
    if (message != null) {
      if (inSnapshot == null) inSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_IN_TRAFFIC : (isProvider() ? CLIENT_IN_TRAFFIC : NODE_IN_TRAFFIC));
      if (outSnapshot == null) outSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_OUT_TRAFFIC : (isProvider() ? CLIENT_OUT_TRAFFIC : NODE_OUT_TRAFFIC));
      double value = message.getChannelReadCount();
      if (value > 0d) inSnapshot.addValues(value, 1L);
      value = message.getChannelWriteCount();
      if (value > 0d) outSnapshot.addValues(value, 1L);
    }
  }

  /**
   * Determine whether the specified resource is a request for a single resource definition.
   * @param resource the resource to check.
   * @return <code>true</code> if the specified resource is a request for a single resource definition, <code>false</code> otherwise.
   */
  public static String getResourceName(final JPPFResourceWrapper resource) {
    final StringBuilder sb = new StringBuilder();
    if (resource.getData(ResourceIdentifier.MULTIPLE) != null) sb.append(ResourceIdentifier.MULTIPLE).append('.').append(resource.getName());
    else if (resource.getData(ResourceIdentifier.MULTIPLE_NAMES) != null) {
      sb.append(ResourceIdentifier.MULTIPLE_NAMES).append('[').append(resource.getName());
      final String[] names = (String[]) resource.getData(ResourceIdentifier.MULTIPLE_NAMES);
      for (int i=0; i<names.length; i++) {
        if (i > 0) sb.append(',');
        sb.append(names[i]);
      }
      sb.append(']');
    } else if (resource.getData(ResourceIdentifier.CALLABLE) != null) sb.append(resource.getData(ResourceIdentifier.DRIVER_CALLABLE_ID));
    else sb.append(resource.getName());
    return sb.toString();
  }
}
