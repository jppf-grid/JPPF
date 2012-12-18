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

import org.jppf.*;
import org.jppf.comm.socket.*;
import org.jppf.node.NodeRunner;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * JPPF class loader implementation for remote standalone nodes.
 * @author Laurent Cohen
 */
public class JPPFClassLoader extends AbstractJPPFClassLoader
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClassLoader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Wrapper for the underlying socket connection.
   */
  private SocketWrapper socketClient = null;
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  private static SocketInitializer socketInitializer = new SocketInitializerImpl();
  /**
   * Detrmines whether SSL is enabled.
   */
  private static boolean sslEnabled = false;

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   */
  public JPPFClassLoader(final ClassLoader parent)
  {
    super(parent);
    init();
  }

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   * @param uuidPath unique identifier for the submitting application.
   */
  public JPPFClassLoader(final ClassLoader parent, final List<String> uuidPath)
  {
    super(parent, uuidPath);
  }

  /**
   * Initialize the underlying socket connection.
   */
  private void initSocketClient()
  {
    if (debugEnabled) log.debug("initializing socket connection");
    TypedProperties props = JPPFConfiguration.getProperties();
    sslEnabled = props.getBoolean("jppf.ssl.enabled", false);
    String host = props.getString("jppf.server.host", "localhost");
    // for backward compatibility with v2.x configurations
    int port = props.getAndReplaceInt("jppf.server.port", "class.server.port", sslEnabled ? 11443 : 11111, false);
    socketClient = new BootstrapSocketClient();
    socketClient.setHost(host);
    socketClient.setPort(port);
  }

  /**
   * Initialize the underlying socket connection.
   * @exclude
   */
  @Override
  protected void init()
  {
    LOCK.lock();
    try
    {
      if (INITIALIZING.compareAndSet(false, true))
      {
        /*
        synchronized(AbstractJPPFClassLoaderLifeCycle.class)
        {
          if (executor == null) executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("ClassloaderRequests", false, Thread.NORM_PRIORITY, true));
        }
        */
        try
        {
          if (debugEnabled) log.debug("initializing connection");
          if (socketClient == null) initSocketClient();
          System.out.println("Attempting connection to the class server at " + socketClient.getHost() + ':' + socketClient.getPort());
          socketInitializer.initializeSocket(socketClient);
          if (!socketInitializer.isSuccessful())
          {
            socketClient = null;
            throw new JPPFNodeReconnectionNotification("Could not reconnect to the server");
          }
          if (sslEnabled) createSSLConnection();
          performHandshake();
          System.out.println("Reconnected to the class server");
        }
        finally
        {
          INITIALIZING.set(false);
        }
      }
    }
    finally
    {
      LOCK.unlock();
    }
  }

  /**
   * Create the ssl connection over an established plain connection.
   */
  private void createSSLConnection()
  {
    try
    {
      socketClient = SSLHelper.createSSLClientConnection(socketClient);
    }
    catch(Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform the handshake with the server. The handshake consists in:
   * <ol>
   * <li>sending a channel identifier {@link JPPFIdentifiers#NODE_CLASSLOADER_CHANNEL} to the server</li>
   * <li>sending an initial message to the server</li>
   * <li>receiving an initial response from the server</li>
   * </ol>
   * 
   */
  private void performHandshake()
  {
    try
    {
      if (debugEnabled) log.debug("sending channel identifier");
      socketClient.writeInt(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL);
      socketClient.flush();
      if (debugEnabled) log.debug("sending node initiation message");
      JPPFResourceWrapper request = new JPPFResourceWrapper();
      request.setState(JPPFResourceWrapper.State.NODE_INITIATION);
      request.setData("node.uuid", NodeRunner.getUuid());
      ResourceRequest rr = new RemoteResourceRequest(getSerializer(), socketClient);
      rr.setRequest(request);
      rr.run();
      Throwable t = rr.getThrowable();
      if (t != null)
      {
        if (t instanceof Exception) throw (Exception) t;
        else throw new RuntimeException(t);
      }
      if (debugEnabled) log.debug("received node initiation response");
      rr.reset();
      requestHandler = new ClassLoaderRequestHandler(rr);
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

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void reset()
  {
    LOCK.lock();
    try
    {
      synchronized(JPPFClassLoader.class)
      {
        socketClient = null;
      }
      init();
    }
    finally
    {
      LOCK.unlock();
    }
  }

  /**
   * Terminate this classloader and clean the resources it uses.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#close()
   */
  @Override
  public void close()
  {
    LOCK.lock();
    try
    {
      synchronized(AbstractJPPFClassLoaderLifeCycle.class)
      {
        if (requestHandler != null)
        {
          requestHandler.close();
          requestHandler = null;
        }
      }
      synchronized(JPPFClassLoader.class)
      {
        if (socketInitializer != null) socketInitializer.close();
        if (socketClient != null)
        {
          try
          {
            socketClient.close();
          }
          catch(Exception e)
          {
            if (debugEnabled) log.debug(e.getMessage(), e);
          }
          socketClient = null;
        }
      }
      super.close();
    }
    finally
    {
      LOCK.unlock();
    }
  }

  /**
   * Load the specified class from a socket connection.
   * @param map contains the necessary resource request data.
   * @param asResource true if the resource is loaded using getResource(), false otherwise.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws Exception if the connection was lost and could not be reestablished.
   * @exclude
   */
  @Override
  protected JPPFResourceWrapper loadRemoteData(final Map<String, Object> map, final boolean asResource) throws Exception
  {
    JPPFResourceWrapper resource = new JPPFResourceWrapper();
    resource.setState(JPPFResourceWrapper.State.NODE_REQUEST);
    resource.setDynamic(dynamic);
    TraversalList<String> list = new TraversalList<String>(uuidPath);
    resource.setUuidPath(list);
    if (list.size() > 0) list.setPosition(uuidPath.size()-1);
    for (Map.Entry<String, Object> entry: map.entrySet()) resource.setData(entry.getKey(), entry.getValue());
    resource.setAsResource(asResource);
    resource.setRequestUuid(requestUuid);

    Future<JPPFResourceWrapper> f = requestHandler.addRequest(resource);
    //Future<JPPFResourceWrapper> f = requestHandler.addRequest(resource, this);
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
}
