/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.management;

import javax.management.NotificationEmitter;


/**
 * MBean interface for task-level monitoring on each node.
 * @author Laurent Cohen
 */
public interface JPPFNodeTaskMonitorMBean extends NotificationEmitter
{
  /**
   * Name of the node's task monitor MBean.
   */
  String MBEAN_NAME = "org.jppf:name=task.monitor,type=node";
  /**
   * Get the total number of tasks executed by the node.
   * @return the number of tasks as an integer value.
   */
  Integer getTotalTasksExecuted();
  /**
   * The total number of tasks that ended in error.
   * @return the number as an integer value.
   */
  Integer getTotalTasksInError();
  /**
   * The total number of tasks that executed successfully.
   * @return the number as an integer value.
   */
  Integer getTotalTasksSucessfull();
  /**
   * The total cpu time used by the tasks in milliseconds.
   * @return the cpu time as long value.
   */
  Long getTotalTaskCpuTime();
  /**
   * The total elapsed time used by the tasks in milliseconds.
   * @return the elapsed time as long value.
   */
  Long getTotalTaskElapsedTime();
}
