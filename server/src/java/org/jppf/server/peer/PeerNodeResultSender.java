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
package org.jppf.server.peer;

import java.util.List;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Result sender for a peer driver.<br>
 * Instances of this class are used by a driver to receive task bundles from another driver
 * and send the results back to this driver.
 * @author Laurent Cohen
 */
class PeerNodeResultSender implements ServerTaskBundleClient.CompletionListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PeerNodeResultSender.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Output destination wrapping all write operations on the socket client.
   */
  private final OutputDestination destination;
  /**
   * The socket client used to communicate over a socket connection.
   */
  protected SocketWrapper socketClient = null;
  /**
   * The bundle being processed.
   */
  ServerTaskBundleClient bundle = null;

  /**
   * Initialize this result sender with a specified socket client.
   * @param socketClient the socket client used to send results back.
   */
  public PeerNodeResultSender(final SocketWrapper socketClient) {
    this.socketClient = socketClient;
    destination = new SocketWrapperOutputDestination(socketClient);
  }

  /**
   * This method waits until all tasks of a request have been completed.
   * @throws Exception if handing of the results fails.
   */
  public synchronized void waitForExecution() throws Exception {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    while (bundle.getPendingTasksCount() > 0) wait();
  }

  /**
   * Send the results of the tasks in a bundle back to the client who submitted the request.
   * @param clientBundle the bundle to get the task results from.
   * @throws Exception if an IO exception occurred while sending the results back.
   */
  public void sendResults(final ServerTaskBundleClient clientBundle) throws Exception {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (clientBundle == null) throw new IllegalArgumentException("bundleWrapper is null");
    TaskBundle bundle = clientBundle.getJob();
    // i don't know why bundle.getTaskCount() = 0 at this point
    bundle.setTaskCount(clientBundle.getTaskCount());
    //bundle.setSLA(null);
    //bundle.setMetadata(null);
    if (debugEnabled) log.debug("Sending bundle with " + clientBundle.getTaskList().size() + " tasks: " + bundle);
    IOHelper.sendData(socketClient, bundle, JPPFDriver.getSerializer());
    for (ServerTask task : clientBundle.getTaskList()) IOHelper.writeData(task.getResult(), destination);
    socketClient.flush();
    if (debugEnabled) log.debug("bundle sent");
  }

  @Override
  public void taskCompleted(final ServerTaskBundleClient clientBundle, final List<ServerTask> results) {
    if (clientBundle == null) throw new IllegalStateException("bundle is null");
    if (clientBundle.isCancelled()) {
      clientBundle.removeCompletionListener(this);
    } else {
      int pendingTasksCount = (bundle == null) ? 0 : bundle.getPendingTasksCount();
      if (debugEnabled) log.debug("Sending notification of bundle with " + clientBundle.getTaskList().size() + " tasks: " + bundle);
      if (pendingTasksCount <= 0) {
        synchronized(this) {
          if (debugEnabled) log.debug("result sender = " + this);
          notifyAll();
        }
      }
    }
  }

  @Override
  public void bundleEnded(final ServerTaskBundleClient bundle) {
    this.bundle = null;
  }
}
