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

package org.jppf.server.node.local;

import org.jppf.classloader.*;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.jppf.utils.hooks.HookFactory;

/**
 * Local (in-VM) node implementation.
 * @author Laurent Cohen
 */
public class JPPFLocalNode extends JPPFNode {
  /**
   * Wraps the connection to the driver's class server.
   */
  private AbstractClassLoaderConnection<?> classLoaderConnection;

  /**
   * Initialize this local node with the specified I/O handler.
   * @param configuration the configuration of this node.
   * @param nodeConnection wraps the connection to the driver's job server.
   * @param classLoaderConnection wraps the connection to the driver's class server.
   * @param hookFactory used to create and invoke hook instances.
   */
  public JPPFLocalNode(final TypedProperties configuration, final AsyncLocalNodeConnection nodeConnection, final AbstractClassLoaderConnection<?> classLoaderConnection, final HookFactory hookFactory) {
    super(uuidFromConfig(configuration), configuration, hookFactory);
    this.nodeConnection = nodeConnection;
    this.classLoaderConnection = classLoaderConnection;
    classLoaderManager = new LocalClassLoaderManager(this);
  }

  @Override
  public void initDataChannel() throws Exception {
    nodeIO = new AsyncLocalNodeIO(this);
  }

  @Override
  public void closeDataChannel() throws Exception {
  }

  /**
   * Get the connection to the driver's class server.
   * @return a {@link LocalClassLoaderConnection} instance.
   */
  AbstractClassLoaderConnection<?> getClassLoaderConnection() {
    return classLoaderConnection;
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  /**
   * Get the nodes uuid from its configuration, create it if needed.
   * @param configuration the configuration to search in.
   * @return the node uuid.
   */
  private static String uuidFromConfig(final TypedProperties configuration) {
    return configuration.getString("jppf.node.uuid", JPPFUuid.normalUUID());
  }
}
