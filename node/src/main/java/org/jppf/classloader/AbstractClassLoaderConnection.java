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

package org.jppf.classloader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;

import org.jppf.*;
import org.jppf.node.*;
import org.jppf.node.connection.ConnectionReason;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Abstract implementation of {@link ClassLoaderConnection}.
 * @param <C> the type of communication channel used by this connection.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractClassLoaderConnection<C> extends AbstractNodeConnection<C> implements ClassLoaderConnection<C> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractClassLoaderConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The object which sends the class laoding requests and receives the responses.
   */
  protected ClassLoaderRequestHandler requestHandler;
  /**
   * This node's uuid.
   */
  protected final String uuid;

  /**
   * 
   * @param uuid this node's uuid.
   */
  protected AbstractClassLoaderConnection(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * Perform the part of the handshake common to remote and local nodes. This consists in:
   * <ol>
   * <li>sending an initial message to the server</li>
   * <li>receiving an initial response from the server</li>
   * </ol>
   * @param requestRunner the object wrapping the driver connection and handling the class loading requests.
   */
  protected void performCommonHandshake(final ResourceRequestRunner requestRunner) {
    try {
      if (debugEnabled) log.debug("sending node initiation message");
      final JPPFResourceWrapper request = new JPPFResourceWrapper();
      request.setState(JPPFResourceWrapper.State.NODE_INITIATION);
      request.setData(ResourceIdentifier.NODE_UUID, uuid);
      requestRunner.setRequest(request);
      requestRunner.run();
      final Throwable t = requestRunner.getThrowable();
      if (t != null) {
        if (debugEnabled) log.debug("handshake error from {} for request = {}: {}", this, request, ExceptionUtils.getMessage(t));
        if (t instanceof Exception) throw (Exception) t;
        else throw new RuntimeException(t);
      }
      if (debugEnabled) log.debug("received node initiation response");
      requestRunner.reset();
      requestHandler = new ClassLoaderRequestHandler(requestRunner);
    } catch (final IOException e) {
      log.debug(e.getMessage(), e);
      throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver", e, ConnectionReason.CLASSLOADER_INIT_ERROR);
    } catch (final Exception e) {
      log.error("error during class loader handshake, connection = {}", this, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Send a command to close the channel to the server.
   * @param requestRunner the object wrapping the driver connection and handling the class loading requests.
   */
  protected void sendCloseChannelCommand(final ResourceRequestRunner requestRunner) {
    try {
      if (debugEnabled) log.debug("sending close channel command");
      final JPPFResourceWrapper request = new JPPFResourceWrapper();
      request.setState(JPPFResourceWrapper.State.CLOSE_CHANNEL);
      request.setData(ResourceIdentifier.NODE_UUID, uuid);
      requestRunner.setRequest(request);
      requestRunner.run();
      final Throwable t = requestRunner.getThrowable();
      if (t != null) {
        if (t instanceof Exception) throw (Exception) t;
        else throw new RuntimeException(t);
      }
      if (debugEnabled) log.debug("received node response");
      requestRunner.reset();
    } catch (final Exception e) {
      if (debugEnabled) log.debug( "error sending close channel command from {}:\n{}", this, ExceptionUtils.getStackTrace(e));
      else log.warn( "error sending close channel command form {}: {}", this, ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public JPPFResourceWrapper loadResource(final Map<ResourceIdentifier, Object> map, final boolean dynamic, final String requestUuid, final List<String> uuidPath) throws Exception {
    JPPFResourceWrapper resource = new JPPFResourceWrapper();
    resource.setState(JPPFResourceWrapper.State.NODE_REQUEST);
    resource.setDynamic(dynamic);
    final TraversalList<String> list = new TraversalList<>(uuidPath);
    resource.setUuidPath(list);
    if (list.size() > 0) list.setPosition(uuidPath.size()-1);
    for (final Map.Entry<ResourceIdentifier, Object> entry: map.entrySet()) resource.setData(entry.getKey(), entry.getValue());
    resource.setRequestUuid(requestUuid);
    if (requestHandler != null) {
      final Future<JPPFResourceWrapper> f = requestHandler.addRequest(resource);
      resource = f.get();
      final Throwable t = ((ResourceFuture<?>) f).getThrowable();
      if (t != null) {
        if (debugEnabled) log.debug("error loading resource {} from {}: {}", resource, this, ExceptionUtils.getMessage(t));
        if (t instanceof Exception) throw (Exception) t;
        else if (t instanceof Error) throw (Error) t;
        else throw new JPPFException(t);
      }
    }
    return resource;
  }

  /**
   * Get the object which sends the class laoding requests and receives the responses.
   * @return a {@link ClassLoaderRequestHandler} instance.
   */
  public ClassLoaderRequestHandler getRequestHandler() {
    return requestHandler;
  }

  @Override
  public void reset() throws Exception {
    lock.lock();
    try {
      init();
    } catch (final Exception e) {
      if (debugEnabled) log.debug("{} could not reconnect to the server after connection reset: {}", this, ExceptionUtils.getMessage(e));
      throw new JPPFNodeReconnectionNotification("Could not reconnect to the server after connection reset", e, ConnectionReason.CLASSLOADER_INIT_ERROR);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("channel=").append(channel)
      .append(", initializing=").append(initializing)
      .append(", requestHandler=").append(requestHandler)
      .append(']').toString();
  }
}
