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
}
