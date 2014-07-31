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

package org.jppf.server.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.management.JMXServer;
import org.jppf.node.AbstractNode;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.protocol.BundleParameter;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This class is used as a container for common methods that cannot be implemented in {@link AbstractNode}.
 * @author Laurent Cohen
 */
public abstract class AbstractCommonNode extends AbstractNode {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Manages the class loaders and how they are used.
   * @exclude
   */
  protected AbstractClassLoaderManager classLoaderManager = null;
  /**
   * Flag which determines whether a reset of the resource caches
   * should be performed at the next opportunity.
   * @exclude
   */
  protected AtomicBoolean cacheResetFlag = new AtomicBoolean(false);

  /**
   * Add management parameters to the specified bundle, before sending it back to a server.
   * @param bundle the bundle to add parameters to.
   * @exclude
   */
  protected void setupManagementParameters(final TaskBundle bundle) {
    try {
      JMXServer jmxServer = getJmxServer();
      bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
      bundle.setParameter(BundleParameter.NODE_PROVISIONING_MASTER, isMasterNode());
      bundle.setParameter(BundleParameter.NODE_PROVISIONING_SLAVE, isSlaveNode());
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
   * @exclude
   */
  protected void clearResourceCachesIfRequested() {
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
}
