/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.management;

import java.io.Serializable;

import javax.management.NotificationEmitter;

/**
 * This MBean notifies any listener of changes to the number of threads of a node.
 * @author Laurent Cohen
 */
public interface NodeConfigNotifierMBean extends Serializable, NotificationEmitter {
  /**
   * The name under which this MBean is registered with the MBean server.
   */
  String MBEAN_NAME = "org.jppf:name=config.notifier,type=node";
}
