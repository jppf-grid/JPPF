/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.io.*;
import org.jppf.nio.*;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.serialization.SerializationHelper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.ClassContext;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
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
   * Reference to the driver.
   */
  protected static final JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * The task bundle to send or receive.
   */
  protected ServerTaskBundleClient clientBundle = null;
  /**
   * Helper used to serialize the bundle objects.
   */
  protected SerializationHelper helper = new SerializationHelperImpl();
  /**
   * List of completed bundles to send to the client.
   */
  //protected final LinkedList<ServerTaskBundleClient> completedBundles = new LinkedList<>();
  protected final Queue<ServerTaskBundleClient> completedBundles = new ConcurrentLinkedQueue<>();
  /**
   * The job as initially submitted by the client.
   */
  private ServerTaskBundleClient initialBundleWrapper;
  /**
   * The number of tasks remaining to send.
   */
  private int nbTasksToSend = 0;
  /**
   * Unique ID for the client.
   */
  private String clientUuid = null;

  /**
   * Get the task bundle to send or receive.
   * @return a <code>ServerJob</code> instance.
   */
  public ServerTaskBundleClient getBundle()
  {
    return clientBundle;
  }

  /**
   * Set the task bundle to send or receive.
   * @param bundle a {@link JPPFTaskBundle} instance.
   */
  public void setBundle(final ServerTaskBundleClient bundle)
  {
    this.clientBundle = bundle;
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception e)
  {
    ClientNioServer.closeClient(channel);
    if (clientUuid != null)
    {
      ClientClassNioServer classServer = (ClientClassNioServer) JPPFDriver.getInstance().getClientClassServer();
      List<ChannelWrapper<?>> list = classServer.getProviderConnections(clientUuid);
      if ((list != null) && !list.isEmpty())
      {
        for (ChannelWrapper<?> classChannel: list)
        {
          ClassContext ctx = (ClassContext) classChannel.getContext();
          if (ctx.getConnectionUuid().equals(connectionUuid))
          {
            ClientClassNioServer.closeConnection(channel);
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
    TaskBundle bundle = clientBundle.getJob();
    bundle.setSLA(null);
    bundle.setMetadata(null);
    List<ServerTask> tasks = clientBundle.getTaskList();
    int[] positions = new int[tasks.size()];
    for (int i=0; i<tasks.size(); i++) positions[i] = tasks.get(i).getJobPosition();
    if (debugEnabled) log.debug("serializing bundle with tasks postions={}", StringUtils.buildString(positions));
    bundle.setParameter(BundleParameter.TASK_POSITIONS, positions);
    message.addLocation(IOHelper.serializeData(bundle, helper.getSerializer()));
    for (ServerTask task: tasks) message.addLocation(task.getResult());
    message.setBundle(bundle);
    setClientMessage(message);
  }

  /**
   * Deserialize a task bundle from the message read into this buffer.
   * @return a {@link ClientContext} instance.
   * @throws Exception if an error occurs during the deserialization.
   */
  public ServerTaskBundleClient deserializeBundle() throws Exception
  {
    List<DataLocation> locations = ((ClientMessage) message).getLocations();
    TaskBundle bundle = ((ClientMessage) message).getBundle();
    if (locations.size() <= 2) return new ServerTaskBundleClient(bundle, locations.get(1));
    return new ServerTaskBundleClient(bundle, locations.get(1), locations.subList(2, locations.size()));
  }

  /**
   * Create a new message.
   * @return an {@link ClientMessage} instance.
   */
  public ClientMessage newMessage()
  {
    return new ClientMessage(getChannel());
  }

  /**
   * Get the message wrapping the data sent or received over the socket channel.
   * @return a {@link ClientMessage NodeMessage} instance.
   */
  public ClientMessage getClientMessage()
  {
    return (ClientMessage) message;
  }

  /**
   * Set the message wrapping the data sent or received over the socket channel.
   * @param message a {@link ClientMessage NodeMessage} instance.
   */
  public void setClientMessage(final ClientMessage message)
  {
    this.message = message;
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

  @Override
  public boolean readMessage(final ChannelWrapper<?> channel) throws Exception
  {
    if (message == null) message = newMessage();
    boolean b = false;
    try
    {
      b = message.read();
    }
    catch (Exception e)
    {
      updateInStats();
      throw e;
    }
    if (b) updateInStats();
    return b;
  }

  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception
  {
    boolean b = false;
    try
    {
      b = message.write();
    }
    catch (Exception e)
    {
      updateOutStats();
      throw e;
    }
    if (b) updateOutStats();
    return b;
  }

  /**
   * Add a completed bundle to the queue of bundles to send to the client
   * @param bundleWrapper the bundle to add.
   */
  public void offerCompletedBundle(final ServerTaskBundleClient bundleWrapper)
  {
    completedBundles.offer(bundleWrapper);
  }

  /**
   * Get the next bundle in the queue.
   * @return A {@link ServerJob} instance, or null if the queue is empty.
   */
  public ServerTaskBundleClient pollCompletedBundle()
  {
    return completedBundles.poll();
  }

  /**
   * Get the number of tasks that remain to be sent to the client.
   * @return the number of tasks as an int.
   */
  public int getPendingTasksCount()
  {
    if(initialBundleWrapper == null) throw new IllegalStateException("initialBundleWrapper is null");
    return initialBundleWrapper.getPendingTasksCount();
  }

  /**
   * Determine whether list of completed bundles is empty.
   * @return whether list of <code>ServerJob</code> instances is empty.
   */
  public boolean isCompletedBundlesEmpty()
  {
    return completedBundles.isEmpty();
  }

  /**
   * Send the job ended notification.
   */
  void jobEnded()
  {
    ServerTaskBundleClient bundle;
    if ((bundle = getInitialBundleWrapper()) != null)
    {
      bundle.bundleEnded();
      setInitialBundleWrapper(null);
    }
  }

  /**
   * Send the job ended notification.
   */
  void cancelJobOnClose()
  {
    ServerTaskBundleClient bundle;
    if ((bundle = getInitialBundleWrapper()) != null)
    {
      TaskBundle header = bundle.getJob();
      if (debugEnabled) log.debug("cancelUponClientDisconnect = " + header.getSLA().isCancelUponClientDisconnect() + " for " + header);
      if (header.getSLA().isCancelUponClientDisconnect()) bundle.cancel();
      bundle.bundleEnded();
      setInitialBundleWrapper(null);
    }
  }

  /**
   * Get the job as initially submitted by the client.
   * @return a <code>ServerTaskBundleClient</code> instance.
   */
  public synchronized ServerTaskBundleClient getInitialBundleWrapper()
  {
    return initialBundleWrapper;
  }

  /**
   * Set the job as initially submitted by the client.
   * @param initialBundleWrapper <code>ServerTaskBundleClient</code> instance.
   */
  synchronized void setInitialBundleWrapper(final ServerTaskBundleClient initialBundleWrapper)
  {
    this.initialBundleWrapper = initialBundleWrapper;
    nbTasksToSend = initialBundleWrapper == null ? 0 : initialBundleWrapper.getPendingTasksCount();
  }

  @Override
  public String toString()
  {
    return new StringBuilder(super.toString()).append(", nbTasksToSend=").append(nbTasksToSend).toString();
  }

  /**
   * Get the number of tasks remaining to send.
   * @return the number of tasks as an <code>int</code>.
   */
  synchronized int getNbTasksToSend()
  {
    return nbTasksToSend;
  }

  /**
   * Set the number of tasks remaining to send.
   * @param nbTasksToSend the number of tasks as an <code>int</code>.
   */
  synchronized void setNbTasksToSend(final int nbTasksToSend)
  {
    this.nbTasksToSend = nbTasksToSend;
  }

  /**
   * Update the inbound traffic statistics.
   */
  private void updateInStats() {
    if (message != null) {
      long n = message.getChannelCount();
      if (n > 0) driver.getStatistics().addValue(peer ? PEER_IN_TRAFFIC : CLIENT_IN_TRAFFIC, n);
    }
  }

  /**
   * Update the outbound traffic statistics.
   */
  private void updateOutStats() {
    if (message != null) {
      long n = message.getChannelCount();
      if (n > 0) driver.getStatistics().addValue(peer ? PEER_OUT_TRAFFIC : CLIENT_OUT_TRAFFIC, n);
    }
  }
}
