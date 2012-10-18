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

import org.jppf.io.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
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
   * The task bundle to send or receive.
   */
  protected ServerTaskBundleClient bundle = null;
  /**
   * Helper used to serialize the bundle objects.
   */
  protected SerializationHelper helper = new SerializationHelperImpl();
  /**
   * List of completed bundles to send to the client.
   */
  protected final LinkedList<ServerTaskBundleClient> completedBundles = new LinkedList<ServerTaskBundleClient>();
  /**
   * The job as initially submitted by the client.
   */
  private ServerTaskBundleClient initialBundleWrapper;
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
    return bundle;
  }

  /**
   * Set the task bundle to send or receive.
   * @param bundle a {@link JPPFTaskBundle} instance.
   */
  public void setBundle(final ServerTaskBundleClient bundle)
  {
    this.bundle = bundle;
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
    message.addLocation(IOHelper.serializeData(bundle.getJob(), helper.getSerializer()));
    for (ServerTask task: bundle.getTaskList()) message.addLocation(task.getResult());
    message.setBundle(bundle.getJob());
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
      JPPFTaskBundle bundle = ((ClientMessage) message).getBundle();
      if (locations.size() > 2)
        return new ServerTaskBundleClient(bundle, locations.get(1), locations.subList(2, locations.size()));
       else
        return new ServerTaskBundleClient(bundle, locations.get(1));
  }

  /**
   * Create a new message.
   * @return an {@link ClientMessage} instance.
   */
  public ClientMessage newMessage()
  {
    return new ClientMessage(sslHandler != null);
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
    return getClientMessage().read(channel);
  }

  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception
  {
    return getClientMessage().write(channel);
  }

  /**
   * Add a completed bundle to the queue of bundles to send to the client
   * @param bundleWrapper the bundle to add.
   */
  public void offerCompletedBundle(final ServerTaskBundleClient bundleWrapper)
  {
    synchronized(completedBundles)
    {
      completedBundles.offer(bundleWrapper);
    }
  }

  /**
   * Get the next bundle in the queue.
   * @return A {@link ServerJob} instance, or null if the queue is empty.
   */
  public ServerTaskBundleClient pollCompletedBundle()
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
    if(initialBundleWrapper == null) throw new IllegalStateException("initialBundleWrapper is null");
    return initialBundleWrapper.getPendingTasksCount();
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
   * Send the job ended notification.
   */
  synchronized void jobEnded()
  {
    if (initialBundleWrapper != null)
    {
      initialBundleWrapper.bundleEnded();
      initialBundleWrapper = null;
    }
  }

  /**
   * Send the job ended notification.
   */
  private synchronized void cancelJobOnClose()
  {
    if (initialBundleWrapper != null)
    {
      JPPFTaskBundle header = initialBundleWrapper.getJob();
      if (debugEnabled) log.debug("cancelUponClientDisconnect = " + header.getSLA().isCancelUponClientDisconnect() + " for " + header);
      if (header.getSLA().isCancelUponClientDisconnect())
      {
        initialBundleWrapper.cancel();
      }
      initialBundleWrapper.bundleEnded();
      initialBundleWrapper = null;
    }
  }

  /**
   * Set the job as initially submitted by the client.
   * @param initialBundleWrapper <code>ServerJob</code> instance.
   */
  synchronized void setInitialBundleWrapper(final ServerTaskBundleClient initialBundleWrapper)
  {
    this.initialBundleWrapper = initialBundleWrapper;
  }
}
