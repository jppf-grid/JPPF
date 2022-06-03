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

package org.jppf.management.forwarding.generated;

import javax.management.Notification;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.NodeConfigNotifierMBean;
import org.jppf.management.forwarding.AbstractMBeanForwarder;
import org.jppf.utils.TypedProperties;

/**
 * Forwarding proxy for the {@link NodeConfigNotifierMBean} MBean.
 * MBean description: interface listneing for specific configuration changes in a node.
 * <p>This Mbean emits notification of type {@link Notification}:
 * <br>- notifies of changes to the number of processing threads of a node.
 * <br>- user data: the configuration properties that changed.
 * <br>- user data type: {@link TypedProperties}.
 * @since 6.2
 */
public class NodeConfigNotifierMBeanForwarder extends AbstractMBeanForwarder {
  /**
   * Initialize this proxy.
   * @param jmx a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs..
   */
  public NodeConfigNotifierMBeanForwarder(final JMXDriverConnectionWrapper jmx) throws Exception {
    super(jmx, "org.jppf:name=config.notifier,type=node");
  }
}
