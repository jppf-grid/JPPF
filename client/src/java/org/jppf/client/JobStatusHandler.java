/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.client;


/**
 * Interface implemented by classes that wish to handle job statuses.
 * @author Laurent Cohen
 * @exclude
 */
public interface JobStatusHandler {
  /**
   * Get the status of the job.
   * @return a {@link JobStatus} enumerated value.
   */
  JobStatus getStatus();

  /**
   * Set the status of the job.
   * @param newStatus a {@link JobStatus} enumerated value.
   */
  void setStatus(final JobStatus newStatus);
}
