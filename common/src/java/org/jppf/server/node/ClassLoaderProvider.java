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

package org.jppf.server.node;

import java.util.List;

/**
 * Interface for a class loader provider.
 * @author Martin JANDA
 */
public interface ClassLoaderProvider {
  /**
   * Get a reference to the class loader associated with an application uuid.
   * @param uuidPath the uuid path containing the key to the container.
   * @return a <code>ClassLoader</code> used for loading the classes of the framework.
   * @throws Exception if an error occurs while getting the class loader.
   */
  ClassLoader getClassLoader(final List<String> uuidPath) throws Exception;
}
