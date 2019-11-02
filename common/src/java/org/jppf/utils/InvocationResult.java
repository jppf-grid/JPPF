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

package org.jppf.utils;

import java.io.Serializable;

/**
 * Instances of this class hold a reference to an object than can be either of the specified parametrized type or an exception.
 * They can be seen as a way to carry the result of a method invocation, even if this invocation raises an exception.
 * @param <R> the non-exception type of the referenced object.
 * @author Laurent Cohen
 */
public class InvocationResult<R> implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The referenced object.
   */
  private final Object ref;
  
  /**
   * Initialize with the specified reference object.
   * @param ref the referenced object with the expected type.
   */
  public InvocationResult(final R ref) {
    this.ref = ref;
  }
  
  /**
   * Initialize with the specified reference object.
   * @param ref the referenced object as an exception type.
   */
  public InvocationResult(final Exception ref) {
    this.ref = ref;
  }

  /**
   * Determine whether the referenced object is an exception.
   * @return {@code true} if the referenced object is an exception, {@code false} otherwise.
   */
  public boolean isException() {
    return ref instanceof Exception;
  }

  /**
   * Get the referenced object as an exception.
   * @return the referenced object as an {@link Exception}, or {@code null} if it is not an exception.
   */
  public Exception exception() {
    return isException() ? (Exception) ref : null;
  }

  /**
   * Get the invocation result.
   * @return the result with the desired parametrized type, or {@code null} if the result is an exception.
   */
  @SuppressWarnings("unchecked")
  public R result() {
    return isException() ? null : (R) ref;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("ref=").append(ref)
      .append(']').toString();
  }

  @Override
  public int hashCode() {
    return 31 + ((ref == null) ? 0 : ref.hashCode());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final InvocationResult<?> other = (InvocationResult<?>) obj;
    if (ref == null) return other.ref == null;
    return ref.equals(other.ref);
  }
}
