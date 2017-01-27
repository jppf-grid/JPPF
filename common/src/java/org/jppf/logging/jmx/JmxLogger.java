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

package org.jppf.logging.jmx;

import javax.management.NotificationEmitter;


/**
 * MBean interface for sending log and trace messages as JMX notifications.
 * @author Laurent Cohen
 */
public interface JmxLogger extends NotificationEmitter {
  /**
   * Name of the driver's logger MBean.
   */
  String DEFAULT_MBEAN_NAME = "org.jppf:name=jmxlogger,type=all";
  /**
   * Log the specified message with the specified level.
   * @param message the message to log.
   */
  void log(String message);
}
