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

package org.jppf.node.protocol;

import java.util.*;

import org.jppf.location.Location;

/**
 * A simple implementation of the {@link ClassPath} interface
 * @author Laurent Cohen
 */
public class ClassPathImpl implements ClassPath
{
  /**
   * Mapping of classpath elements to their names.
   */
  private final Map<String, ClassPathElement> elementMap = new HashMap<>();
  /**
   * Determines whether the node should force a reset of the class loader before executing the tasks.
   */
  private boolean forceClassLoaderReset = false;

  @Override
  public Iterator<ClassPathElement> iterator()
  {
    return elementMap.values().iterator();
  }

  @Override
  public ClassPath add(final ClassPathElement element)
  {
    elementMap.put(element.getName(), element);
    return this;
  }

  @Override
  public ClassPath add(final String name, final Location<?> location)
  {
    elementMap.put(name, new ClassPathElementImpl(name, location));
    return this;
  }

  @Override
  public ClassPath add(final String name, final Location<?> localLocation, final Location<?> remoteLocation)
  {
    elementMap.put(name, new ClassPathElementImpl(name, localLocation, remoteLocation));
    return this;
  }

  @Override
  public ClassPath remove(final ClassPathElement element)
  {
    elementMap.remove(element.getName());
    return this;
  }

  @Override
  public ClassPath remove(final String name)
  {
    elementMap.remove(name);
    return null;
  }

  @Override
  public ClassPath clear()
  {
    elementMap.clear();
    return this;
  }

  @Override
  public ClassPathElement element(final String name)
  {
    return elementMap.get(name);
  }

  @Override
  public Collection<ClassPathElement> allElements()
  {
    return new ArrayList<>(elementMap.values());
  }

  @Override
  public boolean isEmpty()
  {
    return elementMap.isEmpty();
  }

  @Override
  public boolean isForceClassLoaderReset()
  {
    return forceClassLoaderReset;
  }

  @Override
  public void setForceClassLoaderReset(final boolean forceClassLoaderReset)
  {
    this.forceClassLoaderReset = forceClassLoaderReset;
  }
}
