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

import static org.jppf.utils.StringUtils.build;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.caching.*;
import org.jppf.classloader.resource.ResourceCache;
import org.jppf.node.connection.ConnectionReason;
import org.jppf.node.protocol.TaskThreadLocals;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClassLoaderLifeCycle extends URLClassLoader {
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
   * Whether resources should be looked up in the file system if not found in the classpath.
   */
  private static final boolean FILE_LOOKUP = JPPFConfiguration.get(JPPFProperties.CLASSLOADER_FILE_LOOKUP);
  /**
   * Determines whether this class loader should handle dynamic class updating.
   * @exclude
   */
  protected boolean dynamic;
  /**
   * The unique identifier for the submitting application.
   * @exclude
   */
  protected List<String> uuidPath = new ArrayList<>();
  /**
   * The cache handling resources temporarily stored to file.
   * @exclude
   */
  protected ResourceCache resourceCache = createResourceCache();
  /**
   * The cache handling resources that were not found by this class loader.
   * @exclude
   */
  protected final JPPFCollectionCache<String> notFoundCache = new JPPFSimpleSetCache<>();
  /**
   * Uniquely identifies this class loader instance.
   * @exclude
   */
  protected final int instanceNumber = INSTANCE_COUNT.incrementAndGet();
  /**
   * The connection to the driver.
   * @exclude
   */
  protected ClassLoaderConnection<?> connection;
  /**
   * Determines whether this class laoder is in connected mode or not.
   */
  protected final boolean offline;
  /**
   * Determines whether this remote class loading is enabled.
   * @since 4.2
   * @exclude
   */
  protected final AtomicBoolean remoteClassLoadingDisabled = new AtomicBoolean(false);
  /**
   * To create and invoke hook instances.
   * @exclude
   */
  protected final HookFactory hookFactory;

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   * @param connection the connection to the driver.
   * @param uuidPath unique identifier for the submitting application.
   * @param hookFactory a {@link HookFactory} instance.
   * @exclude
   */
  protected AbstractJPPFClassLoaderLifeCycle(final ClassLoaderConnection<?> connection, final ClassLoader parent, final List<String> uuidPath, final HookFactory hookFactory) {
    super(StringUtils.ZERO_URL, parent);
    this.connection = connection;
    this.dynamic = parent instanceof AbstractJPPFClassLoaderLifeCycle;
    this.offline = dynamic ? ((AbstractJPPFClassLoaderLifeCycle) parent).isOffline() : connection == null;
    if (uuidPath != null) this.uuidPath = uuidPath;
    this.hookFactory = hookFactory;
    hookFactory.registerSPIMultipleHook(ClassLoaderListener.class, null, null);
    if (debugEnabled) log.debug("initalized {} with connection={}", this, connection);
  }

  /**
   * Initialize the connection to the driver.
   * @exclude
   */
  protected void init() {
    if (!isOffline()) {
      if (debugEnabled) log.debug("initializing class loader connection for {}", this);
      try {
        connection.init();
      } catch (final Exception e) {
        throw new JPPFNodeReconnectionNotification("Could not reconnect to the server", e, ConnectionReason.CLASSLOADER_INIT_ERROR);
      }
    } else {
      System.out.println("This node is 'offline', no class loader connection is established");
    }
  }


  /**
   * Reset and reinitialize the connection to the server.
   * @exclude
   */
  protected abstract void reset();

  /**
   * Load the specified class from a socket connection.
   * @param map contains the necessary resource request data.
   * @return a <code>JPPFResourceWrapper</code> containing the resource content.
   * @throws ClassNotFoundException if the class could not be loaded from the remote server.
   * @exclude
   */
  protected JPPFResourceWrapper loadResource(final Map<ResourceIdentifier, Object> map) throws ClassNotFoundException {
    JPPFResourceWrapper resource = null;
    if (!isRemoteClassLoadingDisabled()) {
      try {
        if (debugEnabled) log.debug(build(this, " loading remote definition for resource [", map.get("name"), "]"));
        map.put(ResourceIdentifier.FILE_LOOKUP_ALLOWED, FILE_LOOKUP);
        resource = connection.loadResource(map, dynamic, TaskThreadLocals.getRequestUuid(), uuidPath);
        if (debugEnabled) log.debug(build(this, " remote definition for resource [", map.get("name") + "] ", resource.getDefinition()==null ? "not " : "", "found"));
      } catch(final IOException e) {
        if (debugEnabled) log.debug(this.toString() + " connection with class server ended, re-initializing, exception is:", e);
        throw new JPPFNodeReconnectionNotification("connection with class server ended, re-initializing, exception is:", e, ConnectionReason.CLASSLOADER_PROCESSING_ERROR);
      } catch(final ClassNotFoundException e) {
        throw e;
      } catch(final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
      }
    }
    return resource;
  }

  @Override
  public void addURL(final URL url) {
    super.addURL(url);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[id=").append(instanceNumber).append(", type=").append(dynamic ? "client" : "server");
    sb.append(", uuidPath=").append(uuidPath);
    sb.append(", offline=").append(offline);
    final URL[] urls = getURLs();
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
  protected URL[] findMultipleResources(final String...names) {
    if ((names == null) || (names.length <= 0)) return StringUtils.ZERO_URL;
    final URL[] results = new URL[names.length];
    final boolean[] alreadyNotFound = new boolean[names.length];
    for (int i=0; i<names.length; i++) {
      results[i] = null;
      alreadyNotFound[i] = notFoundCache.has(names[i]);
    }
    try {
      final List<Integer> indices = new ArrayList<>();
      for (int i=0; i<names.length; i++) {
        if (alreadyNotFound[i]) continue;
        final String name = names[i];
        final List<URL> locationsList = resourceCache.isEnabled() ? resourceCache.getResourcesURLs(name) : null;
        if ((locationsList != null) && !locationsList.isEmpty()) {
          results[i] = locationsList.get(0);
          if (debugEnabled) log.debug(build(this, " resource ", name, " found in local cache as ", results[i]));
        } else {
          final URL url = super.findResource(names[i]);
          if (url != null) {
            results[i] = url;
            if (debugEnabled) log.debug(build(this, " resource ", name, " found in URL classpath as ", results[i]));
          } else {
            if (debugEnabled) log.debug(build("resource ", name, " not found locally"));
            indices.add(i);
          }
        }
      }
      if (indices.isEmpty() || isRemoteClassLoadingDisabled()) {
        if (debugEnabled) {
          if (isRemoteClassLoadingDisabled()) log.debug(this.toString() + " offline mode: resources were looked up locally only");
          else log.debug(this.toString() + " all resources were found locally");
        }
        return results;
      }
      final Map<ResourceIdentifier, Object> map = new EnumMap<>(ResourceIdentifier.class);
      final String[] namesToLookup = new String[indices.size()];
      for (int i=0; i<indices.size(); i++) namesToLookup[i] = names[indices.get(i)];
      map.put(ResourceIdentifier.NAME, StringUtils.arrayToString(", ", null, null, namesToLookup));
      map.put(ResourceIdentifier.MULTIPLE_NAMES, namesToLookup);
      final JPPFResourceWrapper resource = loadResource(map);
      final Map<String, List<byte[]>> dataMap = (Map<String, List<byte[]>>) resource.getData(ResourceIdentifier.RESOURCE_MAP);
      for (final Integer index : indices) {
        final String name = names[index];
        final List<byte[]> dataList = dataMap.get(name);
        final boolean found = (dataList != null) && !dataList.isEmpty();
        if (debugEnabled && !found) log.debug(build(this, " resource [", name, "] not found remotely"));
        if (found) {
          resourceCache.registerResources(name, dataList);
          final URL url = resourceCache.getResourceURL(name);
          results[index] = url;
          if (debugEnabled) log.debug(build(this, " resource [", name, "] found remotely as ", url));
        }
        else if (resource.getState() != JPPFResourceWrapper.State.NODE_RESPONSE_ERROR) notFoundCache.add(name);
      }
    } catch(final Exception e) {
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
    final int length = names.length;
    URL[] results = new URL[length];
    final boolean[] alreadyNotFound = new boolean[length];
    for (int i=0; i<length; i++) {
      results[i] = null;
      alreadyNotFound[i] = notFoundCache.has(names[i]);
    }
    try {
      final ClassLoader parent = getParent();
      if (parent == null) {
        for (int i=0; i<length; i++) if (!alreadyNotFound[i]) results[i] = getSystemResource(names[i]);
      } else if (!(parent instanceof AbstractJPPFClassLoader)) {
        for (int i=0; i<length; i++) if (!alreadyNotFound[i]) results[i] = parent.getResource(names[i]);
      } else {
        results = ((AbstractJPPFClassLoader) parent).getMultipleResources(names);
      }
      for (int i=0; i<length; i++) if (results[i] == null) results[i] = super.getResource(names[i]);
      final List<Integer> indices = new ArrayList<>();
      for (int i=0; i<length; i++) if (results[i] == null) indices.add(i);
      if (!indices.isEmpty()) {
        final String[] namesToFind = new String[indices.size()];
        for (int i=0; i<namesToFind.length; i++) namesToFind[i] = names[indices.get(i)];
        final URL[] foundURLs = findMultipleResources(namesToFind);
        for (int i=0; i<namesToFind.length; i++) results[indices.get(i)] = foundURLs[i];
      }
    } catch(final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return results;
  }

  /**
   * Get the connection to the driver.
   * @return a {@link ClassLoaderConnection} instance.
   * @exclude
   */
  public ClassLoaderConnection<?> getConnection() {
    return connection;
  }

  /**
   * Get the uuid of the JPPF client this class loader gets resources from.
   * @return a client uuid as a string, or <code>null</code> if this class cloader is not a client class loader.
   */
  public String getClientUuid() {
    if (!dynamic) return null;
    return uuidPath.get(0);
  }

  /**
   * Reset this class loader's resource cache. This method actually clears the
   * cache, including removing the locally stored temp files, then creates a
   * new cache instance.
   */
  public void resetResourceCache() {
    if (resourceCache != null) {
      resourceCache.close();
      resourceCache = createResourceCache();
    }
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing classloader " + this);
    try {
      super.close();
    } catch (final IOException|NoSuchMethodError e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else if (!isOffline()) log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Determine whether this class loader is in offline mode or not.
   * @return <code>true</code> if this class loader is offline, <code>false</code> otherwise.
   */
  public boolean isOffline() {
    return offline;
  }

  /**
   * Specify whether remote class loading is disabled.
   * @param disabled <code>true</code> to disable remote class loading, <code>false</code> to enable it.
   * @since 4.2
   * @exclude
   */
  public void setRemoteClassLoadingDisabled(final boolean disabled) {
    remoteClassLoadingDisabled.set(disabled);
    if (dynamic) ((AbstractJPPFClassLoaderLifeCycle) getParent()).setRemoteClassLoadingDisabled(disabled);
  }

  /**
   * Detrmine whether remote class loading is enabled.
   * @return <code>true</code> if remote class loading is disabled, <code>false</code> if enabled.
   * @since 4.2
   * @exclude
   */
  public boolean isRemoteClassLoadingDisabled() {
    return remoteClassLoadingDisabled.get() || isOffline();
  }

  /**
   * Get the resource cache handled by this class loader.
   * @return an instance of {@link ResourceCache}.
   * @exclude
   */
  public ResourceCache getResourceCache() {
    return resourceCache;
  }

  /**
   * Create a new resource cache instance.
   * @return a {@code ResourceCache} object.
   */
  private static ResourceCache createResourceCache() {
    final boolean enabled = JPPFConfiguration.get(JPPFProperties.RESOURCE_CACHE_ENABLED);
    return new ResourceCache(enabled);
  }

  /**
   * Get the factory that creates and invoke hook instances for this node.
   * @return a {@link HookFactory} instance.
   * @exclude
   */
  public HookFactory getHookFactory() {
    return hookFactory;
  }
}
