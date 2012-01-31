/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.jppf.classloader.LocalClassLoaderChannel;
import org.jppf.server.nio.nodeserver.LocalNodeChannel;
import org.jppf.server.node.JPPFNode;

/**
 * Local (in-VM) node implementation.
 * @author Laurent Cohen
 */
public class JPPFLocalNode extends JPPFNode
{
  /**
   * The I/O handler for this node.
   */
  private LocalNodeChannel channel = null;
  /**
   * The I/O handler for the class loader.
   */
  private LocalClassLoaderChannel classLoaderHandler = null;

  /**
   * Initialize this local node with the specified I/O handler.
   * @param channel the I/O handler for this node.
   * @param classLoaderHandler the I/O handler for the class loader.
   */
  public JPPFLocalNode(final LocalNodeChannel channel, final LocalClassLoaderChannel classLoaderHandler)
  {
    this.channel = channel;
    this.classLoaderHandler = classLoaderHandler;
    classLoaderManager = new LocalClassLoaderManager(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initDataChannel() throws Exception
  {
    nodeIO = new LocalNodeIO(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void closeDataChannel() throws Exception
  {
  }

  /**
   * Get the I/O handler for this node.
   * @return a {@link LocalNodeChannel} instance.
   */
  LocalNodeChannel getChannel()
  {
    return channel;
  }

  /**
   * Get the I/O handler for the class loader.
   * @return a {@link LocalClassLoaderChannel} instance.
   */
  LocalClassLoaderChannel getClassLoaderHandler()
  {
    return classLoaderHandler;
  }
}
