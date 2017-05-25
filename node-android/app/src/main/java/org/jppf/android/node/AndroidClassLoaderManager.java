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
package org.jppf.android.node;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.android.classloader.AndroidClassLoader;
import org.jppf.node.NodeRunner;
import org.jppf.node.protocol.ClassPath;
import org.jppf.server.node.AbstractClassLoaderManager;
import org.jppf.server.node.JPPFContainer;
import org.jppf.server.node.remote.JPPFRemoteContainer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * An android-sepcific {@link AbstractClassLoaderManager} implementation.
 * @author Laurent Cohen
 */
public class AndroidClassLoaderManager extends AbstractClassLoaderManager {
  @Override
  protected JPPFContainer newJPPFContainer(final List<String> uuidPath, final AbstractJPPFClassLoader cl, final boolean clientAccess) throws Exception {
    return new JPPFRemoteContainer(null, uuidPath, cl, false);
  }

  @Override
  protected AbstractJPPFClassLoader createClassLoader() {
    return NodeRunner.getJPPFClassLoader();
  }

  @Override
  protected Callable<AbstractJPPFClassLoader> newClassLoaderCreator(final List<String> uuidPath, final Object... params) {
    return new Callable<AbstractJPPFClassLoader>() {
      @Override
      public AbstractJPPFClassLoader call() {
        final ClassPath classpath = (params != null) && (params.length > 0) ? (ClassPath) params[0] : null;
        PrivilegedAction<AbstractJPPFClassLoader> pa = new PrivilegedAction<AbstractJPPFClassLoader>() {
          @Override
          public AbstractJPPFClassLoader run() {
            AbstractJPPFClassLoader parent = getClassLoader();
            return new AndroidClassLoader(parent, classpath);
          }
        };
        return AccessController.doPrivileged(pa);
      }
    };
  }
}
