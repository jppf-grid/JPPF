/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.client;

import java.util.*;

import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Handles the class loaders used for inbound class loading requests from the servers.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public class ClassLoaderRegistrationHandler implements AutoCloseable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractGenericClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of registered class loaders.
   */
  private final CollectionMap<String, RegisteredClassLoader> classLoaderRegistrations = new SetHashMap<>();

  /**
   * Get a class loader associated with a job.
   * @param uuid unique id assigned to classLoader. Added as temporary fix for problems hanging jobs.
   * @return a <code>RegisteredClassLoader</code> instance.
   * @exclude
   */
  public RegisteredClassLoader getRegisteredClassLoader(final String uuid) {
    if (uuid == null) throw new IllegalArgumentException("uuid is null");
    synchronized (classLoaderRegistrations) {
      Collection<RegisteredClassLoader> c = classLoaderRegistrations.getValues(uuid);
      if ((c == null) || c.isEmpty()) {
        //throw new IllegalStateException("no class loader found for requestUuid=" + uuid);
        // workaround for bug http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-237
        if (debugEnabled) log.debug("job '{}' may have been submitted by a different client instance, looking for an alternate class loader", uuid);
        Iterator<RegisteredClassLoader> it = classLoaderRegistrations.iterator();
        if (it.hasNext()) return it.next();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = getClass().getClassLoader();
        return new RegisteredClassLoader(this, uuid, cl);
      }
      return c.iterator().next();
    }
  }

  /**
   * Register class loader with this submission manager.
   * @param cl a <code>ClassLoader</code> instance.
   * @param uuid unique id assigned to classLoader. Added as temporary fix for problems hanging jobs.
   * @return a <code>RegisteredClassLoader</code> instance.
   * @exclude
   */
  public RegisteredClassLoader registerClassLoader(final ClassLoader cl, final String uuid) {
    if (cl == null) throw new IllegalArgumentException("cl is null");
    if (uuid == null) throw new IllegalArgumentException("uuid is null");
    RegisteredClassLoader registeredClassLoader;
    synchronized (classLoaderRegistrations) {
      registeredClassLoader = new RegisteredClassLoader(this, uuid, cl);
      classLoaderRegistrations.putValue(uuid, registeredClassLoader);
    }
    if (debugEnabled) log.debug("registered {}", registeredClassLoader);
    return registeredClassLoader;
  }

  /**
   * Unregisters class loader from this submission manager.
   * @param registeredClassLoader a <code>RegisteredClassLoader</code> instance.
   * @exclude
   */
  protected void unregister(final RegisteredClassLoader registeredClassLoader) {
    if (registeredClassLoader == null) throw new IllegalArgumentException("registeredClassLoader is null");
    if (debugEnabled) log.debug("unregistering {}", registeredClassLoader);
    synchronized (classLoaderRegistrations) {
      classLoaderRegistrations.removeValue(registeredClassLoader.getUuid(), registeredClassLoader);
    }
  }

  /**
   * Helper class for managing registered class loaders.
   * @exclude
   */
  public static class RegisteredClassLoader {
    /**
     *The object which holds and registers this registered class loader.
     */
    private final ClassLoaderRegistrationHandler handler;
    /**
     * Unique id assigned to class loader.
     */
    private final String uuid;
    /**
     * A <code>ClassLoader</code> instance.
     */
    private final ClassLoader classLoader;

    /**
     * Initialize this registered class laoder.
     * @param handler the object which holds and registers this registered class loader.
     * @param uuid unique id assigned to classLoader
     * @param classLoader a <code>ClassLoader</code> instance.
     */
    RegisteredClassLoader(final ClassLoaderRegistrationHandler handler, final String uuid, final ClassLoader classLoader) {
      this.handler = handler;
      this.uuid = uuid;
      this.classLoader = classLoader;
    }

    /**
     * Get unique id assigned to class loader.
     * @return an id assigned to <code>ClassLoader</code>
     */
    public String getUuid() {
      return uuid;
    }

    /**
     * Get a class loader instance.
     * @return a <code>ClassLoader</code> instance.
     */
    public ClassLoader getClassLoader() {
      return classLoader;
    }

    /**
     * Disposes this registration for classLoader.
     */
    public void dispose() {
      handler.unregister(this);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
      sb.append("classLoader=").append(classLoader);
      sb.append(", uuid=").append(uuid);
      sb.append(']');
      return sb.toString();
    }
  }

  @Override
  public void close() {
    classLoaderRegistrations.clear();
  }
}
