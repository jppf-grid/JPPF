/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.node;

import java.util.List;

import org.jppf.node.protocol.*;
import org.jppf.utils.Pair;

/**
 * This interface defines how a node receives a job and sends its execution results.
 * @author Laurent Cohen
 * @exclude
 */
public interface NodeIO
{
  /**
   * Read a task from the socket connection, along with its header information.
   * @return a pair of <code>JPPFTaskBundle</code> and a <code>List</code> of <code>JPPFTask</code> instances.
   * @throws Exception if an error is raised while reading the task data.
   */
  Pair<TaskBundle, List<Task<?>>> readTask() throws Exception;

  /**
   * Write the execution results to the socket stream.
   * @param bundle the task wrapper to send along.
   * @param tasks the list of tasks with their result field updated.
   * @throws Exception if an error occurs while writing to the socket stream.
   */
  void writeResults(TaskBundle bundle, List<Task<?>> tasks) throws Exception;

}
