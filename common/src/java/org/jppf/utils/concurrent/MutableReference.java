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

package org.jppf.utils.concurrent;

/**
 * Holds a mutable object reference witho no locking or synchronization mechanism.
 * This can be used in anonymous classes for mutating values without the cost of synchronization such as implemented in {@link java.util.concurrent.atomic.AtomicReference}.
 * <p>For thread-safe access, instances of this class <i>must</i> be synchronized externally or by using the
 * {code getSynchronized(Object)} and {@code setSynchronized(E, Object)} methods.
 * @param <E> The type of reference to handle.
 * @author Laurent Cohen
 * @since 5.0
 */
public class MutableReference<E> {
  /**
   * The object held by this reference.
   */
  private E referend;

  /**
   * Initiaize this reference with the referend set to null.
   */
  public MutableReference() {
    this.referend = null;
  }

  /**
   * Initiaize this reference with the specified referend.
   * @param referend the object held by this reference.
   */
  public MutableReference(final E referend) {
    this.referend = referend;
  }

  /**
   * Get the object held by this reference.
   * @return an object of the reference type.
   */
  public E get() {
    return referend;
  }

  /**
   * Set the object held by this reference.
   * @param referend an object of the reference type.
   * @return the new referend.
   */
  public E set(final E referend) {
    return this.referend = referend;
  }

  /**
   * Get the referend within a block synchronized on the specified monitor.
   * @param monitor an object on which to synchronize.
   * @throws IllegalArgumentException if the monitor is null.
   * @return an object of the reference type.
   */
  public E getSynchronized(final Object monitor) throws IllegalArgumentException {
    if (monitor == null) throw new IllegalArgumentException("the monitor cannot be null");
    synchronized(monitor) {
      return referend;
    }
  }

  /**
   * Set the referend within a block synchronized on the specified monitor.
   * @param referend the value to set for this reference.
   * @param monitor an object on which to synchronize.
   * @return the new referend.
   * @throws IllegalArgumentException if the monitor is null.
   */
  public E setSynchronized(final E referend, final Object monitor) throws IllegalArgumentException {
    if (monitor == null) throw new IllegalArgumentException("the monitor cannot be null");
    synchronized(monitor) {
      return this.referend = referend;
    }
  }
}
