/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.server.node;

import org.jppf.management.JMXServer;
import org.jppf.node.AbstractNode;
import org.jppf.server.protocol.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This class is used a container for common methods that cannot be implemented in {@link AbstractNode}.
 * @author Laurent Cohen
 */
public abstract class AbstractCommonNode extends AbstractNode
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Add management parameters to the specified bundle, before sending it back to a server.
   * @param bundle the bundle to add parameters to.
   */
  protected void setupManagementParameters(final JPPFTaskBundle bundle)
  {
    try
    {
      JMXServer jmxServer = getJmxServer();
      bundle.setParameter(BundleParameter.NODE_MANAGEMENT_HOST_PARAM, jmxServer.getManagementHost());
      bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
      bundle.setParameter(BundleParameter.NODE_MANAGEMENT_ID_PARAM, jmxServer.getId());
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }
}
