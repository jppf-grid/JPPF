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

package org.jppf.server.nio.client;

import org.jppf.io.IOHelper;
import org.jppf.nio.*;
import org.jppf.node.protocol.*;
import org.jppf.node.protocol.graph.TaskGraphInfo;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.server.protocol.ServerTaskBundleClient;
import org.slf4j.*;

/**
 * Representation of a message sent or received by a remote node.
 * @author Laurent Cohen
 */
public class ClientMessage extends AbstractTaskBundleMessage {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientMessage.class);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The actual client task bundle from which this message is created
   */
  private final ServerTaskBundleClient clientBundle;

  /**
   * Initialize this nio message with the specified context.
   * @param context the context to read from or write to.
   * @param clientBundle the actual client task bundle from which this message is created.
   */
  public ClientMessage(final NioContext context, final ServerTaskBundleClient clientBundle) {
    super(context);
    this.clientBundle = clientBundle;
  }

  /**
   * Actions to take after the first object in the message has been fully read.
   * @throws Exception if an IO error occurs.
   */
  @Override
  protected void afterFirstRead() throws Exception {
    bundle = (TaskBundle) IOHelper.unwrappedData(locations.get(0));
    final TaskGraphInfo graphInfo = bundle.getParameter(BundleParameter.JOB_TASK_GRAPH_INFO, null);
    final int dependencyCount = (graphInfo == null) ? 0 : graphInfo.getNbDependencies();
    nbObjects = bundle.getTaskCount() + 2 + dependencyCount;
    if (traceEnabled) log.trace("read task bundle with {} dependencies : {}, context = {}", dependencyCount, bundle, channel);
  }

  /**
   * Actions to take before the first object in the message is written.
   * @throws Exception if an IO error occurs.
   */
  @Override
  protected void beforeFirstWrite() throws Exception {
    nbObjects = bundle.getTaskCount() + 1;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("nb locations=").append(locations == null ? -1 : locations.size())
      .append(", position=").append(position)
      .append(", nbObjects=").append(nbObjects)
      .append(", count=").append(count)
      .append(", currentLength=").append(currentLength)
      .append(']').toString();
  }

  /**
   * @return the actual client task bundle from which this message is created
   */
  public ServerTaskBundleClient getClientBundle() {
    return clientBundle;
  }

  @Override
  protected boolean readNextObject() throws Exception {
    final boolean result = super.readNextObject();
    if (traceEnabled && result) log.trace("read next object of {}, context = {}", this, channel);;
    return result;
  }
}
