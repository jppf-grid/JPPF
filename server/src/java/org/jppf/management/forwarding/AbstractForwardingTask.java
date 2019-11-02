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
 * Common super class for all forwarding tasks.
 * @param <E> the type of result.
 */
abstract class AbstractForwardingTask<E> implements Runnable {
  /**
   * Represents the node to which a request is sent.
   */
  final String uuid;
  /**
   * The result.
   */
  InvocationResult<E> result;
  /**
   * 
   */
  final ForwardCallback<E> callback;

  /**
   * Initialize this task.
   * @param uuid represents the node to which a request is sent.
   * @param callback .
   */
  AbstractForwardingTask(final String uuid, final ForwardCallback<E> callback) {
    this.uuid = uuid;
    this.callback = callback;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run() {
    try {
      result = new InvocationResult<>(execute());
    } catch (final Exception e) {
      result = new InvocationResult<>(e);
    } finally {
      callback.gotResult(uuid, result);
    }
  }

  /**
   * Perform a JMX operation.
   * @return the result of the JMX invocation.
   * @throws Exception if any error occurs.
   */
  abstract E execute() throws Exception;
}
