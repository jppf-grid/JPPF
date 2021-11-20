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

package org.jppf.server.node.remote;

import java.security.*;
import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.classloader.*;
import org.jppf.server.node.*;
import org.slf4j.*;

/**
 * Concrete implementation of {@link AbstractClassLoaderManager} for a remote node.
 * @author Laurent Cohen
 */
public class RemoteClassLoaderManager extends AbstractClassLoaderManager<JPPFRemoteNode> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RemoteClassLoaderManager.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this class loader manager with the specified node.
   * @param node the node that holds this class loader manager.
   */
  RemoteClassLoaderManager(final JPPFRemoteNode node) {
    super(node);
  }

  @Override
  protected AbstractJPPFClassLoader createClassLoader() {
    if (debugEnabled) log.debug("Initializing classloader");
    return node.getJPPFClassLoader();
  }

  @Override
  protected JPPFContainer newJPPFContainer(final List<String> uuidPath, final AbstractJPPFClassLoader cl, final boolean clientAccess) throws Exception {
    return new JPPFRemoteContainer(node, uuidPath, cl, clientAccess);
  }

  @Override
  protected Callable<AbstractJPPFClassLoader> newClassLoaderCreator(final List<String> uuidPath, final Object...params) {
    return () -> {
      final PrivilegedAction<AbstractJPPFClassLoader> pa = () -> {
        final AbstractJPPFClassLoader parent = getClassLoader();
        return new JPPFClassLoader(parent.getConnection(), parent, uuidPath, node.getHookFactory());
      };
      return AccessController.doPrivileged(pa);
    };
  }
}
