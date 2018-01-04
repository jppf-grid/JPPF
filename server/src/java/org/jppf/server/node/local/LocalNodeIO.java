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

package org.jppf.server.node.local;

import static java.nio.channels.SelectionKey.*;
import static org.jppf.node.protocol.BundleParameter.*;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.io.*;
import org.jppf.node.protocol.*;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class LocalNodeIO extends AbstractNodeIO {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LocalNodeIO.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The I/O channel for this node.
   */
  private LocalNodeChannel channel = null;
  /**
   * The message to deserialize.
   */
  private LocalNodeMessage currentMessage = null;

  /**
   * Initialize this TaskIO with the specified node.
   * @param node - the node who owns this TaskIO.
   */
  public LocalNodeIO(final JPPFNode node) {
    super(node);
    this.channel = ((LocalNodeConnection) node.getNodeConnection()).getChannel();
  }

  /**
   * Performs the actions required if reloading the classes is necessary.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.node.AbstractNodeIO#handleReload()
   */
  @Override
  protected void handleReload() throws Exception {
    node.setClassLoader(null);
    node.initHelper();
  }

  @Override
  protected Object[] deserializeObjects() throws Exception {
    Object[] result = null;
    synchronized(channel.getNodeLock()) {
      channel.setReadyOps(OP_WRITE);
      if (debugEnabled) log.debug("waiting for next request");
      // wait until a message has been sent by the server
      while ((currentMessage = channel.getNodeResource()) == null) channel.getNodeLock().goToSleep();
      if (debugEnabled) log.debug("got request");
      channel.setReadyOps(0);
      channel.setNodeResource(null);
    }
    final DataLocation location = currentMessage.getLocations().get(0);
    final TaskBundle bundle = (TaskBundle) IOHelper.unwrappedData(location, node.getHelper().getSerializer());
    if (debugEnabled) log.debug("got bundle " + bundle);
    node.getExecutionManager().setBundle(bundle);
    result = deserializeObjects(bundle);
    if (debugEnabled) log.debug("got all data");
    return result;
  }

  @Override
  protected Object[] deserializeObjects(final TaskBundle bundle) throws Exception {
    final int count = bundle.getTaskCount();
    //List<Object> list = new ArrayList<>(count + 1);
    final Object[] list = new Object[count + 2];
    list[0] = bundle;
    try {
      initializeBundleData(bundle);
      if (debugEnabled) log.debug("bundle task count = " + count + ", handshake = " + bundle.isHandshake());
      if (!bundle.isHandshake()) {
        //JPPFLocalContainer cont = (JPPFLocalContainer) node.getContainer(bundle.getUuidPath().getList());
        final boolean clientAccess = !bundle.getParameter(FROM_PERSISTENCE, false);
        final JPPFLocalContainer cont = (JPPFLocalContainer) node.getClassLoaderManager().getContainer(bundle.getUuidPath().getList(), clientAccess, (Object[]) null);
        cont.getClassLoader().setRequestUuid(bundle.getUuid());
        if (!node.isOffline() && !bundle.getSLA().isRemoteClassLoadingEnabled()) cont.getClassLoader().setRemoteClassLoadingDisabled(true);
        node.getLifeCycleEventHandler().fireJobHeaderLoaded(bundle, cont.getClassLoader());
        cont.setCurrentMessage(currentMessage);
        cont.deserializeObjects(list, 1+count, node.getExecutionManager().getExecutor());
      } else {
        // skip null data provider
      }
      if (debugEnabled) log.debug("got all data");
    } catch(final Throwable t) {
      log.error("Exception occurred while deserializing the tasks", t);
      bundle.setTaskCount(0);
      bundle.setParameter(NODE_EXCEPTION_PARAM, t);
    } finally {
      currentMessage = null;
    }
    return list;
  }

  @Override
  protected void sendResults(final TaskBundle bundle, final List<Task<?>> tasks) throws Exception {
    if (debugEnabled) log.debug("writing results for " + bundle);
    final ExecutorService executor = node.getExecutionManager().getExecutor();
    finalizeBundleData(bundle, tasks);
    final List<Future<DataLocation>> futureList = new ArrayList<>(tasks.size() + 1);
    final JPPFContainer cont = node.getContainer(bundle.getUuidPath().getList());
    futureList.add(executor.submit(new ObjectSerializationTask(bundle, cont.getSerializer(), cont.getClassLoader())));
    for (Task<?> task : tasks) futureList.add(executor.submit(new ObjectSerializationTask(task, cont.getSerializer(), cont.getClassLoader())));
    final LocalNodeContext ctx = channel.getChannel();
    final LocalNodeMessage message = (LocalNodeMessage) ctx.newMessage();
    for (final Future<DataLocation> f: futureList) {
      final DataLocation location = f.get();
      message.addLocation(location);
    }
    message.setBundle(bundle);
    synchronized(channel.getServerLock()) {
      channel.setReadyOps(OP_READ);
      channel.setServerResource(message);
      if (debugEnabled) log.debug("wrote full results");
      // wait until the message has been read by the server
      while (channel.getServerResource() != null) channel.getServerLock().goToSleep();
      channel.setReadyOps(0);
    }
  }
}
