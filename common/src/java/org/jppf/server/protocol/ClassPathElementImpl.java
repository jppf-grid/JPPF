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

package org.jppf.server.protocol;

import org.jppf.node.protocol.*;

/**
 * A simple implementation of the {@link ClassPathElement} interface.
 * @author Laurent Cohen
 */
public class ClassPathElementImpl implements ClassPathElement
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The location of this classpath element in the client environment.
   */
  protected final Location<?> localLocation;
  /**
   * The location of this classpath element in the node environment.
   */
  protected final Location<?> remoteLocation;
  /**
   * The name of this classpath element.
   */
  protected final String name;

  /**
   * Initialize this classpath element with the specified name and local location.
   * The remote location is set to the same location.
   * @param name the name of this classpath element.
   * @param location the location of this classpath element.
   */
  protected ClassPathElementImpl(final String name, final Location<?> location)
  {
    this.name = name;
    this.localLocation = location;
    this.remoteLocation = this.localLocation;
  }

  /**
   * Initialize this classpath element with the specified name and local location.
   * The remote location is set to the same location.
   * @param name the name of this classpath element.
   * @param localLocation the location of this classpath element in the client environment.
   * @param remoteLocation the location of this classpath element in the node environment.
   */
  protected ClassPathElementImpl(final String name, final Location<?> localLocation, final Location<?> remoteLocation)
  {
    this.name = name;
    this.localLocation = localLocation;
    this.remoteLocation = remoteLocation;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public Location<?> getLocalLocation()
  {
    return localLocation;
  }

  @Override
  public Location<?> getRemoteLocation()
  {
    return remoteLocation;
  }

  /**
   * This default implementation always return true.
   * @return <code>true</code>.
   */
  public boolean validate()
  {
    return true;
  }
}
