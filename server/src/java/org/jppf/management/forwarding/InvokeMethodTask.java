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

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This task invokes an MBean method on a remote node.
 */
class InvokeMethodTask extends AbstractForwardingTask {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(InvokeMethodTask.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The method parameter values.
   */
  private final Object[] params;
  /**
   * The types of the method parameters.
   */
  private final String[] signature;

  /**
   * Initialize this task.
   * @param context represents the node to which a request is sent.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param methodName the name of the method to invoke, or the attribute to get or set.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   */
  protected InvokeMethodTask(final AbstractNodeContext context, final String mbeanName, final String methodName, final Object[] params, final String[] signature) {
    super(context, mbeanName, methodName);
    this.params = params;
    this.signature = signature;
  }

  @Override
  protected Pair<String, Object> execute() throws Exception {
    String uuid = context.getUuid();
    JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
    if (debugEnabled) log.debug(String.format("invoking %s() on mbean=%s for node=%s with jmx=%s", memberName, mbeanName, uuid, wrapper));
    Object o = wrapper.invoke(mbeanName, memberName, params, signature);
    return new Pair<>(uuid, o);
  }
}
