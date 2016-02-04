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
 * This interface should be implemented by tasks that wish a callback invoked as soon
 * as they timeout. This differs from the {@link org.jppf.node.protocol.Task#onTimeout() Task.onTimeout()}
 * callback in that {@code onTimeout()} is called after the task has ended.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public interface TimeoutHandler {
  /**
   * Invoked immediately when a task times out.
   * @throws Exception if any error occurs while this callback is executing.
   */
  void doTimeoutAction() throws Exception;
}
