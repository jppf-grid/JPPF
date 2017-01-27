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

import java.util.concurrent.Callable;

import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.utils.Pair;

/**
 * Common super class for all forwrding tasks.
 */
abstract class AbstractForwardingTask implements Callable<Pair<String, Object>> {
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
   * Initialize this task.
   * @param context represents the node to which a request is sent.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param memberName the name of the method to invoke, or the attribute to get or set.
   */
  AbstractForwardingTask(final AbstractNodeContext context, final String mbeanName, final String memberName) {
    this.context = context;
    this.mbeanName = mbeanName;
    this.memberName = memberName;
  }

  @Override
  public Pair<String, Object> call() {
    try {
      return execute();
    } catch (Exception e) {
     return new Pair<String, Object>(context.getUuid(), e);
    }
  }

  /**
   * Executes the request.
   * @return a pair made of the node uuid and either the request result or an exception that was raised.
   * @throws Exception if any error occurs.
   */
  protected abstract Pair<String, Object> execute() throws Exception;
}
