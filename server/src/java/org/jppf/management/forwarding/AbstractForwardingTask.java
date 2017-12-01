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

import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
   * The results map.
   */
  final Map<String, Object> resultMap;
  /**
   * 
   */
  final CountDownLatch latch;

  /**
   * Initialize this task.
   * @param latch .
   * @param context represents the node to which a request is sent.
   * @param resultMap the results map.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param memberName the name of the method to invoke, or the attribute to get or set.
   */
  AbstractForwardingTask(final CountDownLatch latch, final AbstractNodeContext context, final Map<String, Object> resultMap, final String mbeanName, final String memberName) {
    this.latch = latch;
    this.context = context;
    this.resultMap = resultMap;
    this.mbeanName = mbeanName;
    this.memberName = memberName;
  }


  @Override
  public void run() {
    String uuid = context.getUuid();
    Object result = null;
    try {
      result = execute();
    } catch (Exception e) {
      result = e;
    }
    synchronized(resultMap) {
      resultMap.put(uuid, result);
    }
    latch.countDown();
  }

  /**
   * Perform a JMX operation.
   * @return the result of the JMX invocation.
   * @throws Exception if any error occurs.
   */
  abstract Object execute() throws Exception;
}
