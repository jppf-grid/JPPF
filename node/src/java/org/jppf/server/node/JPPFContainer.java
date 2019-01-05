/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.serialization.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class represent dynamic class loading, and serialization/deserialization, capabilities, associated
 * with a specific client application.<br>
 * The application is identified through a unique uuid. This class effectively acts as a container for the classes of
 * a client application, a provides the methods to enable the transport, serialization and deserialization of these classes.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class JPPFContainer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFContainer.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Utility for deserialization and serialization.
   */
  protected SerializationHelper helper;
  /**
   * Utility for deserialization and serialization.
   */
  protected ObjectSerializer serializer;
  /**
   * Class loader used for dynamic loading and updating of client classes.
   */
  protected AbstractJPPFClassLoader classLoader;
  /**
   * The unique identifier for the submitting application.
   */
  protected List<String> uuidPath = new ArrayList<>();
  /**
   * Used to prevent parallel deserialization.
   */
  private final Lock lock = new ReentrantLock();
  /**
   * Determines whether tasks deserialization should be sequential rather than parallel.
   */
  private final boolean sequentialDeserialization;
  /**
   * Whether the node has access to the client that submitted the job.
   */
  private final boolean clientAccess;

  /**
   * Initialize this container with a specified application uuid.
   * @param node the node holding this container.
   * @param uuidPath the unique identifier of a submitting application.
   * @param classLoader the class loader for this container.
   * @param clientAccess whether the node has access to the client that submitted the job.
   * @throws Exception if an error occurs while initializing.
   */
  public JPPFContainer(final AbstractCommonNode node, final List<String> uuidPath, final AbstractJPPFClassLoader classLoader, final boolean clientAccess) throws Exception {
    if (debugEnabled) log.debug("new JPPFContainer with uuidPath={}, classLoader={}, clientAccess={}", uuidPath, classLoader, clientAccess);
    this.uuidPath = uuidPath;
    this.classLoader = classLoader;
    this.clientAccess = clientAccess;
    this.sequentialDeserialization = node.getConfiguration().get(JPPFProperties.SEQUENTIAL_SERIALiZATION);
    init();
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  public final void init() throws Exception {
    initHelper();
  }

  /**
   * Deserialize a number of objects from the I/O channel.
   * @param list a list holding the resulting deserialized objects.
   * @param count the number of objects to deserialize.
   * @param executor the number of objects to deserialize.
   * @return the new position in the source data after deserialization.
   * @throws Throwable if an error occurs while deserializing.
   */
  public abstract int deserializeObjects(Object[] list, int count, ExecutorService executor) throws Throwable;

  /**
   * Get the main class loader for this container.
   * @return a <code>ClassLoader</code> used for loading the classes of the framework.
   */
  public AbstractJPPFClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * Get the main class loader for this container.
   * @param classLoader a <code>ClassLoader</code> used for loading the classes of the framework.
   */
  public void setClassLoader(final AbstractJPPFClassLoader classLoader) {
    this.classLoader = classLoader;
    try {
      initHelper();
    } catch (final Exception e) {
      log.error("error setting new class loader", e);
    }
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @throws Exception if an error occurs while instantiating the class loader.
   */
  protected void initHelper() throws Exception {
    AbstractJPPFClassLoader cl = getClassLoader();
    if (!clientAccess) cl = (AbstractJPPFClassLoader) cl.getParent();
    final String name = "org.jppf.utils.SerializationHelperImpl";
    if (debugEnabled) log.debug("loading class {} with classloader {}", name, cl);
    final Class<?> c = cl.loadJPPFClass(name);
    helper = (SerializationHelper) c.newInstance();
    serializer = helper.getSerializer();
  }

  /**
   * Get the unique identifier for the submitting application.
   * @return the application uuid as a string.
   */
  public String getAppUuid() {
    return uuidPath.isEmpty() ? null : uuidPath.get(0);
  }

  /**
   * Set the unique identifier for the submitting application.
   * @param uuidPath the application uuid as a string.
   */
  public void setUuidPath(final List<String> uuidPath) {
    this.uuidPath = uuidPath;
  }

  /**
   * Return the utility object for serialization and deserialization.
   * @return an {@link ObjectSerializer} instance.
   */
  public ObjectSerializer getSerializer() {
    return serializer;
  }

  /**
   * @return the lock for sequential deserialization.
   */
  public Lock getLock() {
    return lock;
  }

  /**
   * @return {@code true} if sequential serialization/serialization is enabled, {@code false} otherwise.
   */
  public boolean isSequentialDeserialization() {
    return sequentialDeserialization;
  }
}
