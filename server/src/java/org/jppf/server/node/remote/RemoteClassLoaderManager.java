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

package org.jppf.server.node.remote;

import java.security.*;
import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.classloader.*;
import org.jppf.node.NodeRunner;
import org.jppf.server.node.*;
import org.slf4j.*;

/**
 * Concrete implementation of {@link AbstractClassLoaderManager} for a remote node.
 * @author Laurent Cohen
 */
public class RemoteClassLoaderManager extends AbstractClassLoaderManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RemoteClassLoaderManager.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node that holds this class loader manager.
   */
  private final JPPFRemoteNode node;

  /**
   * Initialize this class loader manager with the specified node.
   * @param node the node that holds this class loader manager.
   */
  RemoteClassLoaderManager(final JPPFNode node)
  {
    if (node == null) throw new IllegalArgumentException("node is null");
    this.node = (JPPFRemoteNode) node;
  }

  @Override
  protected AbstractJPPFClassLoader createClassLoader()
  {
    if (debugEnabled) log.debug("Initializing classloader");
    return NodeRunner.getJPPFClassLoader();
  }

  @Override
  protected JPPFContainer newJPPFContainer(final List<String> uuidPath, final AbstractJPPFClassLoader cl) throws Exception
  {
    return new JPPFRemoteContainer((RemoteNodeConnection) node.getNodeConnection(), uuidPath, cl);
  }

  @Override
  protected Callable<AbstractJPPFClassLoader> newClassLoaderCreator(final List<String> uuidPath)
  {
    return new Callable<AbstractJPPFClassLoader>()
    {
      @Override
      public AbstractJPPFClassLoader call()
      {
        PrivilegedAction<AbstractJPPFClassLoader> pa = new PrivilegedAction<AbstractJPPFClassLoader>()
        {
          @Override
          public AbstractJPPFClassLoader run()
          {
            AbstractJPPFClassLoader parent = getClassLoader();
            return new JPPFClassLoader(parent.getConnection(), parent, uuidPath);
          }
        };
        return AccessController.doPrivileged(pa);
      }
    };
  }
}
