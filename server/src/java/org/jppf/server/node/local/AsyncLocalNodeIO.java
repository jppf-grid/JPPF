/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.node.local;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.io.*;
import org.jppf.node.protocol.*;
import org.jppf.server.nio.nodeserver.LocalNodeMessage;
import org.jppf.server.nio.nodeserver.async.*;
import org.jppf.server.node.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class AsyncLocalNodeIO extends AbstractLocalNodeIO {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AsyncLocalNodeIO.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The I/O channel for this node.
   */
  private AsyncNodeContext channel;

  /**
   * Initialize this TaskIO with the specified node.
   * @param node - the node who owns this TaskIO.
   */
  public AsyncLocalNodeIO(final JPPFLocalNode node) {
    super(node);
    this.channel = (AsyncNodeContext) node.getNodeConnection().getChannel();
  }

  @Override
  protected Object[] deserializeObjects() throws Exception {
    Object[] result = null;
    synchronized(channel.getLocalNodeReadLock()) {
      if (debugEnabled) log.debug("waiting for next request");
      // wait until a message has been sent by the server
      currentMessage = (LocalNodeMessage) channel.takeNextMessageToSend();
      if (debugEnabled) log.debug("got request");
    }
    final DataLocation location = currentMessage.getLocations().get(0);
    final TaskBundle bundle = (TaskBundle) IOHelper.unwrappedData(location, node.getHelper().getSerializer());
    if (debugEnabled) log.debug("got bundle " + bundle);
    result = deserializeObjects(bundle);
    if (debugEnabled) log.debug("got all data");
    return result;
  }

  @Override
  protected void sendResults(final TaskBundle bundle, final List<Task<?>> tasks) throws Exception {
    if (debugEnabled) log.debug("writing {} results for {}", tasks.size(), bundle);
    final ExecutorService executor = node.getSerializationExecutor();
    finalizeBundleData(bundle, tasks);
    final List<Future<DataLocation>> futureList = new ArrayList<>(tasks.size() + 1);
    final JPPFContainer cont = node.getContainer(bundle.getUuidPath().getList());
    int submitCount = 0;
    futureList.add(executor.submit(new ObjectSerializationTask(bundle, cont, submitCount++)));
    for (Task<?> task : tasks) futureList.add(executor.submit(new ObjectSerializationTask(task, cont, submitCount++)));
    final LocalNodeMessage message = (LocalNodeMessage) channel.newMessage();
    for (final Future<DataLocation> f: futureList) {
      final DataLocation location = f.get();
      message.addLocation(location);
    }
    message.setBundle(bundle);
    synchronized(channel.getLocalNodeWriteLock()) {
      if (debugEnabled) log.debug("wrote full results");
      // wait until the message has been read by the server
      final AsyncNodeMessageHandler handler = channel.getServer().getMessageHandler();
      if (bundle.isHandshake()) handler.handshakeReceived(channel, message);
      else handler.resultsReceived(channel, message);
    }
  }
}
