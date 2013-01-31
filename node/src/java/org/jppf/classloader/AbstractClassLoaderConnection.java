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

package org.jppf.classloader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.*;
import org.jppf.node.NodeRunner;
import org.jppf.utils.TraversalList;
import org.slf4j.*;

/**
 * Abstract impelementation of {@link ClassLoaderConnection}.
 * @param <C> the type of communication channel used by this connection.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractClassLoaderConnection<C> implements ClassLoaderConnection<C>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractClassLoaderConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The channel used to communicate witht he driver.
   */
  protected C channel = null;
  /**
   * The object which sends the class laoding requests and receives the responses.
   */
  protected ClassLoaderRequestHandler requestHandler = null;
  /**
   * Used to synchronize access to the communication channel from multiple threads.
   */
  protected final ReentrantLock lock = new ReentrantLock();
  /**
   * Determines whether this connection is initializing.
   */
  protected final AtomicBoolean initializing = new AtomicBoolean(false);

  /**
   * Perform the part of the handshake common to remote and local nodes. This consists in:
   * <ol>
   * <li>sending an initial message to the server</li>
   * <li>receiving an initial response from the server</li>
   * </ol>
   * @param requestRunner the object wrapping the driver connection and handling the class loading requests.
   */
  protected void performCommonHandshake(final ResourceRequestRunner requestRunner)
  {
    try
    {
      if (debugEnabled) log.debug("sending node initiation message");
      JPPFResourceWrapper request = new JPPFResourceWrapper();
      request.setState(JPPFResourceWrapper.State.NODE_INITIATION);
      request.setData("node.uuid", NodeRunner.getUuid());
      requestRunner.setRequest(request);
      requestRunner.run();
      Throwable t = requestRunner.getThrowable();
      if (t != null)
      {
        if (t instanceof Exception) throw (Exception) t;
        else throw new RuntimeException(t);
      }
      if (debugEnabled) log.debug("received node initiation response");
      requestRunner.reset();
      requestHandler = new ClassLoaderRequestHandler(requestRunner);
    }
    catch (IOException e)
    {
      throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver", e);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JPPFResourceWrapper loadResource(final Map<String, Object> map, final boolean dynamic, final String requestUuid, final List<String> uuidPath) throws Exception
  {
    JPPFResourceWrapper resource = new JPPFResourceWrapper();
    resource.setState(JPPFResourceWrapper.State.NODE_REQUEST);
    resource.setDynamic(dynamic);
    TraversalList<String> list = new TraversalList<String>(uuidPath);
    resource.setUuidPath(list);
    if (list.size() > 0) list.setPosition(uuidPath.size()-1);
    for (Map.Entry<String, Object> entry: map.entrySet()) resource.setData(entry.getKey(), entry.getValue());
    resource.setRequestUuid(requestUuid);

    Future<JPPFResourceWrapper> f = requestHandler.addRequest(resource);
    resource = f.get();
    Throwable t = ((ResourceFuture) f).getThrowable();
    if (t != null)
    {
      if (t instanceof Exception) throw (Exception) t;
      else if (t instanceof Error) throw (Error) t;
      else throw new JPPFException(t);
    }
    return resource;
  }

  @Override
  public C getChannel()
  {
    return channel;
  }

  /**
   * Get the object which sends the class laoding requests and receives the responses.
   * @return a {@link ClassLoaderRequestHandler} instance.
   */
  public ClassLoaderRequestHandler getRequestHandler()
  {
    return requestHandler;
  }
}
