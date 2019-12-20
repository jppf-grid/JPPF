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

package org.jppf.node.protocol;

import java.util.List;

import org.jppf.utils.Pair;

/**
 * A convenient class to associate a {@link TaskBundle} and a list of {@link Task}s.
 * @author Laurent Cohen
 * @exclude
 */
public class BundleWithTasks extends Pair<TaskBundle, List<Task<?>>> {
  /**
   * Construct.
   * @param bundle the task bundle.
   * @param Tasks the tasks.
   */
  public BundleWithTasks(final TaskBundle bundle, final List<Task<?>> Tasks) {
    super(bundle, Tasks);
  }

  /**
   * @return the task bundle.
   */
  public TaskBundle getBundle() {
    return first();
  }

  /**
   * @return the tasks.
   */
  public List<Task<?>> getTasks() {
    return second();
  }
}
