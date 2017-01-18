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
package org.jppf.node.protocol;

/**
 * Common interface for client-side jobs (see {@link org.jppf.client.JPPFJob JPPFJob} and server-side jobs
 * (see {@link org.jppf.node.protocol.TaskBundle TaskBundle}).
 * @author Laurent Cohen
 */
public interface JPPFDistributedJob {
  /**
   * Get the user-defined display name for this job. This is the name displayed in the administration console.
   * @return the name as a string.
   */
  String getName();

  /**
   * Get the universal unique id for this job.
   * @return the uuid as a string.
   */
  String getUuid();

  /**
   * Get the service level agreement between the job and the server.
   * @return an instance of {@link JobSLA}.
   */
  JobSLA getSLA();

  /**
   * Get the user-defined metadata associated with this job.
   * @return a {@link JobMetadata} instance.
   */
  JobMetadata getMetadata();

  /**
   * Get the number of tasks in the job.
   * @return the number fo tasks.
   */
  int getTaskCount();
}
