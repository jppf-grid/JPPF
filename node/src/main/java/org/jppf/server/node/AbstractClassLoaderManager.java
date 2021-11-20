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

package org.jppf.server.node;

import java.security.*;
import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.classloader.*;
import org.jppf.node.protocol.TaskThreadLocals;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class manage the node's class loader and associated operations.
 * @param <N> the type of node.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractClassLoaderManager<N extends JPPFNode> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractClassLoaderManager.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Maximum number of containers kept by this node's cache.
   */
  private final int maxContainers;
  /**
   * Class loader used for dynamic loading and updating of client classes.
   */
  private AbstractJPPFClassLoader classLoader = null;
  /**
   * Mapping of containers to their corresponding application uuid.
   */
  private final Map<String, JPPFContainer> containerMap = new HashMap<>();
  /**
   * A list retaining the container in chronological order of their creation.
   */
  private final LinkedList<JPPFContainer> containerList = new LinkedList<>();
  /**
   * The associated node.
   */
  protected final N node;

  /**
   * Default constructor for class loader manager.
   * @param node the associated node.
   */
  protected AbstractClassLoaderManager(final N node) {
    if (node == null) throw new IllegalArgumentException("node is null");
    this.node = node;
    this.maxContainers = node.getConfiguration().get(JPPFProperties.CLASSLOADER_CACHE_SIZE);
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @return a <code>ClassLoader</code> used for loading the classes of the framework.
   */
  public synchronized AbstractJPPFClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = createClassLoader();
      if (debugEnabled) log.debug("created class loader " + classLoader);
    }
    return classLoader;
  }

  /**
   * Create the main class loader for the node.
   * @return a {@link JPPFClassLoader} instance.
   */
  protected abstract AbstractJPPFClassLoader createClassLoader();

  /**
   * Set the main class loader for the node.
   * @param cl the class loader to set.
   */
  public synchronized void setClassLoader(final AbstractJPPFClassLoader cl) {
    classLoader = cl;
  }

  /**
   * Close main class loader for the node.
   */
  public synchronized void closeClassLoader() {
    if (classLoader != null) {
      try {
        classLoader.close();
      } catch(final Throwable t) {
        log.error(t.getMessage(), t);
      } finally {
        classLoader = null;
      }
    }
  }

  /**
   * Get a reference to the JPPF container associated with an application uuid.
   * @param uuidPath the uuid path containing the key to the container.
   * @param params a (possibly empty) set of arbitrary parameters to propagate to the class loader.
   * @return a <code>JPPFContainer</code> instance.
   * @throws Exception if an error occurs while getting the container.
   */
  public JPPFContainer getContainer(final List<String> uuidPath, final Object...params) throws Exception {
    return getContainer(uuidPath, true, params);
  }

  /**
   * Get a reference to the JPPF container associated with an application uuid.
   * @param uuidPath the uuid path containing the key to the container.
   * @param clientAccess whether the node has access to the client that submitted the job.
   * @param params a (possibly empty) set of arbitrary parameters to propagate to the class loader.
   * @return a {@code JPPFContainer} instance.
   * @throws Exception if an error occurs while getting the container.
   */
  public synchronized JPPFContainer getContainer(final List<String> uuidPath, final boolean clientAccess, final Object...params) throws Exception {
    final String uuid = uuidPath.get(0);
    JPPFContainer container = containerMap.get(uuid);
    if (container == null) {
      if (debugEnabled) log.debug("Creating new container for appuuid=" + uuid);
      final AbstractJPPFClassLoader cl = newClientClassLoader(uuidPath, params);
      container = newJPPFContainer(uuidPath, cl, clientAccess);
      if (containerList.size() >= maxContainers) {
        final JPPFContainer toRemove = containerList.removeFirst();
        try {
          clearContainer(toRemove);
        } finally {
          toRemove.helper = null;
          toRemove.classLoader = null;
          containerMap.remove(toRemove.getAppUuid());
        }
      }
      containerList.add(container);
      containerMap.put(uuid, container);
    }
    return container;
  }

  /**
   * Create a new client class loader instance for the specified uuid path.
   * @param uuidPath the uuid path uniquely identifying the client.
   * @param params a (possibly empty) set of arbitrary parameters to propagate to the class loader.
   * @return a {@link AbstractJPPFClassLoader} instance or <code>null</code> if the class laoder could not be created.
   */
  protected AbstractJPPFClassLoader newClientClassLoader(final List<String> uuidPath, final Object...params) {
    return AccessController.doPrivileged(new PrivilegedAction<AbstractJPPFClassLoader>() {
      @Override
      public AbstractJPPFClassLoader run() {
        try {
          return newClassLoaderCreator(uuidPath, params).call();
        } catch(final Exception e) {
          log.error(e.getMessage(), e);
        }
        return null;
      }
    });
  }

  /**
   * Clear all containers associated with applications uuids.
   */
  public synchronized void clearContainers() {
    closeClassLoader();
    try {
      for (JPPFContainer container : containerList) clearContainer(container);
    } finally {
      containerMap.clear();
      containerList.clear();
    }
  }

  /**
   * Clear the specified container and cleanup its class loader.
   * @param container th container to clean up.
   */
  protected void clearContainer(final JPPFContainer container) {
    final AbstractJPPFClassLoader loader = container.getClassLoader();
    if (loader != null) loader.close();
  }

  /**
   * Create a new container based on the uuid path and class loader.
   * @param uuidPath uuid path for the corresponding client.
   * @param cl the class loader to use.
   * @param clientAccess whether the node has access to the client that submitted the job.
   * @return a {@link JPPFContainer} instance.
   * @throws Exception if any error occurs.
   */
  protected abstract JPPFContainer newJPPFContainer(List<String> uuidPath, AbstractJPPFClassLoader cl, final boolean clientAccess) throws Exception;

  /**
   * Instantiate the callback used to create the class loader in each {@link JPPFContainer}.
   * @param uuidPath the uuid path containing the key to the container.
   * @param params a (possibly empty) set of arbitrary parameters to propagate to the class loader.
   * @return a {@link Callable} instance.
   */
  protected abstract Callable<AbstractJPPFClassLoader> newClassLoaderCreator(List<String> uuidPath, final Object...params);

  /**
   * Clear the resource caches of all class loaders managed by this object.
   */
  public void clearResourceCaches() {
    for (final JPPFContainer cont: containerList) {
      final AbstractJPPFClassLoader cl = cont.getClassLoader();
      if (cl != null) cl.resetResourceCache();
    }
    if (classLoader != null) classLoader.resetResourceCache();
  }

  /**
   * Reset the class loader specified by its uuid path.
   * @param uuidPath uuid path of the class loader to reset.
   * @param params a (possibly empty) set of arbitrary parameters to propagate to the class loader.
   * @return the new created class loader istance.
   * @throws Exception if any error occurs.
   */
  public AbstractJPPFClassLoader resetClassLoader(final List<String> uuidPath, final Object...params) throws Exception {
    final JPPFContainer cont = getContainer(uuidPath);
    final AbstractJPPFClassLoader oldCL = cont.getClassLoader();
    final String requestUuid = TaskThreadLocals.getRequestUuid();
    final AbstractJPPFClassLoader newCL = newClientClassLoader(cont.uuidPath, params);
    TaskThreadLocals.setRequestUuid(requestUuid);
    cont.setClassLoader(newCL);
    oldCL.close();
    return newCL;
  }
}
