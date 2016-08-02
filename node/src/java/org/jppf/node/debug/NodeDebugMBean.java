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

package org.jppf.node.debug;

import java.io.Serializable;

/**
 * 
 * @author Laurent Cohen
 */
public interface NodeDebugMBean extends Serializable {
  /**
   * The name of this mbean.
   */
  String MBEAN_NAME = "org.jppf:name=debug,type=node";

  /**
   * Print the specified messages to the server's log.
   * @param messages the messages ot print.
   */
  void log(String...messages);
}
