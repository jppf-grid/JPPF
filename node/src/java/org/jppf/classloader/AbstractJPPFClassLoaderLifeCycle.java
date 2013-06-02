/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.caching.*;
import org.jppf.classloader.resource.ResourceCache;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
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
   * Determines whether this class loader should handle dynamic class updating.
   * @exclude
   */
  private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
  /**
   * Determines whether this class loader should handle dynamic class updating.
   */
  protected final boolean dynamic;
  /**
   * The unique identifier for the submitting application.
   * @exclude
   */
  protected List<String> uuidPath = new ArrayList<String>();
  /**
   * Uuid of the original task bundle that triggered a resource loading request.
   * @exclude
   */
  protected String requestUuid = null;
  /**
   * The cache handling resources temporarily stored to file.
   * @exclude
   */
  protected ResourceCache cache = new ResourceCache();
  /**
   * The cache handling resources that were not found by this class loader.
   * @exclude
   */
  protected final JPPFCollectionCache<String> notFoundCache = new JPPFSimpleSetCache<String>();
  /**
   * Uniquely identifies this class loader instance.
   * @exclude
   */
  protected final int instanceNumber = INSTANCE_COUNT.incrementAndGet();
  /**
   * The connection to the driver.
   */
  protected ClassLoaderConnection<?> connection;
  /**
   * The list of listeners to this class loader.
   */
  protected final List<ClassLoaderListener> listeners = new CopyOnWriteArrayList<ClassLoaderListener>();

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   * @param connection the connection to the driver.
   * @param uuidPath unique identifier for the submitting application.
   * @exclude
   */
  protected AbstractJPPFClassLoaderLifeCycle(final ClassLoaderConnection connection, final ClassLoader parent, final List<String> uuidPath) {
    super(StringUtils.ZERO_URL, parent);
    this.connection = connection;
    this.dynamic = parent instanceof AbstractJPPFClassLoaderLifeCycle;
    if (uuidPath != null) this.uuidPath = uuidPath;
    listeners.addAll(ClassLoaderListenerHandler.getInstance().getListeners());
  }

  /**
   * Initialize the connection to the driver.
   * @exclude
   */
  protected void init() {
    try {
      connection.init();
    } catch (Exception e) {
      throw new JPPFNodeReconnectionNotification("Could not reconnect to the server", e);
    }
  }


  /**
   * Reset and reinitialize the connection to the server.
   * @exclude
   */
  public abstract void reset();

  /**
   * Load the specified class from a socket connection.
   * @param map contains the necessary resource request data.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws ClassNotFoundException if the class could not be loaded from the remote server.
   * @exclude
   */
  protected JPPFResourceWrapper loadResource(final Map<String, Object> map) throws ClassNotFoundException {
    JPPFResourceWrapper resource = null;
    try {
      if (debugEnabled) log.debug(build("loading remote definition for resource [", map.get("name"), "]"));
      resource = connection.loadResource(map, dynamic, requestUuid, uuidPath);
      if (debugEnabled) log.debug(build("remote definition for resource [", map.get("name") + "] ", resource.getDefinition()==null ? "not " : "", "found"));
    } catch(IOException e) {
      if (debugEnabled) log.debug("connection with class server ended, re-initializing, exception is:", e);
      throw new JPPFNodeReconnectionNotification("connection with class server ended, re-initializing, exception is:", e);
    } catch(ClassNotFoundException e) {
      throw e;
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return resource;
  }

  /**
   * Get the uuid for the original task bundle that triggered this resource request.
   * @return the uuid as a string.
   */
  public String getRequestUuid() {
    return requestUuid;
  }

  /**
   * Set the uuid for the original task bundle that triggered this resource request.
   * @param requestUuid the uuid as a string.
   * @exclude
   */
  public void setRequestUuid(final String requestUuid) {
    this.requestUuid = requestUuid;
  }

  @Override
  public void addURL(final URL url) {
    super.addURL(url);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[id=").append(instanceNumber).append(", type=").append(dynamic ? "client" : "server");
    sb.append(", uuidPath=").append(uuidPath);
    URL[] urls = getURLs();
    sb.append(", classpath=");
    if ((urls != null) && (urls.length > 0)) {
      for (int i=0; i<urls.length; i++) {
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
   * @exclude
   */
  @SuppressWarnings("unchecked")
  protected URL[] findMultipleResources(final String...names)
  {
    if ((names == null) || (names.length <= 0)) return StringUtils.ZERO_URL;
    URL[] results = new URL[names.length];
    boolean[] alreadyNotFound = new boolean[names.length];
    for (int i=0; i<names.length; i++) {
      results[i] = null;
      alreadyNotFound[i] = notFoundCache.has(names[i]);
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
      JPPFResourceWrapper resource = loadResource(map);
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
        else if (resource != null) notFoundCache.add(name);
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
  public URL[] getMultipleResources(final String...names) {
    if ((names == null) || (names.length <= 0)) return StringUtils.ZERO_URL;
    int length = names.length;
    URL[] results = new URL[length];
    boolean[] alreadyNotFound = new boolean[length];
    for (int i=0; i<length; i++) {
      results[i] = null;
      alreadyNotFound[i] = notFoundCache.has(names[i]);
    }
    try {
      ClassLoader parent = getParent();
      if (parent == null) {
        for (int i=0; i<length; i++) if (!alreadyNotFound[i]) results[i] = getSystemResource(names[i]);
      } else if (!(parent instanceof AbstractJPPFClassLoader)) {
        for (int i=0; i<length; i++) if (!alreadyNotFound[i]) results[i] = parent.getResource(names[i]);
      } else {
        results = ((AbstractJPPFClassLoader) parent).getMultipleResources(names);
      }
      for (int i=0; i<length; i++) if (results[i] == null) results[i] = super.getResource(names[i]);
      List<Integer> indices = new ArrayList<Integer>();
      for (int i=0; i<length; i++) if (results[i] == null) indices.add(i);
      if (!indices.isEmpty()) {
        String[] namesToFind = new String[indices.size()];
        for (int i=0; i<namesToFind.length; i++) namesToFind[i] = names[indices.get(i)];
        URL[] foundURLs = findMultipleResources(namesToFind);
        for (int i=0; i<namesToFind.length; i++) results[indices.get(i)] = foundURLs[i];
      }
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return results;
  }

  /**
   * Get the connection to the driver.
   * @return a {@link ClassLoaderConnection} instance.
   * @exclude 
   */
  public ClassLoaderConnection getConnection() {
    return connection;
  }

  /**
   * Get the uuid of the JPPF client this class laoder gets resources from.
   * @return a client uuid as a string, or <code>null</code> if this class cloader is not a client class loader.
   */
  public String getClientUuid() {
    if (!dynamic) return null;
    return uuidPath.get(0);
  }

  /**
   * Add the specified listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addClassLoaderListener(final ClassLoaderListener listener) {
    if (listener == null) throw new IllegalArgumentException("cannot add a null listener");
    listeners.add(listener);
  }

  /**
   * Remove the specified listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeClassLoaderListener(final ClassLoaderListener listener) {
    if (listener == null) throw new IllegalArgumentException("cannot remove a null listener");
    listeners.remove(listener);
  }

  /**
   * Reset this class loader's resource cache. This method actually clears the
   * cache, including removing the locally stored temp files, then creates a
   * new cache instance.
   */
  public void resetResourceCache() {
    if (cache != null) {
      cache.close();
      cache = new ResourceCache();
    }
  }

  /**
   * This method does nothing.
   */
  public void close()
  {
  }
}
