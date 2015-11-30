/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.classloader.LocalClassLoaderConnection;
import org.jppf.server.node.*;

/**
 * Local (in-VM) node implementation.
 * @author Laurent Cohen
 */
public class JPPFLocalNode extends JPPFNode
{
  /**
   * Wraps the connection to the driver's class server.
   */
  private LocalClassLoaderConnection classLoaderConnection = null;

  /**
   * Initialize this local node with the specified I/O handler.
   * @param nodeConnection wraps the connection to the driver's job server.
   * @param classLoaderConnection wraps the connection to the driver's class server.
   */
  public JPPFLocalNode(final LocalNodeConnection nodeConnection, final LocalClassLoaderConnection classLoaderConnection)
  {
    this.nodeConnection = nodeConnection;
    this.classLoaderConnection = classLoaderConnection;
    classLoaderManager = new LocalClassLoaderManager(this);
  }

  @Override
  public void initDataChannel() throws Exception
  {
    nodeIO = new LocalNodeIO(this);
  }

  @Override
  public void closeDataChannel() throws Exception
  {
  }

  /**
   * Get the connection to the driver's class server.
   * @return a {@link LocalClassLoaderConnection} instance.
   */
  LocalClassLoaderConnection getClassLoaderConnection()
  {
    return classLoaderConnection;
  }

  @Override
  protected NodeConnectionChecker createConnectionChecker()
  {
    return new LocalNodeConnectionChecker();
  }

  @Override
  public boolean isLocal()
  {
    return true;
  }
}
