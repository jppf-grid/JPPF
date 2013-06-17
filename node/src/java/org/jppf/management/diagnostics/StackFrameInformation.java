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

package org.jppf.management.diagnostics;

import java.io.Serializable;

/**
 * nformation about a frame in a thread stack trace.
 * @author Laurent Cohen
 */
public class StackFrameInformation implements Serializable
{
  /**
   * The stack trace element.
   */
  private final StackTraceElement stackTraceElement;
  /**
   * The associated lock, if any.
   */
  private final LockInformation lock;

  /**
   * Create this object with the specified parameters.
   * @param stackTraceElement the stack trace element.
   * @param lock the associated lock, may be null if none exists.
   */
  public StackFrameInformation(final StackTraceElement stackTraceElement, final LockInformation lock)
  {
    super();
    this.stackTraceElement = stackTraceElement;
    this.lock = lock;
  }

  /**
   * Get the stack trace element.
   * @return a {@link StackTraceElement} instance.
   */
  public StackTraceElement getStackTraceElement()
  {
    return stackTraceElement;
  }

  /**
   * Get the associated lock.
   * @return a {@link LockInformation} instance, or <code>null</code> if no lock is associated with the frame.
   */
  public LockInformation getLock()
  {
    return lock;
  }

  @Override
  public String toString()
  {
    /*
    return new StringBuilder().append(getClass().getSimpleName()).append("[stackTraceElement=").append(stackTraceElement)
        .append(", lock=").append(lock).append(']').toString();
    */
    return new StringBuilder().append(stackTraceElement).toString();
  }
}
