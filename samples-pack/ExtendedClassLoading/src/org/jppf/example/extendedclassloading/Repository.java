/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.example.extendedclassloading;

import java.net.URL;

import org.jppf.classloader.AbstractJPPFClassLoader;

/**
 * A repository manages a store of Java libraries on the node. These libraries are downloaded
 * on demand from a JPPF client and can be added dynamically to the classpath of a client class loader. 
 * @author Laurent Cohen
 */
public interface Repository {
  /**
   * Download the elements of the specified classpath, via the specified class loader, and save them to this repository's persistent store.
   * This method only downloads the elements that are not already present in the repository.
   * @param classpath contains the list of classpath elements to download.
   * @param cl the class loader to use to download the classpath elements.
   * @return an array of URLs corresponding to the elements in the classpath.
   */
  URL[] download(ClassPath classpath, AbstractJPPFClassLoader cl);

  /**
   * Delete the elements specified by a filter from this repository's persistent store.
   * @param filter the filter which provides the repository elements to delete.
   */
  void delete(RepositoryFilter filter);

  /**
   * Perform cleanup operations on this repository.
   */
  void cleanup();
}
