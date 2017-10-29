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

package test.org.jppf.jmxremote;

import java.io.Serializable;

import javax.management.NotificationEmitter;

/**
 * 
 * @author Laurent Cohen
 */
public interface ConnectorTestMBean extends Serializable, NotificationEmitter {
  /**
   * Name of the node's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=TestConnector,type=test";

  /**
   * Test method.
   * @param stringParam .
   * @param intParam .
   * @return .
   */
  String test1(String stringParam, int intParam);

  /**
   * @return a string.
   */
  String getStringParam();

  /**
   * Set the string parameter.
   * @param stringParam the parameter's value.
   */
  void setStringParam(String stringParam);

  /**
   * @return an int.
   */
  int getIntParam();

  /**
   * Set the int parameter.
   * @param intParam the parameter's value.
   */
  void setIntParam(int intParam);

  /**
   * Send a notification for each of the specified messages, where user data is set to the message.
   * @param messages the messages to send.
   */
  void triggerNotifications(final String...messages);
}
