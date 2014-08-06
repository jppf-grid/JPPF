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

package org.jppf.server.node;

import org.jppf.node.protocol.JPPFExceptionResult;

/**
 * This interface represents a hook invoked when the serialization of a task raises an exception.
 * @author Laurent Cohen
 */
public interface SerializationExceptionHook
{
  /**
   * Build the {@link JPPFExceptionResult} task that will be returned instead of the user task.
   * @param task the user task which can't be serialized.
   * @param t the throwable raised whil eattempting to serialize the user task.
   * @return an instance of {@link JPPFExceptionResult}.
   */
  JPPFExceptionResult buildExceptionResult(Object task, Throwable t);
}
