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

package test.org.jppf.test.setup.common;

import java.util.*;

import org.jppf.management.*;
import org.jppf.node.Node;
import org.jppf.test.addons.mbeans.NodeTestMBean;

/**
 * This is a helper class to ease the use of {@link NodeTestMBean}.
 * @author Laurent Cohen
 */
public class TaskNotifier {
  /**
   * Mapping of node uuids to corresponding test mbean instance.
   */
  private static final Map<String, NodeTestMBean> mbeanMap = new HashMap<>();

  /**
   * Send a notification message to all registered listeners.
   * @param node the node where the mbean is registered.
   * @param message the message to send to all registered listeners.
   * @throws Exception if any error occurs.
   */
  public static void addNotification(final Node node, final Object message) throws Exception {
    final NodeTestMBean mbean = getMBeanFor(node);
    mbean.sendUserObject(message);
    System.out.println("sent object: " + message);
  }

  /**
   * Retrieve the test mbean for the specified node.
   * @param node the JPPF node for which to retrieve an MBean.
   * @return the retireved mbean.
   * @throws Exception if any error occurs.
   */
  private static NodeTestMBean getMBeanFor(final Node node) throws Exception {
    NodeTestMBean mbean = null;
    synchronized(mbeanMap) {
      mbean = mbeanMap.get(node.getUuid());
      if (mbean == null) {
        final JPPFManagementInfo info = node.getManagementInfo();
        final JMXNodeConnectionWrapper jmx = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), info.isSecure());
        if (!jmx.connectAndWait(5000L)) throw new IllegalStateException("could not conenct to node jmx " + jmx);
        mbean = jmx.getProxy(NodeTestMBean.MBEAN_NAME, NodeTestMBean.class);
        mbeanMap.put(node.getUuid(), mbean);
      }
    }
    return mbean;
  }
}
