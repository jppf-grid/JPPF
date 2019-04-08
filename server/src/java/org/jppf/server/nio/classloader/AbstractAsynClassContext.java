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

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.concurrent.*;

import org.jppf.classloader.*;
import org.jppf.io.*;
import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;

/**
 * Context object associated with a socket channel used by the class server of the JPPF driver.
 * @author Laurent Cohen
 */
public abstract class AbstractAsynClassContext extends StatelessNioContext {
  /**
   * Reference to the driver.
   */
  protected final JPPFDriver driver;
  /**
   * 
   */
  private final StatelessNioServer<? extends AbstractAsynClassContext> server;
  /**
   * This queue contains all the result bundles to send back to the node.
   */
  protected final BlockingQueue<ClassLoaderNioMessage> sendQueue = new LinkedBlockingQueue<>();

  /**
   * @param driver reference to the JPPF driver.
   * @param server the server handling this context.
   */
  public AbstractAsynClassContext(final JPPFDriver driver, final StatelessNioServer<? extends AbstractAsynClassContext> server) {
    this.server = server;
    this.driver = driver;
  }

  /**
   * Deserialize a resource wrapper from an array of bytes.
   * @param message the message to deserialize.
   * @return a {@link JPPFResourceWrapper} instance.
   * @throws Exception if an error occurs while deserializing.
   */
  public JPPFResourceWrapper deserializeResource(final ClassLoaderNioMessage message) throws Exception {
    if (local) return message.getResource();
    final DataLocation dl = message.getLocations().get(0);
    final Thread currentThread = Thread.currentThread();
    final ClassLoader cl = currentThread.getContextClassLoader();
    JPPFResourceWrapper resource = null;
    try {
      currentThread.setContextClassLoader(getClass().getClassLoader());
      resource = (JPPFResourceWrapper) IOHelper.unwrappedData(dl, driver.getSerializer());
    } finally {
      currentThread.setContextClassLoader(cl);
    }
    return resource;
  }

  /**
   * Serialize a resource wrapper to an array of bytes.
   * @param resource the resource to serialize.
   * @return a {@link ClassLoaderNioMessage} instance.
   * @throws Exception if an error occurs while serializing.
   */
  public ClassLoaderNioMessage serializeResource(final JPPFResourceWrapper resource) throws Exception {
    final ClassLoaderNioMessage message = new ClassLoaderNioMessage(this, resource);
    if (!local) {
      final DataLocation location = IOHelper.serializeData(resource, driver.getSerializer());
      message.addLocation(location);
    }
    return message;
  }

  /**
   * Determine whether this context relates to a provider or node connection.
   * @return true if this is a provider context, false otherwise.
   */
  protected abstract boolean isProvider();

  @Override
  public boolean readMessage() throws Exception {
    if (message == null) message = new ClassLoaderNioMessage(this);
    byteCount = message.getChannelReadCount();
    boolean b = false;
    try {
      b = message.read();
    } catch (final Exception e) {
      updateInTrafficStats();
      throw e;
    }
    byteCount = message.getChannelReadCount() - byteCount;
    if (b) updateInTrafficStats();
    return b;
  }

  @Override
  public boolean writeMessage() throws Exception {
    writeByteCount = writeMessage.getChannelWriteCount();
    boolean b = false;
    try {
      b = writeMessage.write();
    } catch (final Exception e) {
      updateOutTrafficStats();
      throw e;
    }
    writeByteCount = writeMessage.getChannelWriteCount() - writeByteCount;
    if (b) updateOutTrafficStats();
    return b;
  }

  /**
   * Update the inbound and outbound traffic statistics.
   */
  private void updateInTrafficStats() {
    if (message != null) {
      if (inSnapshot == null) inSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_IN_TRAFFIC : (isProvider() ? CLIENT_IN_TRAFFIC : NODE_IN_TRAFFIC));
      final double value = message.getChannelReadCount();
      if (value > 0d) inSnapshot.addValues(value, 1L);
    }
  }

  /**
   * Update the inbound and outbound traffic statistics.
   */
  private void updateOutTrafficStats() {
    if (writeMessage != null) {
      if (outSnapshot == null) outSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_OUT_TRAFFIC : (isProvider() ? CLIENT_OUT_TRAFFIC : NODE_OUT_TRAFFIC));
      final double value = writeMessage.getChannelWriteCount();
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

  /**
   * @return the Nio server handling this context.
   */
  public StatelessNioServer<? extends AbstractAsynClassContext> getServer() {
    return server;
  }

  @Override
  protected ClassLoaderNioMessage nextMessageToSend() {
    return sendQueue.poll();
  }
}
