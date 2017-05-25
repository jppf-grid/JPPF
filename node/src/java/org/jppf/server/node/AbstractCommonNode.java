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

package org.jppf.server.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.management.JMXServer;
import org.jppf.node.AbstractNode;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is used as a container for common methods that cannot be implemented in {@link AbstractNode}.
 * @author Laurent Cohen
 */
public abstract class AbstractCommonNode extends AbstractNode {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractCommonNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Manages the class loaders and how they are used.
   * @exclude
   */
  protected AbstractClassLoaderManager classLoaderManager = null;
  /**
   * Flag which determines whether a reset of the resource caches
   * should be performed at the next opportunity.
   */
  AtomicBoolean cacheResetFlag = new AtomicBoolean(false);
  /**
   * Flag indicating whether a node shutdown or restart has been requested.
   * @since 5.0
   */
  final AtomicBoolean shutdownRequestFlag = new AtomicBoolean(false);
  /**
   * Flag indicating whether it is a shutdown or restart that was last requested.
   * @since 5.0
   */
  final AtomicBoolean restart = new AtomicBoolean(false);
  /**
   * Determines whetehr the node is currently processing tasks.
   * @since 5.0
   */
  boolean executing = false;
  /**
   * Flag indicating whether the node is suspended, i.e. it is still alive but has stopped taking on new jobs.
   * @since 5.2
   */
  final AtomicBoolean suspended = new AtomicBoolean(false);
  /**
   * Lock for synchronization on the suspended state.
   */
  final ThreadSynchronization suspendedLock = new ThreadSynchronization();
  /**
   * Flag indicating whether the node is suspended, i.e. it is still alive but has stopped taking on new jobs.
   * @since 5.2
   */
  final AtomicBoolean reading = new AtomicBoolean(false);

  /**
   * Add management parameters to the specified bundle, before sending it back to a server.
   * @param bundle the bundle to add parameters to.
   * @exclude
   */
  protected void setupBundleParameters(final TaskBundle bundle) {
    try {
      JMXServer jmxServer = getJmxServer();
      bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
      bundle.setParameter(BundleParameter.NODE_PROVISIONING_MASTER, isMasterNode());
      bundle.setParameter(BundleParameter.NODE_PROVISIONING_SLAVE, isSlaveNode());
      bundle.setParameter(BundleParameter.NODE_DOTNET_CAPABLE, isDotnetCapable());
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @return a <code>ClassLoader</code> used for loading the classes of the framework.
   */
  public AbstractJPPFClassLoader getClassLoader() {
    return classLoaderManager.getClassLoader();
  }

  /**
   * Set the main classloader for the node.
   * @param cl the class loader to set.
   * @exclude
   */
  public void setClassLoader(final AbstractJPPFClassLoader cl) {
    classLoaderManager.setClassLoader(cl);
  }

  /**
   * Get a reference to the JPPF container associated with an application uuid.
   * @param uuidPath the uuid path containing the key to the container.
   * @return a <code>JPPFContainer</code> instance.
   * @throws Exception if an error occurs while getting the container.
   * @exclude
   */
  public JPPFContainer getContainer(final List<String> uuidPath) throws Exception {
    return classLoaderManager.getContainer(uuidPath);
  }

  /**
   * Clear the resource caches of all class loaders managed by this object.
   */
  void clearResourceCachesIfRequested() {
    if (cacheResetFlag.get()) {
      try {
        classLoaderManager.clearResourceCaches();
      } finally {
        cacheResetFlag.set(false);
      }
    }
  }

  /**
   * Request a reset of the class loaders resource caches.
   * This method merely sets a floag, the actual reset will
   * be performed at the next opportunity, when it is safe to do so.
   */
  public void requestResourceCacheReset() {
    cacheResetFlag.compareAndSet(false, true);
  }

  /**
   * Request that the node be shut down or restarted when it is no longer executing tasks.
   * @param restart {@code true} to restart the node, {@code false} to shut it down.
   * @return {@code true} if the node had no pending action and the request succeeded, {@code false} otherwise.
   * @since 5.0
   * @exclude
   */
  public boolean requestShutdown(final boolean restart) {
    if (shutdownRequestFlag.compareAndSet(false, true)) {
      this.restart.set(restart);
    }
    return shutdownRequestFlag.get();
  }

  /**
   * Cancel a previous deferred shutdown or restart request, if any.
   * @return {@code true} if the node has a pending action and it was cancelled, {@code false} otherwise.
   * @since 5.0
   * @exclude
   */
  public boolean cancelShutdownRequest() {
    return shutdownRequestFlag.compareAndSet(true, false);
  }

  /**
   * Determine whether a node shurdown or restart was requested..
   * @return {@code true} if a shudown or restart was requested, {@code false} otherwise.
   * @since 5.0
   * @exclude
   */
  public boolean isShutdownRequested() {
    return shutdownRequestFlag.get();
  }

  /**
   * Determine whether a restart or shutdown was requested.
   * @return {@code true} if a restart was requested, false if a {@code shutdown} was requested.
   * @since 5.0
   * @exclude
   */
  public boolean isRestart() {
    return restart.get();
  }

  /**
   * Determine whether the node is currently processing tasks.
   * @return {@code true} if the node is processing tasks, {@code false} otherwise.
   * @since 5.0
   * @exclude
   */
  public boolean isExecuting() {
    return executing;
  }

  /**
   * Specifiy whether the node is currently processing tasks.
   * @param executing {@code true} to specify that the node is processing tasks, {@code false} otherwise.
   * @since 5.0
   * @exclude
   */
  public void setExecuting(final boolean executing) {
    this.executing = executing;
  }

  /**
   * Determine whether the node is suspended, i.e. it is still alive but has stopped taking on new jobs.
   * @return {@code true} if the node is suspended, {@code false} otherwise.
   * @since 5.2
   * @exclude
   */
  public boolean isSuspended() {
    return suspended.get();
  }

  /**
   * Set the node's suspended state, i.e. whether it should sto taking on new jobs.
   * @param suspended {@code true} to suspend the node, {@code false} otherwise.
   * @since 5.2
   * @exclude
   */
  public void setSuspended(final boolean suspended) {
    this.suspended.set(suspended);
    if (!suspended) suspendedLock.wakeUp();
  }

  /**
   * Determine the node's reading state.
   * @return {@code true} set the node in reading mode, {@code false} otherwise.
   * @since 5.2
   * @exclude
   */
  public boolean isReading() {
    return reading.get();
  }

  /**
   * Set the node's reading state.
   * @param suspended {@code true} set the node in reading mode, {@code false} otherwise.
   * @since 5.2
   * @exclude
   */
  public void setReading(final boolean suspended) {
    this.reading.set(suspended);
  }

  /**
   * Get the service that manages the class loaders and how they are used.
   * @return an {@link AbstractClassLoaderManager} instance.
   * @exclude
   */
  public AbstractClassLoaderManager getClassLoaderManager() {
    return classLoaderManager;
  }
}
