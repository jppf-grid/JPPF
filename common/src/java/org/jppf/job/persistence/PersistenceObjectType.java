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

package org.jppf.job.persistence;

/**
 * An enumeration of the possible types of objects  or job elements that are persisted.
 * @author Laurent Cohen
 * @since 6.0
 */
public enum PersistenceObjectType {
  /**
   * The job header, which includes the job's uuid, name, SLA and metadata, but also the information required for job routing and scheduling.
   */
  JOB_HEADER,
  /**
   * The {@link org.jppf.node.protocol.DataProvider data provider}.
   */
  DATA_PROVIDER,
  /**
   * A non-executed task.
   */
  TASK,
  /**
   * An executed task returned by a node, also known as task execution result.
   */
  TASK_RESULT
}
