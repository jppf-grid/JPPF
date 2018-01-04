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

package org.jppf.client.taskwrapper;

/**
 * Common abstract superclass for non-JPPF tasks wrappers.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractTaskObjectWrapper implements TaskObjectWrapper {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The type of the method to execute on the object.
   */
  protected MethodType methodType = MethodType.INSTANCE;

  /**
   * The type of the method to execute on the object.
   * @return a <code>MethodType</code> enum value.
   * @see org.jppf.client.taskwrapper.TaskObjectWrapper#getMethodType()
   */
  @Override
  public MethodType getMethodType() {
    return null;
  }

  /**
   * Load the class witht ht epsecified name.
   * @param classname name of the class ot laod.
   * @return a {@code Class} object if the class is found.
   * @throws Exception if any error occurs.
   */
  Class<?> getTaskobjectClass(final String classname) throws Exception {
    final ClassLoader[] loaders = { Thread.currentThread().getContextClassLoader(), getClass().getClassLoader() };
    for (ClassLoader cl : loaders) {
      try {
        return Class.forName(classname, true, cl);
      } catch (@SuppressWarnings("unused") final ClassNotFoundException e) {
      }
    }
    throw new ClassNotFoundException(classname);
  }
}
