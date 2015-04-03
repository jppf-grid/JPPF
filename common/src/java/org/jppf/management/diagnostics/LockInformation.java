/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.management.diagnostics;

import java.io.Serializable;
import java.lang.management.LockInfo;

/**
 * Information about a lock found in a <code>ThreadInfo</code> object.
 * @author Laurent Cohen
 */
public class LockInformation implements Serializable
{
  /**
   * The fully qualified class name of this lock.
   */
  private final String className;
  /**
   * The identity hash code of this lock.
   */
  private final int identityHashcode;

  /**
   * Initialize this lock information object with the specified parameters.
   * @param className the fully qualified class name of this lock.
   * @param identityHashcode the identity hash code of this lock.
   */
  public LockInformation(final String className, final int identityHashcode)
  {
    this.className = className;
    this.identityHashcode = identityHashcode;
  }

  /**
   * Initialize this lock information from a {@link LockInfo}.
   * @param lockInfo the fully qualified class name of this lock.
   */
  public LockInformation(final LockInfo lockInfo)
  {
    this(lockInfo.getClassName(), lockInfo.getIdentityHashCode());
  }

  /**
   * Get the fully qualified class name of this lock.
   * @return the class name as a string.
   */
  public String getClassName()
  {
    return className;
  }

  /**
   * Get the identity hash code of this lock.
   * @return the hascode as an int value.
   */
  public int getIdentityHashcode()
  {
    return identityHashcode;
  }

  @Override
  public String toString()
  {
    return new StringBuilder().append(getClass().getSimpleName()).append("[className=").append(className)
      .append(", identityHashcode=").append(identityHashcode).append(']').toString();
  }
}
