/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.node.event;

/**
 * A type safe enumeration of all possible types of node life cycle events.
 * @author Laurent Cohen
 */
public enum NodeLifeCycleEventType
{
  /**
   * The node is starting and before any job processing.
   */
  NODE_STARTING,
  /**
   * The node is disconnected from the server.
   */
  NODE_ENDING,
  /**
   * The job header was loaded from the server and before the tasks are loaded.
   */
  JOB_HEADER_LOADED,
  /**
   * Before the processing of the job.
   */
  JOB_STARTING,
  /**
   * After a job processing is complete.
   */
  JOB_ENDING
}
