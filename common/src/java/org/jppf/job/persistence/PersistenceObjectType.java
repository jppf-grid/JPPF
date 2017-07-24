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
 * An enumeration of the possible types of objects that are persisted.
 * @author Laurent Cohen
 * @since 6.0
 */
public enum PersistenceObjectType {
  /**
   * The job header.
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
   * The execution result of a task.
   */
  TASK_RESULT
}
