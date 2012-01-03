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
package org.jppf.server.app;

import java.io.InputStream;
import java.net.*;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.io.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.ClassNioServer;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Instances of this class listen to incoming client application requests, so as
 * to dispatch them for execution.<br>
 * When the execution of a task is complete, this connection is automatically
 * notified, through an asynchronous event mechanism.
 * @author Laurent Cohen
 */
class ApplicationConnection extends JPPFConnection
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ApplicationConnection.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * IO wrapper for the header.
   */
  private BundleWrapper headerWrapper = null;
  /**
   * Helper used to serialize or deserialize task bundles.
   */
  private SerializationHelper helper = new SerializationHelperImpl();
  /**
   * Input source for the socket client.
   */
  private InputSource source = null;
  /**
   * Total number of tasks submitted to this application connection.
   */
  private int totalTaskCount = 0;
  /**
   * Used to send the task results back to the requester.
   */
  private ApplicationResultSender resultSender = null;
  /**
   * Reference to the driver.
   */
  private JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * The id of the last job submitted via this connection.
   */
  private String currentJobId = null;

  /**
   * Initialize this connection with an open socket connection to a remote client.
   * @param server the server that created this connection.
   * @param socket the socket connection from which requests are received and to
   * which responses are sent.
   * @throws JPPFException if this socket handler can't be initialized.
   */
  public ApplicationConnection(final JPPFServer server, final Socket socket) throws JPPFException
  {
    super(server, socket);
    source = new SocketWrapperInputSource(socketClient);
    resultSender = new ApplicationResultSender(socketClient);
    InetAddress addr = socket.getInetAddress();
    setName("appl [" + addr.getHostAddress() + ':' + socket.getPort() + ']');
    driver.getStatsManager().newClientConnection();
  }

  /**
   * Handle execution requests from a remote client application, and send the
   * results back to the client.<br>
   * The main flow is as follows:
   * <ul>
   * <li>receive an execution request</li>
   * <li>extract the execution tasks and add them to the execution queue</li>
   * <li>wait until all tasks completion notifications have been received</li>
   * <li>recompose the tasks results in the same order as they were received</li>
   * <li><send results back to the client/li>
   * </ul>
   * 
   * @throws Exception if an error is raised while processing an execution request.
   * @see org.jppf.server.app.JPPFConnection#perform()
   */
  @Override
  public void perform() throws Exception
  {
    if (debugEnabled) log.debug("before reading header");
    // Read the request header - with tasks count information
    DataLocation dl = IOHelper.readData(source);
    InputStream is = dl.getInputStream();
    byte[] data;
    try
    {
      data = FileUtils.getInputStreamAsByte(is);
    }
    finally
    {
      StreamUtils.close(is);
    }
    data = JPPFDataTransformFactory.transform(false, data, 0, data.length);
    JPPFTaskBundle header = (JPPFTaskBundle) helper.getSerializer().deserialize(data);
    if (debugEnabled) log.debug("received header from client, data length = " + data.length);
    if (header.getParameter(BundleParameter.JOB_RECEIVED_TIME) == null)
      header.setParameter(BundleParameter.JOB_RECEIVED_TIME, System.currentTimeMillis());
    headerWrapper = new BundleWrapper(header);
    executeTasks();
  }

  /**
   * Execute the tasks received from a client.
   * @throws Exception if the tasks could not be read.
   */
  protected void executeTasks() throws Exception
  {
    JPPFTaskBundle header = (JPPFTaskBundle) headerWrapper.getJob();
    header.getUuidPath().incPosition();
    String uuid = header.getUuidPath().getCurrentElement();
    ClassNioServer classServer = JPPFDriver.getInstance().getClassServer();
    List<ChannelWrapper<?>> list = classServer.getProviderConnections(uuid);
    while ((list == null) || list.isEmpty())
    {
      Thread.sleep(1L);
      list = classServer.getProviderConnections(uuid);
    }
    int count = header.getTaskCount();
    if (debugEnabled) log.debug("Received " + count + " tasks");

    for (int i=0; i<count+1; i++)
    {
      DataLocation dl = IOHelper.readData(source);
      if (i == 0)
      {
        headerWrapper.setDataProvider(dl);
        if (log.isTraceEnabled()) log.trace("received data provider from client, data length = " + dl.getSize());
      }
      else
      {
        headerWrapper.addTask(dl);
        if (log.isTraceEnabled()) log.trace("received task #" + i + " from client, data length = " + dl.getSize());
      }
    }

    header.getUuidPath().add(driver.getUuid());
    header.setCompletionListener(resultSender);
    currentJobId = header.getUuid();
    JPPFDriver.getQueue().addBundle(headerWrapper);
    if (count > 0)
    {
      totalTaskCount += count;
      if (debugEnabled) log.debug("Queued " + totalTaskCount + " tasks");
      resultSender.run(count);
    }
    else resultSender.sendPartialResults(headerWrapper);
    jobEnded();
  }

  /**
   * Close this application connection.
   * @see org.jppf.server.app.JPPFConnection#close()
   */
  @Override
  public void close()
  {
    if (debugEnabled) log.debug("closing " + this);
    cancelJobOnClose();
    super.close();
    driver.getStatsManager().clientConnectionClosed();
  }

  /**
   * Send the job ended notification.
   */
  private synchronized void jobEnded()
  {
    if (currentJobId != null)
    {
      currentJobId = null;
      JPPFDriver.getInstance().getJobManager().jobEnded(headerWrapper);
    }
  }

  /**
   * Send the job ended notification.
   */
  private synchronized void cancelJobOnClose()
  {
    if (currentJobId != null)
    {
      currentJobId = null;
      JPPFTaskBundle header = (JPPFTaskBundle) headerWrapper.getJob();
      header.setCompletionListener(null);
      JMXDriverConnectionWrapper wrapper = new JMXDriverConnectionWrapper();
      wrapper.connect();
      try
      {
        wrapper.cancelJob(header.getUuid());
      }
      catch (Exception e)
      {
        if (debugEnabled) log.error(ExceptionUtils.getMessage(e), e);
        else log.warn(ExceptionUtils.getMessage(e));
      }
      JPPFDriver.getInstance().getJobManager().jobEnded(headerWrapper);
    }
  }

  /**
   * Get a string representation of this connection.
   * @return a string representation of this connection.
   * @see org.jppf.server.app.JPPFConnection#toString()
   */
  @Override
  public String toString()
  {
    return "application connection : " + super.toString();
  }
}
