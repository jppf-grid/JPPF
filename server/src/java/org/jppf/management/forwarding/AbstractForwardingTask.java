/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.management.forwarding.JPPFNodeForwarding.ForwardCallback;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;

/**
 * Common super class for all forwrding tasks.
 */
abstract class AbstractForwardingTask implements Runnable {
  /**
   * Represents the node to which a request is sent.
   */
  final AbstractNodeContext context;
  /**
   * The name of the node MBean to which the request is sent.
   */
  final String mbeanName;
  /**
   * The name of the method to invoke, or the attribute to get or set.
   */
  final String memberName;
  /**
   * The result.
   */
  Object result;
  /**
   * 
   */
  final ForwardCallback callback;

  /**
   * Initialize this task.
   * @param context represents the node to which a request is sent.
   * @param callback .
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param memberName the name of the method to invoke, or the attribute to get or set.
   */
  AbstractForwardingTask(final AbstractNodeContext context, final ForwardCallback callback, final String mbeanName, final String memberName) {
    this.context = context;
    this.callback = callback;
    this.mbeanName = mbeanName;
    this.memberName = memberName;
  }

  @Override
  public void run() {
    try {
      result = execute();
    } catch (final Exception e) {
      result = e;
    } finally {
      callback.gotResult(context.getUuid(), result);
    }
  }

  /**
   * Perform a JMX operation.
   * @return the result of the JMX invocation.
   * @throws Exception if any error occurs.
   */
  abstract Object execute() throws Exception;
}
