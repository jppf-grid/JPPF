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

package org.jppf.server.nio.classloader;

import org.jppf.classloader.ResourceProvider;
import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;

/**
 * Instances of this class serve class loading requests from the JPPF nodes.
 * @param <S> the types of the states of the channels.
 * @param <T> the supported channel state transitions.
 * @author Laurent Cohen
 */
public abstract class ClassNioServer<S extends Enum<S>, T extends Enum<T>> extends NioServer<S, T> {
  /**
   * Reads resource files from the classpath.
   */
  protected ResourceProvider resourceProvider = ResourceProvider.Factory.initResourceProvider();
  /**
   * Reference to the driver.
   */
  protected final JPPFDriver driver;

  /**
   * Initialize this class server.
   * @param identifier the channel identifier for channels handled by this server.
   * @param driver reference to the driver.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public ClassNioServer(final int identifier, final JPPFDriver driver, final boolean useSSL) throws Exception {
    super(identifier, useSSL);
    if (driver == null) throw new IllegalArgumentException("driver is null");

    this.driver = driver;
    selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
  }

  /**
   * Get the soft cache of classes downloaded form the clients r from this driver's classpath.
   * @return an instance of {@link ClassCache}.
   */
  public ClassCache getClassCache() {
    return driver.getInitializer().getClassCache();
  }

  /**
   * Get the resource provider for this server.
   * @return a ResourceProvider instance.
   */
  public ResourceProvider getResourceProvider() {
    return resourceProvider;
  }
}
