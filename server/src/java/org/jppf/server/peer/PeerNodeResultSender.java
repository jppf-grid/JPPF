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
package org.jppf.server.peer;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

import java.util.List;

/**
 * Result sender for a peer driver.<br>
 * Instances of this class are used by a driver to receive task bundles from another driver
 * and send the results back to this driver.
 * @author Laurent Cohen
 */
class PeerNodeResultSender implements ServerTaskBundleClient.CompletionListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PeerNodeResultSender.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Output destination wrapping all write operations on the socket client.
   */
  private final OutputDestination destination;
  /**
   * Used to serialize and deserialize the tasks data.
   */
  protected SerializationHelper helper = new SerializationHelperImpl();
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
  public PeerNodeResultSender(final SocketWrapper socketClient)
  {
    this.socketClient = socketClient;
    destination = new SocketWrapperOutputDestination(socketClient);
  }

  /**
   * This method waits until all tasks of a request have been completed.
   * @throws Exception if handing of the results fails.
   */
  public synchronized void waitForExecution() throws Exception
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    while (bundle.getPendingTasksCount() > 0) wait();
  }

  /**
   * Send the results of the tasks in a bundle back to the client who submitted the request.
   * @param bundleWrapper the bundle to get the task results from.
   * @throws Exception if an IO exception occurred while sending the results back.
   */
  public void sendResults(final ServerTaskBundleClient bundleWrapper) throws Exception
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    JPPFTaskBundle bundle = bundleWrapper.getJob();
    if (debugEnabled) log.debug("Sending bundle with " + bundle.getTaskCount() + " tasks");
    IOHelper.sendData(socketClient, bundle, helper.getSerializer());
    for (ServerTask task : bundleWrapper.getTaskList())
    {
      DataLocation dl = task.getResult();
      destination.writeInt(dl.getSize());
      dl.transferTo(destination, true);
    }
    socketClient.flush();
    if (debugEnabled) log.debug("bundle sent");
  }


  @Override
  public void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results) {
    if (bundle == null) throw new IllegalStateException("bundle is null");

    if (bundle.isCancelled())
    {
      bundle.removeCompletionListener(this);
    } else {
      int pendingTasksCount = this.bundle.getPendingTasksCount();
      if (pendingTasksCount <= 0) notifyAll();
    }
  }

  @Override
  public void bundleDone(final ServerTaskBundleClient bundle) {
  }
}
