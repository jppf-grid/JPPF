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

import static org.jppf.utils.StringUtils.build;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.caching.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractJPPFClassLoaderLifeCycle extends URLClassLoader
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClassLoaderLifeCycle.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  protected static final ReentrantLock LOCK = new ReentrantLock();
  /**
   * Determines whether this class loader should handle dynamic class updating.
   */
  protected static final AtomicBoolean INITIALIZING = new AtomicBoolean(false);
  /**
   * Determines whether this class loader should handle dynamic class updating.
   */
  private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
  /**
   * 
   */
  protected static ClassLoaderRequestHandler requestHandler = null;
  /**
   * Determines whether this class loader should handle dynamic class updating.
   */
  protected boolean dynamic = false;
  /**
   * The unique identifier for the submitting application.
   */
  protected List<String> uuidPath = new ArrayList<String>();
  /**
   * Uuid of the original task bundle that triggered a resource loading request.
   */
  protected String requestUuid = null;
  /**
   * The cache handling resources temporarily stored to file.
   */
  protected final ResourceCache cache = new ResourceCache();
  /**
   * The cache handling resources that were not found by this class loader.
   */
  protected final JPPFCollectionCache<String> nfCache = new JPPFSimpleSetCache<String>();
  /**
   * The object used to serialize and deserialize resources.
   */
  protected ObjectSerializer serializer = null;
  /**
   * Uniquely identifies this class loader instance.
   */
  protected final int instanceNumber = INSTANCE_COUNT.incrementAndGet();

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   */
  protected AbstractJPPFClassLoaderLifeCycle(final ClassLoader parent)
  {
    super(StringUtils.ZERO_URL, parent);
    if (parent instanceof AbstractJPPFClassLoaderLifeCycle) dynamic = true;
  }

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   * @param uuidPath unique identifier for the submitting application.
   */
  protected AbstractJPPFClassLoaderLifeCycle(final ClassLoader parent, final List<String> uuidPath)
  {
    this(parent);
    this.uuidPath = uuidPath;
  }

  /**
   * Initialize the underlying socket connection.
   */
  protected abstract void init();
  /**
   * Reset and reinitialize the connection ot the server.
   */
  public abstract void reset();

  /**
   * Load the specified class from a socket connection.
   * @param map contains the necessary resource request data.
   * @param asResource true if the resource is loaded using getResource(), false otherwise.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws ClassNotFoundException if the class could not be loaded from the remote server.
   */
  protected JPPFResourceWrapper loadResourceData(final Map<String, Object> map, final boolean asResource) throws ClassNotFoundException
  {
    JPPFResourceWrapper resource = null;
    try
    {
      if (debugEnabled) log.debug(build("loading remote definition for resource [", map.get("name"), "]"));
      resource = loadResourceData0(map, asResource);
    }
    catch(IOException e)
    {
      if (debugEnabled) log.debug("connection with class server ended, re-initializing, exception is:", e);
      throw new JPPFNodeReconnectionNotification("connection with class server ended, re-initializing, exception is:", e);
    }
    catch(ClassNotFoundException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return resource;
  }

  /**
   * Load the specified class from a socket connection.
   * @param map contains the necessary resource request data.
   * @param asResource true if the resource is loaded using getResource(), false otherwise.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws Exception if the connection was lost and could not be reestablished.
   */
  protected  JPPFResourceWrapper loadResourceData0(final Map<String, Object> map, final boolean asResource) throws Exception
  {
    if (debugEnabled) log.debug(build("loading remote definition for resource [", map.get("name"), "], requestUuid = ", requestUuid));
    JPPFResourceWrapper resource = loadRemoteData(map, false);
    if (debugEnabled) log.debug(build("remote definition for resource [", map.get("name") + "] ", resource.getDefinition()==null ? "not " : "", "found"));
    return resource;
  }

  /**
   * Load the specified class from a socket connection.
   * @param map contains the necessary resource request data.
   * @param asResource true if the resource is loaded using getResource(), false otherwise.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws Exception if the connection was lost and could not be reestablished.
   */
  protected abstract JPPFResourceWrapper loadRemoteData(Map<String, Object> map, boolean asResource) throws Exception;

  /**
   * Determine whether the socket client is being initialized.
   * @return true if the socket client is being initialized, false otherwise.
   */
  static boolean isInitializing()
  {
    return INITIALIZING.get();
  }

  /**
   * Set the socket client initialization status.
   * @param initFlag true if the socket client is being initialized, false otherwise.
   */
  static void setInitializing(final boolean initFlag)
  {
    INITIALIZING.set(initFlag);
  }

  /**
   * Set the uuid for the original task bundle that triggered this resource request.
   * @param requestUuid the uuid as a string.
   */
  public void setRequestUuid(final String requestUuid)
  {
    this.requestUuid = requestUuid;
  }

  /**
   * Terminate this classloader and clean the resources it uses.
   */
  public abstract void close();

  /**
   * Get the object used to serialize and deserialize resources.
   * @return an {@link ObjectSerializer} instance.
   * @throws Exception if any error occurs.
   */
  protected ObjectSerializer getSerializer() throws Exception
  {
    if (serializer == null) serializer = (ObjectSerializer) getParent().loadClass("org.jppf.comm.socket.BootstrapObjectSerializer").newInstance();
    return serializer;
  }

  @Override
  public void addURL(final URL url)
  {
    super.addURL(url);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[id=").append(instanceNumber).append(", type=").append(dynamic ? "client" : "server");
    URL[] urls = getURLs();
    if ((urls != null) && (urls.length > 0))
    {
      sb.append(", classpath=");
      for (int i=0; i<urls.length; i++)
      {
        if (i > 0) sb.append(';');
        sb.append(urls[i]);
      }
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get multiple resources, specified by their names, from the classpath.
   * This method functions like #getResource(String), except that it look up and returns multiple URLs.
   * @param names the names of te resources to find.
   * @return an array of URLs, one for each looked up resources. Some URLs may be null, however the returned array
   * is never null, and results are in the same order as the specified resource names.
   */
  @SuppressWarnings("unchecked")
  protected URL[] findMultipleResources(final String...names)
  {
    if ((names == null) || (names.length <= 0)) return StringUtils.ZERO_URL;
    URL[] results = new URL[names.length];
    boolean[] alreadyNotFound = new boolean[names.length];
    for (int i=0; i<names.length; i++) {
      results[i] = null;
      alreadyNotFound[i] = nfCache.has(names[i]);
    }
    try {
      List<Integer> indices = new ArrayList<Integer>();
      for (int i=0; i<names.length; i++) {
        if (alreadyNotFound[i]) continue;
        String name = names[i];
        List<URL> locationsList = cache.getResourcesURLs(name);
        if ((locationsList != null) && !locationsList.isEmpty()) {
          results[i] = locationsList.get(0);
          if (debugEnabled) log.debug(build("resource ", name, " found in local cache as ", results[i]));
        } else {
          URL url = super.findResource(names[i]);
          if (url != null) {
            results[i] = url;
            if (debugEnabled) log.debug(build("resource ", name, " found in URL classpath as ", results[i]));
          } else {
            if (debugEnabled) log.debug(build("resource ", name, " not found locally"));
            indices.add(i);
          }
        }
      }
      if (indices.isEmpty()) {
        if (debugEnabled) log.debug("all resources were found locally");
        return results;
      }
      Map<String, Object> map = new HashMap<String, Object>();
      String[] namesToLookup = new String[indices.size()];
      for (int i=0; i<indices.size(); i++) namesToLookup[i] = names[indices.get(i)];
      map.put("name", StringUtils.arrayToString(", ", null, null, namesToLookup));
      map.put("multiple.resources.names", namesToLookup);
      JPPFResourceWrapper resource = loadResourceData(map, true);
      Map<String, List<byte[]>> dataMap = (Map<String, List<byte[]>>) resource.getData("resource_map");
      for (Integer index : indices) {
        String name = names[index];
        List<byte[]> dataList = dataMap.get(name);
        boolean found = (dataList != null) && !dataList.isEmpty();
        if (debugEnabled && !found) log.debug(build("resource [", name, "] not found remotely"));
        if (found) {
          cache.registerResources(name, dataList);
          URL url = cache.getResourceURL(name);
          results[index] = url;
          if (debugEnabled) log.debug(build("resource [", name, "] found remotely as ", url));
        }
        else if (resource != null) nfCache.add(name);
      }
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
    return results;
  }

  /**
   * Get multiple resources, specified by their names, from the classpath.
   * This method functions like #getResource(String), except that it looks up and returns multiple URLs.
   * @param names the names of te resources to find.
   * @return an array of URLs, one for each looked up resources. Some URLs may be null, however the returned array
   * is never null, and results are in the same order as the specified resource names.
   */
  public URL[] getMultipleResources(final String...names)
  {
    if ((names == null) || (names.length <= 0)) return StringUtils.ZERO_URL;
    int length = names.length;
    URL[] results = new URL[length];
    boolean[] alreadyNotFound = new boolean[length];
    for (int i=0; i<length; i++)
    {
      results[i] = null;
      alreadyNotFound[i] = nfCache.has(names[i]);
    }
    try
    {
      ClassLoader parent = getParent();
      if (parent == null)
      {
        for (int i=0; i<length; i++) if (!alreadyNotFound[i]) results[i] = getSystemResource(names[i]);
      }
      else if (!(parent instanceof AbstractJPPFClassLoader))
      {
        for (int i=0; i<length; i++) if (!alreadyNotFound[i]) results[i] = parent.getResource(names[i]);
      }
      else
      {
        results = ((AbstractJPPFClassLoader) parent).getMultipleResources(names);
      }
      for (int i=0; i<length; i++) if (results[i] == null) results[i] = super.getResource(names[i]);
      List<Integer> indices = new ArrayList<Integer>();
      for (int i=0; i<length; i++) if (results[i] == null) indices.add(i);
      if (!indices.isEmpty())
      {
        String[] namesToFind = new String[indices.size()];
        for (int i=0; i<namesToFind.length; i++) namesToFind[i] = names[indices.get(i)];
        URL[] foundURLs = findMultipleResources(namesToFind);
        for (int i=0; i<namesToFind.length; i++) results[indices.get(i)] = foundURLs[i];
      }
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return results;
  }
}
