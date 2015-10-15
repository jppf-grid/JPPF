/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.server.node.remote;

import static org.jppf.node.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.node.protocol.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.server.node.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 */
public class RemoteNodeIO extends AbstractNodeIO {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RemoteNodeIO.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  /**
   * Initialize this TaskIO with the specified node.
   * @param node - the node who owns this TaskIO.
   */
  public RemoteNodeIO(final JPPFNode node) {
    super(node);
  }

  @Override
  protected Object[] deserializeObjects() throws Exception {
    ObjectSerializer ser = node.getHelper().getSerializer();
    if (debugEnabled) log.debug("waiting for next request. Serializer = " + ser + " (class loader = " + ser.getClass().getClassLoader() + ")");
    TaskBundle bundle = (TaskBundle) IOHelper.unwrappedData(getSocketWrapper(), node.getHelper().getSerializer());
    node.setExecuting(true);
    if (debugEnabled) log.debug("got bundle " + bundle);
    if (!bundle.isHandshake()) node.getExecutionManager().setBundle(bundle);
    Object[] result = deserializeObjects(bundle);
    //if (node.isOffline() && !bundle.isHandshake()) sendOfflineCloseRequest(bundle);
    if (node.isOffline() && !bundle.isHandshake()) waitChannelClosed(getSocketWrapper());
    return result;
  }

  @Override
  protected Object[] deserializeObjects(final TaskBundle bundle) throws Exception {
    int count = bundle.getTaskCount();
    List<Object> list = new ArrayList<>(count + 2);
    list.add(bundle);
    try {
      initializeBundleData(bundle);
      if (debugEnabled) log.debug("bundle task count = " + count + ", handshake = " + bundle.isHandshake());
      if (!bundle.isHandshake()) {
        JPPFRemoteContainer cont = (JPPFRemoteContainer) node.getContainer(bundle.getUuidPath().getList());
        cont.setNodeConnection((RemoteNodeConnection) node.getNodeConnection());
        cont.getClassLoader().setRequestUuid(bundle.getUuid());
        if (!node.isOffline() && !bundle.getSLA().isRemoteClassLoadingEnabled()) cont.getClassLoader().setRemoteClassLoadingDisabled(true);
        node.getLifeCycleEventHandler().fireJobHeaderLoaded(bundle, cont.getClassLoader());
        cont.deserializeObjects(list, 1+count, node.getExecutionManager().getExecutor());
      } else {
        // skip null data provider
        getSocketWrapper().receiveBytes(0);
      }
      if (debugEnabled) log.debug("got all data");
    } catch(Throwable t) {
      log.error("Exception occurred while deserializing the tasks", t);
      bundle.setTaskCount(0);
      bundle.setParameter(NODE_EXCEPTION_PARAM, t);
    }
    return list.toArray(new Object[list.size()]);
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
    getSocketWrapper().setSerializer(node.getHelper().getSerializer());
  }

  @Override
  protected void sendResults(final TaskBundle bundle, final List<Task<?>> tasks) throws Exception {
    if (debugEnabled) log.debug("writing results for " + bundle);
    SocketWrapper socketWrapper = getSocketWrapper();
    if (socketWrapper == null) throw new SocketException("no connection to the server");
    ExecutorService executor = node.getExecutionManager().getExecutor();
    finalizeBundleData(bundle, tasks);
    List<Future<DataLocation>> futureList = new ArrayList<>(tasks.size() + 1);
    futureList.add(executor.submit(new ObjectSerializationTask(bundle)));
    for (Task task : tasks) futureList.add(executor.submit(new ObjectSerializationTask(task)));
    OutputDestination dest = new SocketWrapperOutputDestination(socketWrapper);
    int count = 0;
    for (Future<DataLocation> f: futureList) {
      DataLocation dl = f.get();
      if (traceEnabled) log.trace("writing "  + (count == 0 ? "header" : "task[" + count + ']') + " with size = " + dl.getSize());
      IOHelper.writeData(dl, dest);
      count++;
    }
    socketWrapper.flush();
    if (debugEnabled) log.debug("wrote full results");
  }

  /**
   * Wait until the connection is closed by the other end.
   * @param socketWrapper the connection to check.
   */
  private void waitChannelClosed(final SocketWrapper socketWrapper) {
    try {
      socketWrapper.readInt();
    } catch (Exception ignore) {
    } catch (Error e) {
      if (debugEnabled) log.debug("error closing socket: ", e);
    }
    if (traceEnabled) log.trace("server closed the connection");
    try {
      node.closeDataChannel();
    } catch (Exception ignore) {
    } catch (Error e) {
      if (debugEnabled) log.debug("error closing data channel: ", e);
    }
    if (traceEnabled) log.trace("closed the data channel");
  }

  /**
   * Get the socket wrapper associated with the node connection.
   * @return a {@link SocketWrapper} instance.
   */
  private SocketWrapper getSocketWrapper() {
    return ((RemoteNodeConnection) node.getNodeConnection()).getChannel();
  }
}
