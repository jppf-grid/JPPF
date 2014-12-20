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

package org.jppf.node.protocol;

/**
 * This interface should be implemented by JPPF task classes that wish to control
 * whether they can be interrupted upon cancellation or timeout.
 * @author Laurent Cohen
 * @since 4.2.5
 */
public interface Interruptibility {
  /**
   * Determine whether the thread executing the task can be interrupted.
   * @return {@code true} if the task can be interrupted, {@code false} otherwise.
   */
  boolean isInterruptible();
}
