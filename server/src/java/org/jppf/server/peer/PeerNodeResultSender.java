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

/**
 * Result sender for a peer driver.<br>
 * Instances of this class are used by a driver to receive task bundles from another driver
 * and send the results back to this driver.
 * @author Laurent Cohen
 */
class PeerNodeResultSender implements TaskCompletionListener
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
  private OutputDestination destination = null;
  /**
   * Number of tasks that haven't yet been executed.
   */
  int pendingTasksCount = 0;
  /**
   * Used to serialize and deserialize the tasks data.
   */
  protected SerializationHelper helper = new SerializationHelperImpl();
  /**
   * The socket client used to communicate over a socket connection.
   */
  protected SocketWrapper socketClient = null;
  /**
   * 
   */
  protected ServerJob result = null;

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
    while (pendingTasksCount > 0) wait();
    sendResults(result);
  }

  /**
   * Send the results of the tasks in a bundle back to the client who submitted the request.
   * @param bundleWrapper the bundle to get the task results from.
   * @throws Exception if an IO exception occurred while sending the results back.
   */
  public void sendResults(final ServerJob bundleWrapper) throws Exception
  {
    try
    {
      JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
      if (debugEnabled) log.debug("Sending bundle with " + bundle.getTaskCount() + " tasks");
      IOHelper.sendData(socketClient, bundle, helper.getSerializer());
      for (DataLocation task : bundleWrapper.getTasks())
      {
        destination.writeInt(task.getSize());
        task.transferTo(destination, true);
      }
      socketClient.flush();
      if (debugEnabled) log.debug("bundle sent");
    }
    finally
    {
      result = null;
    }
  }


  /**
   * Callback method invoked when the execution of a task has completed. This
   * method triggers a check of the request completion status. When all tasks
   * have completed, this connection sends all results back.
   * @param nodeResult the result of the task's execution.
   */
  @Override
  public synchronized void taskCompleted(final ServerJob nodeResult)
  {
    JPPFTaskBundle resultJob = (JPPFTaskBundle) nodeResult.getJob();
    pendingTasksCount -= resultJob.getTaskCount();
    if (debugEnabled)
    {
      log.debug("Received results for : " + resultJob.getTaskCount() + " [size=" + nodeResult.getTasks().size() + "] tasks, " + ", pending tasks: " + pendingTasksCount);
    }
    if (result == null) result = nodeResult;
    else
    {
      ((BundleWrapper) result).merge(nodeResult, true);
    }
    if (pendingTasksCount <= 0) notify();
  }
}
