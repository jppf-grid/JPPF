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
package org.jppf.classloader;

/**
 * Instances of this class are dedicated to reading resource files form the JVM's classpath and converting them into arrays of bytes.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @exclude
 */
public class ResourceProviderImpl extends  AbstractResourceProvider {
  /**
   * Default constructor.
   */
  public ResourceProviderImpl() {
  }

  @Override
  protected ClassLoader resolveClassLoader(final ClassLoader classloader) {
    ClassLoader cl = classloader;
    if (cl == null) cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = getClass().getClassLoader();
    return cl;
  }
}
