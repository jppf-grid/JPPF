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

import java.security.*;
import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.classloader.*;
import org.jppf.server.node.*;

/**
 * Concrete implementation of {@link AbstractClassLoaderManager} for a local node.
 * @author Laurent Cohen
 */
class LocalClassLoaderManager extends AbstractClassLoaderManager<JPPFLocalNode> {
  /**
   * Initialize this class loader manager with the specified I/O handler.
   * @param node the node that holds this class loader manager..
   */
  LocalClassLoaderManager(final JPPFLocalNode node) {
    super(node);
  }

  @Override
  protected AbstractJPPFClassLoader createClassLoader() {
    final PrivilegedAction<AbstractJPPFClassLoader> pa = new PrivilegedAction<AbstractJPPFClassLoader>() {
      @Override
      public AbstractJPPFClassLoader run() {
        final AbstractClassLoaderConnection<?> connection = node.getClassLoaderConnection();
        return new JPPFLocalClassLoader(connection, this.getClass().getClassLoader(), null, node.getHookFactory());
      }
    };
    return AccessController.doPrivileged(pa);
  }

  @Override
  protected JPPFContainer newJPPFContainer(final List<String> uuidPath, final AbstractJPPFClassLoader cl, final boolean clientAccess) throws Exception {
    return new JPPFLocalContainer(node, uuidPath, cl, clientAccess);
  }

  @Override
  protected Callable<AbstractJPPFClassLoader> newClassLoaderCreator(final List<String> uuidPath, final Object... params) {
    return new Callable<AbstractJPPFClassLoader>() {
      @Override
      public AbstractJPPFClassLoader call() {
        final PrivilegedAction<AbstractJPPFClassLoader> pa = new PrivilegedAction<AbstractJPPFClassLoader>() {
          @Override
          public AbstractJPPFClassLoader run() {
            final AbstractJPPFClassLoader parent = getClassLoader();
            return new JPPFLocalClassLoader(parent.getConnection(), parent, uuidPath, node.getHookFactory());
          }
        };
        return AccessController.doPrivileged(pa);
      }
    };
  }
}
