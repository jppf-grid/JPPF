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

import java.security.*;
import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.classloader.*;
import org.jppf.server.node.*;

/**
 * Concrete implementation of {@link AbstractClassLoaderManager} for a local node.
 * @author Laurent Cohen
 */
class LocalClassLoaderManager extends AbstractClassLoaderManager
{
  /**
   * The node that holds this class loader manager.
   */
  private JPPFLocalNode node = null;

  /**
   * Initialize this class loader manager with the specified I/O handler.
   * @param node the node that holds this class loader manager..
   */
  LocalClassLoaderManager(final JPPFLocalNode node)
  {
    this.node = node;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected AbstractJPPFClassLoader createClassLoader()
  {
    if (classLoader == null)
    {
      PrivilegedAction<AbstractJPPFClassLoader> pa = new PrivilegedAction<AbstractJPPFClassLoader>()
      {
        @Override
        public AbstractJPPFClassLoader run()
        {
          return new JPPFLocalClassLoader(node.getClassLoaderHandler(), this.getClass().getClassLoader());
        }
      };
      classLoader = AccessController.doPrivileged(pa);
    }
    return classLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected JPPFContainer newJPPFContainer(final List<String> uuidPath, final AbstractJPPFClassLoader cl) throws Exception
  {
    return new JPPFLocalContainer(node.getChannel(), uuidPath, cl);
  }

  /**
   * {@inheritDoc}
   */
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
            return new JPPFLocalClassLoader(getClassLoader(), uuidPath);
          }
        };
        return AccessController.doPrivileged(pa);
      }
    };
  }
}
