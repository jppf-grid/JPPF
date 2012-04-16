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

import java.io.InputStream;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.node.NodeRunner;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * JPPF class loader implementation for nodes running within the same JVM as the JPPF server (local nodes).
 * @author Laurent Cohen
 */
public class JPPFLocalClassLoader extends AbstractJPPFClassLoader
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFLocalClassLoader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * temporary IO handler.
   */
  private static LocalClassLoaderChannel channel = null;

  /**
   * Initialize this class loader with a parent class loader.
   * @param ioHandler wraps the communication channel with the driver.
   * @param parent a ClassLoader instance.
   */
  public JPPFLocalClassLoader(final LocalClassLoaderChannel ioHandler, final ClassLoader parent)
  {
    super(parent);
    channel = ioHandler;
    init();
  }

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   * @param uuidPath unique identifier for the submitting application.
   */
  public JPPFLocalClassLoader(final ClassLoader parent, final List<String> uuidPath)
  {
    super(parent, uuidPath);
  }

  /**
   * Initialize the underlying socket connection.
   */
  @Override
  protected void init()
  {
    LOCK.lock();
    try
    {
      if (INITIALIZING.compareAndSet(false, true))
      {
        synchronized(AbstractJPPFClassLoaderLifeCycle.class)
        {
          if (executor == null) executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("ClassloaderRequests"));
        }
        try
        {
          if (debugEnabled) log.debug("sending node initiation message");
          JPPFResourceWrapper resource = new JPPFResourceWrapper();
          resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
          resource.setData("node.uuid", NodeRunner.getUuid());
          channel.setServerResource(resource);
          channel.setNodeResource(null);
          synchronized(channel)
          {
            channel.setReadyOps(SelectionKey.OP_READ);
          }
          while (channel.getServerResource() != null) channel.getServerLock().goToSleep();
          if (debugEnabled) log.debug("node initiation message sent");
          synchronized(channel)
          {
            channel.setReadyOps(SelectionKey.OP_WRITE);
          }
          while (channel.getNodeResource() == null) channel.getNodeLock().goToSleep();
          channel.setNodeResource(null);
          if (debugEnabled) log.debug("received node initiation response");
          System.out.println(getClass().getSimpleName() + ".init(): Reconnected to the class server");
        }
        catch (Exception e)
        {
          throw new RuntimeException(e);
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
   * {@inheritDoc}
   */
  @Override
  public void reset()
  {
    init();
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
      executor.shutdownNow();
      executor = null;
    }
    finally
    {
      LOCK.unlock();
    }
  }

  /**
   * Load a JPPF class from the server.
   * @param name the binary name of the class
   * @return the resulting <tt>Class</tt> object
   * @throws ClassNotFoundException if the class could not be found
   */
  @Override
  public synchronized Class<?> loadJPPFClass(final String name) throws ClassNotFoundException
  {
    if (debugEnabled) log.debug("looking up resource [" + name + ']');
    Class<?> c = findLoadedClass(name);
    if (c == null)
    {
      if (debugEnabled) log.debug("resource [" + name + "] not already loaded");
      ClassLoader cl = this;
      while (cl instanceof AbstractJPPFClassLoader) cl = cl.getParent();
      if (cl != null)
      {
        int i = name.lastIndexOf('.');
        if (i >= 0)
        {
          String pkgName = name.substring(0, i);
          Package pkg = getPackage(pkgName);
          if (pkg == null)
          {
            definePackage(pkgName, null, null, null, null, null, null, null);
          }
        }
        String resName = name.replace(".", "/") + ".class";
        InputStream is = cl.getResourceAsStream(resName);
        try
        {
          byte[] definition = StreamUtils.getInputStreamAsByte(is);
          c = defineClass(name, definition, 0, definition.length);
        }
        catch(Exception e)
        {
          log.warn(e.getMessage(), e);
        }
      }
    }
    if (c == null)
    {
      c = findClass(name);
    }
    if (debugEnabled) log.debug("definition for resource [" + name + "] : " + c);
    return c;
  }

  /**
   * Load the specified class from a socket connection.
   * @param map contains the necessary resource request data.
   * @param asResource true if the resource is loaded using getResource(), false otherwise.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws Exception if the connection was lost and could not be reestablished.
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

    ResourceRequest request = new ResourceRequest(resource);
    Future<?> future = executor.submit(request);
    future.get();
    Throwable t = request.getThrowable();
    if (t != null)
    {
      if (t instanceof Exception) throw (Exception) t;
      throw (Error) t;
    }
    return request.getResponse();
  }

  /**
   * Encapsulates a remote resource request submitted asynchronously
   * via the single-thread executor.
   */
  protected class ResourceRequest extends AbstractResourceRequest
  {
    /**
     * Initialize with the specified request.
     * @param request the request to send.
     */
    public ResourceRequest(final JPPFResourceWrapper request)
    {
      super(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
      try
      {
        if (debugEnabled) log.debug("channel " + channel + " sending request " + request);
        synchronized(channel.getServerLock())
        {
          channel.setServerResource(request);
          channel.setReadyOps(SelectionKey.OP_READ);
          while (channel.getServerResource() != null) channel.getServerLock().goToSleep();
        }
        synchronized(channel.getNodeLock())
        {
          channel.setReadyOps(SelectionKey.OP_WRITE);
          while ((response = channel.getNodeResource()) == null) channel.getNodeLock().goToSleep();
          //response = channel.getNodeResource();
          channel.setNodeResource(null);
        }
        if (debugEnabled) log.debug("channel " + channel + " got response " + response);
      }
      catch (Throwable t)
      {
        throwable = t;
      }
    }
  }
}
