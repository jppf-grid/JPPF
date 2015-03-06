/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

/**
 * MBean interface for various maintenance operations on the nodes.
 * @author Laurent Cohen
 */
public interface JPPFNodeMaintenanceMBean extends Serializable
{
  /**
   * Object name for this MBean.
   */
  String MBEAN_NAME = "org.jppf:name=node.maintenance,type=node";

  /**
   * Request a reset of the resource caches of all the JPPF class loaders maintained by the node.<br>
   * This method does not perform the reset imediately. Instead, it sets an internal flag, and the reset
   * will take place when it is safe to do so, as part of the node's life cycle.
   * @throws Exception if any error occurs while requesting the reset.
   */
  void requestResourceCacheReset() throws Exception;
}
