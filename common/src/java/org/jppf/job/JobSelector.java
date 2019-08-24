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

package org.jppf.job;

import java.io.Serializable;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * Interface used to select or filter jobs in a client, driver or node.
 * @author Laurent Cohen
 * @since 5.1
 */
public interface JobSelector extends Serializable {
  /**
   * A predefined singleton for {@link AllJobsSelector}.
   */
  JobSelector ALL_JOBS = new AllJobsSelector();

  /**
   * Determine whether the specified job is accepted by this selector. 
   * @param job the job to check.
   * @return {@code true} if the job is acepted, {@code false} otherwise.
   */
  boolean accepts(JPPFDistributedJob job);

  /**
   * Negate this job selector.
   * @return a {@code JobSelector} which negates this selector.
   * @since 6.2
   */
  default JobSelector negate() {
    return job -> !this.accepts(job);
  }

  /**
   * Return a negation of the specified job selector.
   * This convenience method is equivalent to calling {@code selector.negate()}.
   * @param selector the selector to negate.
   * @return a job selector that realizes a logical "not" operation on this selector.
   * @since 6.2
   */
  public static JobSelector not(final JobSelector selector) {
    if (selector == null) throw new NullPointerException("selector cannot be null");
    return selector.negate();
  }

  /**
   * Obtain a job selector that realizes a logical "and" operation on this selector and an other.
   * @param other the other selector to combine with.
   * @return a {@code JobSelector} which combines this selector and the other with an "and" operator.
   * @throws NullPointerException if {@code other} is null.
   * @since 6.2
   */
  default JobSelector and(final JobSelector other) throws NullPointerException {
    if (other == null) throw new NullPointerException("operand cannot be null");
    return job -> this.accepts(job) && other.accepts(job);
  }

  /**
   * Obtain a job selector that realizes a logical "or" operation on this selector and an other.
   * @param other the other selector to combine with.
   * @return a {@code JobSelector} which combines this selector and the other with an "or" operator.
   * @throws NullPointerException if {@code other} is null.
   * @since 6.2
   */
  default JobSelector or(final JobSelector other) throws NullPointerException {
    if (other == null) throw new NullPointerException("operand cannot be null");
    return job -> this.accepts(job) || other.accepts(job);
  }

  /**
   * Obtain a job selector that realizes a logical "xor" operation on this selector and an other.
   * @param other the other selector to combine with.
   * @return a {@code JobSelector} which combines this selector and the other with an "xor" operator.
   * @throws NullPointerException if {@code other} is null.
   * @since 6.2
   */
  default JobSelector xor(final JobSelector other) throws NullPointerException {
    if (other == null) throw new NullPointerException("operand cannot be null");
    return job -> this.accepts(job) != other.accepts(job);
  }
}
