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

package org.jppf.server.nio.client;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.io.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Context associated with a channel receiving jobs from a client, and sending the results back.
 * @author Laurent Cohen
 */
public class ClientContext extends AbstractNioContext<ClientState>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientContext.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the job manager.
   */
  private static JPPFJobManager jobManager = JPPFDriver.getInstance().getJobManager();
  /**
   * The message wrapping the data sent or received over the socket channel.
   */
  protected ClientMessage clientMessage = null;
  /**
   * The task bundle to send or receive.
   */
  protected ServerJob bundle = null;
  /**
   * Helper used to serialize the bundle objects.
   */
  protected SerializationHelper helper = new SerializationHelperImpl();
  /**
   * True means the job was cancelled and the task completion listener must not be called.
   */
  protected boolean jobCanceled = false;
  /**
   * List of completed bundles to send to the client.
   */
  protected final LinkedList<ServerJob> completedBundles = new LinkedList<ServerJob>();
  /**
   * Number of tasks that remain to be sent to the client.
   */
  protected AtomicInteger pendingTasksCount = new AtomicInteger(0);
  /**
   * The id of the last job submitted via this connection.
   */
  private String currentJobId = null;
  /**
   * The job as initially submitted by the client.
   */
  private ServerJob initialBundleWrapper;
  /**
   * Unique ID for the connection.
   */
  private String connectionUuid = null;
  /**
   * Unique ID for the client.
   */
  private String clientUuid = null;

  /**
   * Get the task bundle to send or receive.
   * @return a <code>BundleWrapper</code> instance.
   */
  public ServerJob getBundle()
  {
    return bundle;
  }

  /**
   * Set the task bundle to send or receive.
   * @param bundle a {@link JPPFTaskBundle} instance.
   */
  public void setBundle(final ServerJob bundle)
  {
    this.bundle = bundle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleException(final ChannelWrapper<?> channel)
  {
    ClientNioServer.closeClient(channel);
    if (clientUuid != null)
    {
      ClassNioServer classServer = JPPFDriver.getInstance().getClassServer();
      List<ChannelWrapper<?>> list = classServer.getProviderConnections(clientUuid);
      if ((list != null) && !list.isEmpty())
      {
        for (ChannelWrapper<?> classChannel: list)
        {
          ClassContext ctx = (ClassContext) classChannel.getContext();
          if (ctx.getConnectionUuid().equals(connectionUuid))
          {
            ClassNioServer.closeConnection(channel);
            break;
          }
        }
      }
    }
    cancelJobOnClose();
  }

  /**
   * Serialize this context's bundle into a byte buffer.
   * @throws Exception if any error occurs.
   */
  public void serializeBundle() throws Exception
  {
    ClientMessage message = newMessage();
    message.addLocation(IOHelper.serializeData(bundle.getJob(), helper.getSerializer()));
    for (DataLocation dl: bundle.getTasks()) message.addLocation(dl);
    message.setBundle((JPPFTaskBundle) bundle.getJob());
    setClientMessage(message);
  }

  /**
   * Deserialize a task bundle from the message read into this buffer.
   * @return a {@link ClientContext} instance.
   * @throws Exception if an error occurs during the deserialization.
   */
  public ServerJob deserializeBundle() throws Exception
  {
    List<DataLocation> locations = clientMessage.getLocations();
    JPPFTaskBundle bundle = clientMessage.getBundle();
    BundleWrapper wrapper = new BundleWrapper(bundle);
    wrapper.setDataProvider(locations.get(1));
    if (locations.size() > 2)
    {
      for (int i=2; i<locations.size(); i++) wrapper.addTask(locations.get(i));
    }
    return wrapper;
  }

  /**
   * Create a new message.
   * @return an {@link AbstractNodeMessage} instance.
   */
  public ClientMessage newMessage()
  {
    return new ClientMessage();
  }

  /**
   * Get the message wrapping the data sent or received over the socket channel.
   * @return a {@link ClientMessage NodeMessage} instance.
   */
  public ClientMessage getClientMessage()
  {
    return clientMessage;
  }

  /**
   * Set the message wrapping the data sent or received over the socket channel.
   * @param nodeMessage a {@link ClientMessage NodeMessage} instance.
   */
  public void setClientMessage(final ClientMessage nodeMessage)
  {
    this.clientMessage = nodeMessage;
  }

  /**
   * Get the uuid of the corresponding node.
   * @return the uuid as a string.
   */
  public String getClientUuid()
  {
    return clientUuid;
  }

  /**
   * Set the uuid of the corresponding node.
   * @param nodeUuid the uuid as a string.
   */
  public void setClientUuid(final String nodeUuid)
  {
    this.clientUuid = nodeUuid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> channel) throws Exception
  {
    if (clientMessage == null) clientMessage = newMessage();
    return getClientMessage().read(channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception
  {
    return getClientMessage().write(channel);
  }

  /**
   * Determine whether the job was canceled.
   * @return true if the job was canceled, false otherwise.
   */
  public synchronized boolean isJobCanceled()
  {
    return jobCanceled;
  }

  /**
   * Specify whether the job was canceled.
   * @param jobCanceled true if the job was canceled, false otherwise.
   */
  public synchronized void setJobCanceled(final boolean jobCanceled)
  {
    this.jobCanceled = jobCanceled;
  }

  /**
   * Add a completed bundle to the queue of bundles to send to the client
   * @param bundleWrapper the bundle to add.
   */
  public void offerCompletedBundle(final ServerJob bundleWrapper)
  {
    synchronized(completedBundles)
    {
      completedBundles.offer(bundleWrapper);
    }
  }

  /**
   * Get the next bundle in the queue.
   * @return A {@link BundleWrapper} instance, or null if the queue is empty.
   */
  public ServerJob pollCompletedBundle()
  {
    synchronized(completedBundles)
    {
      return completedBundles.poll();
    }
  }

  /**
   * Get the number of tasks that remain to be sent to the client.
   * @return the number of tasks as an int.
   */
  public int getPendingTasksCount()
  {
    return pendingTasksCount.get();
  }

  /**
   * Set the number of tasks that remain to be sent to the client.
   * @param pendingTasksCount the number of tasks as an int.
   */
  public void setPendingTasksCount(final int pendingTasksCount)
  {
    this.pendingTasksCount.set(pendingTasksCount);
  }

  /**
   * Determine whether list of completed bundles is empty.
   * @return whether list of <code>ServerJob</code> instances is empty.
   */
  public boolean isCompletedBundlesEmpty()
  {
    synchronized (completedBundles)
    {
      return completedBundles.isEmpty();
    }
  }
  /**
   * Get the id of the last job submitted via this connection.
   * @return the id as a string.
   */
  public synchronized String getCurrentJobId()
  {
    return currentJobId;
  }

  /**
   * Set the id of the last job submitted via this connection.
   * @param currentJobId the id as a string.
   */
  public synchronized void setCurrentJobId(final String currentJobId)
  {
    this.currentJobId = currentJobId;
  }

  /**
   * Send the job ended notification.
   */
  synchronized void jobEnded()
  {
    if (currentJobId != null)
    {
      currentJobId = null;
      jobManager.jobEnded(initialBundleWrapper);
      initialBundleWrapper = null;
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
      JPPFTaskBundle header = (JPPFTaskBundle) initialBundleWrapper.getJob();
      header.setCompletionListener(null);
      if (header.getSLA().isCancelUponClientDisconnect())
      {
        JMXDriverConnectionWrapper wrapper = new JMXDriverConnectionWrapper();
        wrapper.connect();
        try
        {
          wrapper.cancelJob(header.getUuid());
        }
        catch (Exception e)
        {
          String s = ExceptionUtils.getMessage(e);
          if (debugEnabled) log.error(s, e);
          else log.warn(s);
        }
      }
      jobManager.jobEnded(initialBundleWrapper);
      initialBundleWrapper = null;
    }
  }

  /**
   * Set the job as initially submitted by the client.
   * @param initialBundleWrapper <code>BundleWrapper</code> instance.
   */
  synchronized void setInitialBundleWrapper(final ServerJob initialBundleWrapper)
  {
    this.initialBundleWrapper = initialBundleWrapper;
  }

  /**
   * Get the unique ID for the connection.
   * @return the connection id.
   */
  public String getConnectionUuid()
  {
    return connectionUuid;
  }

  /**
   * Set the unique ID for the connection.
   * @param connectionUuid the connection id.
   */
  public void setConnectionUuid(final String connectionUuid)
  {
    this.connectionUuid = connectionUuid;
  }
}
