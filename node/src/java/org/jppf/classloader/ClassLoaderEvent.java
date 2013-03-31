/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.EventObject;

/**
 * Event emitted by an {@link AbstractJPPFClassLoader} when a class is loaded or not found for the first time.
 * @author Laurent Cohen
 */
public class ClassLoaderEvent extends EventObject
{
  /**
   * The class that was successfully loaded.
   */
  private final Class<?> loadedClass;
  /**
   * The name of a class that could not be found by this class loader.
   */
  private final String className;
  /**
   * Determines whether the class was loaded from the class loader's URL classpath.
   */
  private final boolean foundInURLClassPath;

  /**
   * Initialize this event with the specified source and Class object.n
   * @param classLoader the source of this event.
   * @param loadedClass the class that was successfully loaded.
   * @param foundInURLClassPath <code>true</code> if the class was loaded from the class loader's URL classpath,
   * <code>false</code> if it was loaded from a remote JPPF driver or client.
   */
  public ClassLoaderEvent(final AbstractJPPFClassLoader classLoader, final Class<?> loadedClass, final boolean foundInURLClassPath)
  {
    super(classLoader);
    this.loadedClass = loadedClass;
    this.className = loadedClass.getName();
    this.foundInURLClassPath = foundInURLClassPath;
  }

  /**
   * Initialize this event with the specified source and Class object.n
   * @param classLoader the source of this event.
   * @param className the name of a class that could not be found by this class loader.
   */
  public ClassLoaderEvent(final AbstractJPPFClassLoader classLoader, final String className)
  {
    super(classLoader);
    this.loadedClass = null;
    this.className = className;
    this.foundInURLClassPath = false;
  }

  /**
   * Get the class that was successfully loaded.
   * @return  a <code>Class</code> object, or null if the class was not found.
   */
  public Class<?> getLoadedClass()
  {
    return loadedClass;
  }

  /**
   * Get the name of a class that was loaded or could not be found by this class loader.
   * @return the class name as a string.
   */
  public String getClassName()
  {
    return className;
  }

  /**
   * Determine whether the class was loaded from the class loader's URL classpath.
   * @return <code>true</code> if the class was loaded from the class loader's URL classpath,
   * <code>false</code> if it was loaded from a remote JPPF driver or client.
   */
  public boolean isFoundInURLClassPath()
  {
    return foundInURLClassPath;
  }

  /**
   * Get the class laoder which emitted this event.
   * @return a {@link AbstractJPPFClassLoader} instance.
   */
  public AbstractJPPFClassLoader getClassLoader()
  {
    return (AbstractJPPFClassLoader) getSource();
  }
}
