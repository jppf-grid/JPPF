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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.TO_SENDING_NODE_RESPONSE;
import static org.jppf.utils.StringUtils.build;

import org.jppf.nio.NioState;
import org.jppf.server.JPPFDriver;
import org.slf4j.*;

/**
 * Abstract superclass for all possible states of a class server connection.
 * @author Laurent Cohen
 */
public abstract class ClassServerState extends NioState<ClassTransition>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClassServerState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The server that handles this state.
   */
  protected final ClassNioServer server;
  /**
   * Reference to the driver.
   */
  protected final JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public ClassServerState(final ClassNioServer server)
  {
    this.server = server;
  }

  /**
   * Serialize the resource and send it back to the node.
   * @param context the context which serializes the resource.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if any error occurs.
   */
  protected ClassTransition sendResponse(final ClassContext context) throws Exception
  {
    context.serializeResource();
    if (debugEnabled) log.debug(build("sending response ", context.getResource(), " to node: ", context.getChannel()));
    return TO_SENDING_NODE_RESPONSE;
  }
}
