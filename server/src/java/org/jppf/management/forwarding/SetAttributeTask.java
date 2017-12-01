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
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This task sets an MBean attribute value on a remote node.
 */
class SetAttributeTask extends AbstractForwardingTask {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SetAttributeTask.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The method parameter values.
   */
  private final Object value;

  /**
   * Initialize this task.
   * @param latch .
   * @param context represents the node to which a request is sent.
   * @param resultMap the results map.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param attribute the name of the attribute to set.
   * @param value the value to set on the attribute.
   */
  SetAttributeTask(final CountDownLatch latch, final AbstractNodeContext context, final Map<String, Object> resultMap, final String mbeanName, final String attribute, final Object value) {
    super(latch, context, resultMap, mbeanName, attribute);
    this.value = value;
  }

  @Override
  protected Pair<String, Object> execute() throws Exception {
    context.getJmxConnection().setAttribute(mbeanName, memberName, value);
    if (debugEnabled) log.debug("set attribute '{}' on node {}", memberName, context.getUuid());
    return null;
  }
}
