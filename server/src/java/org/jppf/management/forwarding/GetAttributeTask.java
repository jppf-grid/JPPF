/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * This task gets an MBean attribute value from a remote node.
 */
class GetAttributeTask extends AbstractForwardingTask {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(GetAttributeTask.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this task.
   * @param context represents the node to which a request is sent.
   * @param mbeanName the name of the node MBean to which the request is sent.
   * @param attribute the name of the attribute to get.
   */
  protected GetAttributeTask(final AbstractNodeContext context, final String mbeanName, final String attribute) {
    super(context, mbeanName, attribute);
  }

  @Override
  protected Pair<String, Object> execute() throws Exception {
    String uuid = context.getUuid();
    JMXNodeConnectionWrapper wrapper = context.getJmxConnection();
    Object o = wrapper.getAttribute(mbeanName, memberName);
    if (debugEnabled) {
      log.debug(String.format("get attribute '%s' = %s on node %s", memberName, o, uuid));
    }
    return new Pair<>(uuid, o);
  }
}