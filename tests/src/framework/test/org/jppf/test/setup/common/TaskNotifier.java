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

package test.org.jppf.test.setup.common;

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.test.addons.mbeans.NodeTestMBean;
import org.jppf.utils.ExceptionUtils;

/**
 * This is a test of a node startup class.
 * @author Laurent Cohen
 */
public class TaskNotifier {
  /**
   * The proxy to the mbean that sends the actual notifications.
   */
  private static NodeTestMBean mbean = null;
  static {
    try (JMXNodeConnectionWrapper jmxWrapper = new JMXNodeConnectionWrapper()) {
      jmxWrapper.connect();
      if (!jmxWrapper.isConnected()) {
        System.out.println("Error: could not connect to the local MBean server");
      } else mbean = jmxWrapper.getProxy(NodeTestMBean.MBEAN_NAME, NodeTestMBean.class);
    } catch (final Exception e) {
      System.out.println("Error: " + ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Send a notification message to all registered listeners.
   * @param message the message to send to all registered listeners.
   * @throws Exception if any error occurs.
   */
  public static void addNotification(final Object message) throws Exception {
    if (mbean == null) return;
    mbean.sendUserObject(message);
    System.out.println("sent object: " + message);
  }
}
