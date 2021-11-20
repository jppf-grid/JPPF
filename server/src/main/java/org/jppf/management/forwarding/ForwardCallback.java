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

package org.jppf.management.forwarding;

import org.jppf.utils.InvocationResult;

/**
 * A callback invoked by each submitted forwarding task to notify that results have arrived from a node.
 * @param <E> the type of result.
 */
interface ForwardCallback<E> {
  /**
   * Called when a result is received from a node.
   * @param uuid the uuid of the node.
   * @param result the result of exception returned by the JMX call.
   */
  void gotResult(final String uuid, final InvocationResult<E> result);
}
