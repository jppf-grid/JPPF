/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.execute.async;

import java.util.List;

import org.jppf.node.protocol.*;

/**
 * 
 * @author Laurent Cohen
 */
public interface ExecutionManagerListener {
  /**
   * Called when the execution of a task bundle has finished.
   * @param bundle the TaskBundle which holds information on the job.
   * @param tasks the tasks that were executed.
   * @param t a {@link Throwable} that prevented or interrupted the job processing.
   */
  void bundleExecuted(final TaskBundle bundle, final List<Task<?>> tasks, final Throwable t);
}
