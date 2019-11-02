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

import javax.management.ListenerNotFoundException;

import org.jppf.management.NodeSelector;
import org.jppf.management.doc.*;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
@MBeanExclude
public interface ForwardingNotficationEmitter {
  /**
   * Register a listener with the specified node selector and MBean.
   * @param selector the node slector to apply to the listener.
   * @param mBeanName the name of the node mbeans to receive notifications from.
   * @return a unique id for the listener.
   * @throws IllegalArgumentException if {@code selector} or {@code mBeanName} is null.
   * @exclude
   */
  @MBeanExclude
  String registerForwardingNotificationListener(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("mbeanName") final String mBeanName) throws IllegalArgumentException;

  /**
   * Unregister the specified listener.
   * @param listenerID the ID of the listener to unregister.
   * @throws ListenerNotFoundException if the listener could not be found.
   * @exclude
   */
  @MBeanExclude
  void unregisterForwardingNotificationListener(@MBeanParamName("listenerId") String listenerID) throws ListenerNotFoundException;
}
