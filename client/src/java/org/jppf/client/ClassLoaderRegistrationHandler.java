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

package org.jppf.client;

import java.util.*;

import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Handles the class loaders used for inbound class loading requests from the servers.
 * @author Laurent Cohen
 * @since 4.1
 */
class ClassLoaderRegistrationHandler implements AutoCloseable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClassLoaderRegistrationHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Mapping of registered class loaders.
   */
  private final CollectionMap<String, ClassLoader> classLoaderRegistrations = new SetHashMap<>();

  /**
   * Get a class loader associated with a job.
   * @param uuid unique id assigned to classLoader. Added as temporary fix for problems hanging jobs.
   * @return a <code>RegisteredClassLoader</code> instance.
   */
  Collection<ClassLoader> getRegisteredClassLoaders(final String uuid) {
    if (uuid == null) throw new IllegalArgumentException("uuid is null");
    synchronized (classLoaderRegistrations) {
      Collection<ClassLoader> c = classLoaderRegistrations.getValues(uuid);
      if ((c == null) || c.isEmpty()) {
        // workaround for bug http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-237
        if (debugEnabled) log.debug("job '{}' may have been submitted by a different client instance, looking for an alternate class loader", uuid);
        ClassLoader rcl = null;
        Iterator<ClassLoader> it = classLoaderRegistrations.iterator();
        if (it.hasNext()) rcl = it.next();
        else {
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          if (cl == null) cl = getClass().getClassLoader();
          rcl = cl;
        }
        if (c == null) c = new ArrayList<>();
        c.add(rcl);
      }
      return c;
    }
  }

  /**
   * Register class loader with this submission manager.
   * @param cl a <code>ClassLoader</code> instance.
   * @param uuid unique id assigned to classLoader. Added as temporary fix for problems hanging jobs.
   * @return theresgistered class loader.
   */
  ClassLoader registerClassLoader(final ClassLoader cl, final String uuid) {
    if (cl == null) throw new IllegalArgumentException("cl is null");
    if (uuid == null) throw new IllegalArgumentException("uuid is null");
    synchronized (classLoaderRegistrations) {
      classLoaderRegistrations.putValue(uuid, cl);
    }
    if (debugEnabled) log.debug("registered {}", cl);
    return cl;
  }

  /**
   * Unregisters the class loader associated with the specified job uuid.
   * @param uuid the uuid of the job the class loaders are associated with.
   */
  void unregister(final String uuid) {
    if (uuid == null) throw new IllegalArgumentException("registeredClassLoader is null");
    if (debugEnabled) log.debug("unregistering {}", uuid);
    synchronized (classLoaderRegistrations) {
      classLoaderRegistrations.removeKey(uuid);
    }
  }

  @Override
  public void close() {
    classLoaderRegistrations.clear();
  }
}
