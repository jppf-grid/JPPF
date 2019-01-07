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

import static org.jppf.node.protocol.BundleParameter.*;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFUnsupportedOperationException;
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
public class AsyncLocalNodeIO extends AbstractNodeIO<JPPFLocalNode> {
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
    LocalNodeMessage currentMessage = null;
    synchronized(channel.getLocalNodeReadLock()) {
      if (debugEnabled) log.debug("waiting for next request");
      // wait until a message has been sent by the server
      currentMessage = (LocalNodeMessage) channel.takeNextMessageToSend();
      if (debugEnabled) log.debug("got request");
    }
    final DataLocation location = currentMessage.getLocations().get(0);
    final TaskBundle bundle = (TaskBundle) IOHelper.unwrappedData(location, node.getHelper().getSerializer());
    if (debugEnabled) log.debug("got bundle " + bundle);
    result = deserializeObjects(bundle, currentMessage);
    if (debugEnabled) log.debug("got all data");
    return result;
  }

  /**
   * Performs the actions required if reloading the classes is necessary.
   * @throws Exception if any error occurs.
   */
  @Override
  protected void handleReload() throws Exception {
    node.setClassLoader(null);
    node.initHelper();
  }

  @Override
  protected Object[] deserializeObjects(final TaskBundle bundle) throws Exception {
    throw new JPPFUnsupportedOperationException("method " + getClass().getName() + ".deserializeObjects(TaskBundle) should never be called for a local node");
  }

  /**
   * Perform the deserialization of the objects received through the socket connection.
   * @param bundle the message header that contains information about the tasks and data provider.
   * @param currentMessage contains the tasks and data provider inserialized form.
   * @return an array of objects deserialized from the socket stream.
   * @throws Exception if an error occurs while deserializing.
   */
  protected Object[] deserializeObjects(final TaskBundle bundle, final LocalNodeMessage currentMessage) throws Exception {
    final int count = bundle.getTaskCount();
    final Object[] list = new Object[count + 2];
    list[0] = bundle;
    try {
      initializeBundleData(bundle);
      if (debugEnabled) log.debug("bundle task count = {}, handshake = {}", count, bundle.isHandshake());
      if (!bundle.isHandshake()) {
        final boolean clientAccess = !bundle.getParameter(FROM_PERSISTENCE, false);
        final JPPFLocalContainer cont = (JPPFLocalContainer) node.getClassLoaderManager().getContainer(bundle.getUuidPath().getList(), clientAccess, (Object[]) null);
        cont.getClassLoader().setRequestUuid(bundle.getUuid());
        if (!node.isOffline() && !bundle.getSLA().isRemoteClassLoadingEnabled()) cont.getClassLoader().setRemoteClassLoadingDisabled(true);
        node.getLifeCycleEventHandler().fireJobHeaderLoaded(bundle, cont.getClassLoader());
        cont.setCurrentMessage(currentMessage);
        cont.deserializeObjects(list, 1 + count, node.getSerializationExecutor());
      } else {
        // skip null data provider
      }
      if (debugEnabled) log.debug("got all data");
    } catch(final Throwable t) {
      log.error("Exception occurred while deserializing the tasks", t);
      bundle.setTaskCount(0);
      bundle.setParameter(NODE_EXCEPTION_PARAM, t);
    }
    return list;
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
      final AsyncNodeMessageHandler handler = channel.getServer().getMessageHandler();
      if (bundle.isHandshake()) handler.handshakeReceived(channel, message);
      else handler.resultsReceived(channel, message);
    }
  }
}
